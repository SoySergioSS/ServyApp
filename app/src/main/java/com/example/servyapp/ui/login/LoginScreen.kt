package com.example.servyapp.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.servyapp.ui.theme.ServyAppTheme

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    signupButtonPressed: () -> Unit
) {
    val state by loginViewModel.uiState.collectAsState()

    LoginContent(
        state = state,
        onEmailChange = { loginViewModel.updateEmail(it) },
        onPasswordChange = { loginViewModel.updatePassword(it) },
        onLoginClick = { loginViewModel.loginButtonPressed() },
        onSignupClick = signupButtonPressed, // Se pasa la acción de navegación
        onToggleForgotDialog = { loginViewModel.toggleForgotDialog(it) },
        onForgotEmailChange = { loginViewModel.updateForgotEmail(it) },
        onForgotPasswordClick = { loginViewModel.forgotPassword() },
    )

}

@Composable
fun LoginContent(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    onToggleForgotDialog: (Boolean) -> Unit,
    onForgotEmailChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoginHeader()

        LoginForm(
            email = state.email,
            onEmailChange = onEmailChange,
            password = state.password,
            onPasswordChange = onPasswordChange
        )
        if(state.mostrarMensajeError){
            Text(state.errorMessage)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onToggleForgotDialog(true) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "¿Olvidaste tu contraseña?")
        }

        if (state.showForgotDialog) {
            AlertDialog(
                onDismissRequest = { onToggleForgotDialog(false) },
                title = { Text("Restablecer contraseña") },
                text = {
                    OutlinedTextField(
                        value = state.forgotEmail,
                        onValueChange = onForgotEmailChange,
                        label = { Text("Correo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = onForgotPasswordClick) {
                        Text("Enviar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onToggleForgotDialog(false) }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        LoginFooter(onSignupClicked = onSignupClick)
    }
}

@Composable
private fun LoginHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )


    }
}

@Composable
private fun LoginFooter(
    onSignupClicked: () -> Unit
) {
    TextButton(
        onClick = onSignupClicked,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(text = "¿No tienes una cuenta? Regístrate")
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true
)
@Composable
fun LoginContentPreview() {
    ServyAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginContent(
                state = LoginState(
                    email = "preview@test.com",
                    password = "password123",
                    mostrarMensajeError = true,
                    errorMessage = "Error de ejemplo para el preview",
                    showForgotDialog = true,
                    forgotEmail = "forgot@test.com"
                ),
                onEmailChange = {},
                onPasswordChange = {},
                onLoginClick = {},
                onSignupClick = {},
                onToggleForgotDialog = {},
                onForgotEmailChange = {},
                onForgotPasswordClick = {}
            )
        }
    }
}