package com.example.servyapp.ui.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.servyapp.R
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.utils.AppLoading

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Recibe el ViewModel (inyectado desde AppNavigation),
 * recolecta el estado y lo pasa al composable de UI (SignupContent).
 */
@Composable
fun SignupScreen(
    signupViewModel: SignupViewModel,
    addCardButtonPressed: () -> Unit,
    loginButtonPressed: () -> Unit
) {
    val state by signupViewModel.uiState.collectAsState()

    // La lógica de navegación (state.navigateToHome)
    // ya se maneja en AppNavigation.kt, así que no se repite aquí.

    // Llamamos al Composable de UI (Contenido)
    SignupContent(
        state = state,
        onEmailChange = { signupViewModel.updateEmail(it) },
        onPasswordChange = { signupViewModel.updatePassword(it) },
        onPhoneChange = { signupViewModel.updatePhone(it) },
        onMostrarPasswordChange = { signupViewModel.mostrarEsconderPassword() },
        onAddCardClick = addCardButtonPressed,
        onRegisterClick = { signupViewModel.registerButtonPressed() },
        onLoginClick = loginButtonPressed
    )
}

/**
 * 2. CONTENIDO (Stateless)
 *
 * Se encarga SOLO de la UI. Recibe el estado y las lambdas
 * para notificar eventos.
 */
@Composable
fun SignupContent(
    state: SignupState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onMostrarPasswordChange: () -> Unit,
    onAddCardClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Usamos un Box para poder superponer el indicador de carga
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // El Column ocupa todo el espacio
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RegisterHeader()

            // Pasamos el icono correcto al formulario
            val icono = if (!state.mostrarPassword)
                R.drawable.ic_launcher_background //TODO: cambiar por los simbolos de ojo
            else
                R.drawable.ic_launcher_foreground //TODO: cambiar por los simbolos de ojo

            RegisterForm(
                email = state.email,
                onEmailChange = onEmailChange,
                password = state.password,
                onPasswordChange = onPasswordChange,
                phone = state.phone,
                onPhoneChange = onPhoneChange,
                mostrarPassword = state.mostrarPassword,
                onMostrarPasswordChange = onMostrarPasswordChange,
                icono = icono
            )

            if (state.mostrarMensajeError) {
                // Copiamos a variable local para el smart-cast
                val errorMessage = state.errorMessage
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onAddCardClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Agregar tarjeta de crédito")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Registrarse")
            }

            // Pie de página de la pantalla
            RegisterFooter(onLoginClicked = onLoginClick)
        }

        // Indicador de carga superpuesto
        if (state.isLoading) {
            AppLoading()
        }
    }
}


// --- Componentes de UI privados (sin cambios) ---

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

/**
 * 3. PREVIEWS FUNCIONALES
 *
 * Actualizamos el preview para que use `SignupContent` y
 * le pasamos un estado de prueba.
 */
@Preview(
    showSystemUi = true,
    showBackground = true,
    name = "Estado por Defecto"
)
@Composable
fun SignupContentPreview_Default() {
    ServyAppTheme {
        SignupContent(
            state = SignupState(email = "test@preview.com", phone = "987654321"),
            onEmailChange = {},
            onPasswordChange = {},
            onPhoneChange = {},
            onMostrarPasswordChange = {},
            onAddCardClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    name = "Estado de Error"
)
@Composable
fun SignupContentPreview_Error() {
    ServyAppTheme {
        SignupContent(
            state = SignupState(
                email = "test@preview.com",
                phone = "987654321",
                mostrarMensajeError = true,
                errorMessage = "Este es un mensaje de error de prueba."
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onPhoneChange = {},
            onMostrarPasswordChange = {},
            onAddCardClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true,
    name = "Estado de Carga"
)
@Composable
fun SignupContentPreview_Loading() {
    ServyAppTheme {
        SignupContent(
            state = SignupState(isLoading = true),
            onEmailChange = {},
            onPasswordChange = {},
            onPhoneChange = {},
            onMostrarPasswordChange = {},
            onAddCardClick = {},
            onRegisterClick = {},
            onLoginClick = {}
        )
    }
}