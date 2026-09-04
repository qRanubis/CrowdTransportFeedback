package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.auth.AuthRepository
import com.example.crowdtransportfeedback.auth.SessionManager
import com.example.crowdtransportfeedback.auth.isValidLoginEmail
import com.example.crowdtransportfeedback.auth.isValidRegistrationEmail
import com.example.crowdtransportfeedback.auth.isValidUsername
import com.example.crowdtransportfeedback.auth.registrationPasswordError
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(repository: AuthRepository, session: SessionManager) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (register) "Create account" else "Login",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        )

        if (register) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                supportingText = { Text("3-20 lowercase letters and digits") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        )

        if (register) {
            Text(
                "Minimum 8 characters, with uppercase, lowercase, digit and symbol.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            )
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                error = when {
                    register && !isValidRegistrationEmail(email) ->
                        "Email must end in a 2-3 letter domain such as .ro, .it or .com."
                    !register && !isValidLoginEmail(email) -> "Enter a valid email address."
                    register && !isValidUsername(username) ->
                        "Username must be 3-20 characters using only lowercase letters and digits."
                    password.isBlank() -> "Enter your password."
                    register && registrationPasswordError(password) != null ->
                        registrationPasswordError(password)
                    register && password != confirm -> "Passwords do not match."
                    else -> null
                }

                if (error == null) {
                    loading = true
                    scope.launch {
                        runCatching {
                            if (register) {
                                repository.register(email, username, password)
                            } else {
                                repository.login(email, password)
                            }
                        }.onSuccess {
                            session.authenticated(it)
                        }.onFailure {
                            error = "Authentication failed. Check your details and connection."
                        }
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (loading) "Please wait…"
                else if (register) "Create account"
                else "Login"
            )
        }

        TextButton(
            onClick = {
                register = !register
                error = null
                password = ""
                confirm = ""
            },
            enabled = !loading
        ) {
            Text(if (register) "Back to Login" else "Register")
        }
    }
}

@Composable
fun AccountBar(
    username: String,
    email: String,
    role: String,
    session: SessionManager,
    onProfile: () -> Unit = {},
    avatarKey: String = "COMMUTER"
) {
    val scope = rememberCoroutineScope()

    Row(
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.clickable(onClick = onProfile)) {
            Text("${avatarSymbol(avatarKey)} ${if (username.isBlank()) email else "@$username"}")
            if (username.isNotBlank()) {
                Text(email, style = MaterialTheme.typography.labelSmall)
            }
            Text(role, style = MaterialTheme.typography.labelSmall)
        }

        TextButton(onClick = { scope.launch { session.logout() } }) {
            Text("Logout")
        }
    }
}
