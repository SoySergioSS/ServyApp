package com.example.servyapp.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servyapp.R
import com.example.servyapp.ui.utils.AppLoading

@Composable
fun SignupScreen(
    signupViewModel: SignupViewModel,
    addCardButtonPressed: () -> Unit,
    loginButtonPressed: () -> Unit
) {
    val state by signupViewModel.uiState.collectAsState()

    var icono = if(!state.mostrarPassword) R.drawable.ic_launcher_background else R.drawable.ic_launcher_foreground //cambiar por los simbolos de ojo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RegisterHeader()

        RegisterForm(
            email = state.email,
            onEmailChange = { signupViewModel.updateEmail(it) },
            password = state.password,
            onPasswordChange = { signupViewModel.updatePassword(it) },
            phone = state.phone,
            onPhoneChange = { signupViewModel.updatePhone(it) },
            mostrarPassword = state.mostrarPassword,
            onMostrarPasswordChange = { signupViewModel.mostrarEsconderPassword() },
            icono = icono
        )

        if (state.mostrarMensajeError) {
            Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = addCardButtonPressed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Agregar tarjeta de crédito")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { signupViewModel.registerButtonPressed() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Registrarse")
        }

        // Pie de página de la pantalla
        RegisterFooter(onLoginClicked = loginButtonPressed)
    }

    if(state.isLoading) AppLoading()
}

@Composable
private fun RegisterHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear una cuenta",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}

@Composable
private fun RegisterForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    mostrarPassword: Boolean,
    onMostrarPasswordChange: () -> Unit,
    icono: Int,
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
            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = onMostrarPasswordChange) {
                    Icon(
                        painter = painterResource(id = icono),
                        contentDescription = "Mostrar password"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Teléfono") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )


    }
}

@Composable
private fun RegisterFooter(
    onLoginClicked: () -> Unit
) {
    TextButton(
        onClick = onLoginClicked,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        Text(text = "¿Ya tienes una cuenta? Iniciar Sesión")
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true
)
@Composable
fun RegisterScreenPreview() {
    SignupScreen(
        signupViewModel = viewModel(),
        addCardButtonPressed = {},
        loginButtonPressed = {}
    )
}