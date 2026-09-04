package com.example.crowdtransportfeedback.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.crowdtransportfeedback.auth.AuthRepository
import com.example.crowdtransportfeedback.auth.SessionManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(repository: AuthRepository, session: SessionManager) {
    var register by remember { mutableStateOf(false) }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (register) "Create account" else "Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp)); OutlinedTextField(email, { email=it }, label={Text("Email")}, singleLine=true, modifier=Modifier.fillMaxWidth(), enabled=!loading)
        OutlinedTextField(password, { password=it }, label={Text("Password")}, visualTransformation=PasswordVisualTransformation(), singleLine=true, modifier=Modifier.fillMaxWidth(), enabled=!loading)
        if(register) OutlinedTextField(confirm,{confirm=it},label={Text("Confirm password")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=!loading)
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Button(onClick={
            error = when { email.isBlank() -> "Enter your email."; password.length < 8 -> "Password must have at least 8 characters."; register && password != confirm -> "Passwords do not match."; else -> null }
            if(error==null){loading=true;scope.launch { runCatching { if(register) repository.register(email,password) else repository.login(email,password) }.onSuccess { session.authenticated(it) }.onFailure { error="Authentication failed. Check your details and connection." };loading=false }}
        },enabled=!loading,modifier=Modifier.fillMaxWidth()){Text(if(loading) "Please wait…" else if(register) "Create account" else "Login")}
        TextButton(onClick={register=!register;error=null},enabled=!loading){Text(if(register) "Back to Login" else "Register")}
    }
}

@Composable
fun AccountBar(email:String, role:String, repository:AuthRepository, session:SessionManager){val scope=rememberCoroutineScope();Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(email);Text(role,style=MaterialTheme.typography.labelSmall)};TextButton(onClick={scope.launch{repository.logout();session.clear()}}){Text("Logout")}}}
