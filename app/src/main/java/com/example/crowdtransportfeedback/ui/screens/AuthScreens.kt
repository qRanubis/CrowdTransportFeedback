package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.auth.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun AuthScreen(repository: AuthRepository, session: SessionManager) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }; var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }; var passwordError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }; var confirmError by remember { mutableStateOf<String?>(null) }
    var serverError by remember { mutableStateOf<String?>(null) }; var loading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }; var confirmVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope(); val focus = LocalFocusManager.current

    fun resetAndSwitch() {
        email = ""; username = ""; password = ""; confirm = ""
        emailError = null; usernameError = null; passwordError = null; confirmError = null; serverError = null
        passwordVisible = false; confirmVisible = false; focus.clearFocus(force = true); register = !register
    }

    Box(Modifier.fillMaxSize().imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().widthIn(max = 480.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (register) "Create account" else "Welcome back", style = MaterialTheme.typography.headlineMedium)
                Text(if (register) "Join the community and improve every trip." else "Sign in to share transport feedback.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(email, { email = it; emailError = null }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = emailError != null,
                    supportingText = errorSupportingText(emailError), enabled = !loading)
                if (register) OutlinedTextField(username, { username = it; usernameError = null }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true,
                    isError = usernameError != null, supportingText = { Text(usernameError ?: "3–20 lowercase letters and digits") }, enabled = !loading)
                PasswordField("Password", password, { password = it; passwordError = null }, passwordVisible, { passwordVisible = !passwordVisible }, passwordError, loading)
                if (register) {
                    Text("At least 8 characters with uppercase, lowercase, digit and symbol.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PasswordField("Confirm password", confirm, { confirm = it; confirmError = null }, confirmVisible, { confirmVisible = !confirmVisible }, confirmError, loading)
                }
                serverError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Button(onClick = {
                    focus.clearFocus(); serverError = null
                    if (!register) {
                        // Login validation is deliberately sequential: do not distract from
                        // the first actionable error or contact the backend prematurely.
                        emailError = if (!isValidLoginEmail(email)) "Enter a valid email address." else null
                        passwordError = if (emailError == null && password.isBlank()) "Enter your password." else null
                        usernameError = null
                        confirmError = null
                    } else {
                        emailError = if (!isValidRegistrationEmail(email)) "Enter a valid email address." else null
                        usernameError = if (!isValidUsername(username)) "Username must be 3–20 characters using only lowercase letters and digits." else null
                        passwordError = if (password.isBlank()) "Enter your password." else registrationPasswordError(password)
                        confirmError = if (password != confirm) "Passwords do not match." else null
                    }
                    if (emailError == null && usernameError == null && passwordError == null && confirmError == null) {
                        loading = true; scope.launch {
                            runCatching { if (register) repository.register(email, username, password) else repository.login(email, password) }
                                .onSuccess { session.authenticated(it) }
                                .onFailure { serverError = authenticationError(it, register) }
                            loading = false
                        }
                    }
                }, enabled = !loading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    if (loading) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                    Text(if (loading) "Please wait…" else if (register) "Create account" else "Login")
                }
                TextButton(onClick = ::resetAndSwitch, enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
                    Text(if (register) "Back to login" else "Create an account")
                }
            }
        }
    }
}

@Composable private fun PasswordField(label: String, value: String, change: (String) -> Unit, visible: Boolean, toggle: () -> Unit, error: String?, loading: Boolean) =
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, enabled = !loading,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), isError = error != null,
        supportingText = errorSupportingText(error), trailingIcon = { TextButton(onClick = toggle) { Text(if (visible) "Hide" else "Show") } })

private fun errorSupportingText(message: String?): (@Composable () -> Unit)? =
    message?.let { { Text(it) } }

internal fun authenticationError(error: Throwable, registering: Boolean): String = when {
    error is IOException -> "Unable to connect. Check your internet connection and try again."
    error is HttpException && error.code() in listOf(400, 401, 403) && !registering -> "Incorrect email or password."
    else -> if (registering) "Registration failed. Please try again." else "Authentication failed. Please try again."
}

@Composable
fun AccountBar(username: String, email: String, role: String, session: SessionManager, onProfile: () -> Unit = {}, showBack: Boolean = false,
    onBack: () -> Unit = {}, avatarKey: String = "COMMUTER", onAdmin: (() -> Unit)? = null,
    showHome: Boolean = false, onHome: () -> Unit = {}, showLogout: Boolean = true) {
    val scope = rememberCoroutineScope()
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            if (showBack) TextButton(onClick = onBack) { Text("‹ Back") }
            else if (!showHome) Row(Modifier.clickable(onClick = onProfile).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(avatarSymbol(avatarKey), style = MaterialTheme.typography.titleLarge); Spacer(Modifier.width(8.dp)); Text("@$username", style = MaterialTheme.typography.titleMedium)
            } else TextButton(onClick = onHome) { Text("⌂ Home") }
            Row { if (showBack && showHome) TextButton(onClick = onHome) { Text("⌂ Home") }; onAdmin?.let { TextButton(onClick = it) { Text("Admin") } }; if (showLogout) TextButton(onClick = { scope.launch { session.logout() } }) { Text("Logout") } }
        }
    }
}
