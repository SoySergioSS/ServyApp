package com.example.servyapp.ui.signup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.servyapp.ui.theme.ServyAppTheme

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Se encarga de la lógica: obtener el ViewModel, manejar
 * los eventos de navegación (LaunchedEffect) y recolectar el estado.
 */
@Composable
fun CardRegistrationScreen(
    signupViewModel: SignupViewModel,
    onNavigateBack: () -> Unit
) {
    val state by signupViewModel.uiState.collectAsState()

    // Este LaunchedEffect maneja la navegación automática
    // Se queda en el contenedor porque es lógica de navegación.
    LaunchedEffect(state.navigateToSignup) {
        if (state.navigateToSignup) {
            onNavigateBack()
            signupViewModel.resetNavigateToSignup()
        }
    }

    // Llamamos al Composable de UI (Contenido)
    CardRegistrationContent(
        state = state,
        onCardNumberChange = { signupViewModel.updateCardNumber(it) },
        onCardHolderNameChange = { signupViewModel.updateCardHolderName(it) },
        onExpirationDateChange = { signupViewModel.updateExpirationDate(it) },
        onCvvChange = { signupViewModel.updateCvv(it) },
        onRegisterClick = {
            signupViewModel.addCardButtonPressed()
            // La navegación se maneja con el LaunchedEffect de arriba
        },
        onNavigateBack = {
            onNavigateBack()
            signupViewModel.clearForm()
        }
    )
}

/**
 * 2. CONTENIDO (Stateless)
 *
 * Se encarga SOLO de la UI. Recibe el estado y las lambdas
 * para notificar eventos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardRegistrationContent(
    state: SignupState,
    onCardNumberChange: (String) -> Unit,
    onCardHolderNameChange: (String) -> Unit,
    onExpirationDateChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Registro de Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { // Llama a la lambda
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a la pantalla anterior"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CardRegistrationForm(
                cardNumber = state.cardNumber,
                onCardNumberChange = onCardNumberChange,
                cardHolderName = state.cardHolderName,
                onCardHolderNameChange = onCardHolderNameChange,
                expirationDate = state.expirationDate,
                onExpirationDateChange = onExpirationDateChange,
                cvv = state.cvv,
                onCvvChange = onCvvChange
            )

            if (state.mostrarMensajeError) {
                // Copiamos a variable local para el smart-cast
                val errorMessage = state.errorMessage
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRegisterClick, // Llama a la lambda
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Registrar Tarjeta")
            }
        }
    }
}


/**
 * CardRegistrationForm (Stateless Sub-component)
 * No necesita cambios, ya era stateless.
 */
@Composable
private fun CardRegistrationForm(
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    cardHolderName: String,
    onCardHolderNameChange: (String) -> Unit,
    expirationDate: String,
    onExpirationDateChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            label = { Text("Número de Tarjeta") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cardHolderName,
            onValueChange = onCardHolderNameChange,
            label = { Text("Nombre del Titular") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = expirationDate,
            onValueChange = onExpirationDateChange,
            label = { Text("Fecha de Vencimiento (MM/AA)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cvv,
            onValueChange = onCvvChange,
            label = { Text("CVV") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/**
 * 3. PREVIEWS FUNCIONALES
 *
 * Creamos vistas previas para los diferentes estados de CardRegistrationContent.
 */

@Preview(showBackground = true, showSystemUi = true, name = "Estado por Defecto")
@Composable
fun CardRegistrationContentPreview_Default() {
    ServyAppTheme {
        CardRegistrationContent(
            state = SignupState(
                cardNumber = "1234567812345678",
                cardHolderName = "Nombre de Prueba",
                expirationDate = "12/25",
                cvv = "123"
            ),
            onCardNumberChange = {},
            onCardHolderNameChange = {},
            onExpirationDateChange = {},
            onCvvChange = {},
            onRegisterClick = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Estado de Error")
@Composable
fun CardRegistrationContentPreview_Error() {
    ServyAppTheme {
        CardRegistrationContent(
            state = SignupState(
                mostrarMensajeError = true,
                errorMessage = "El número de tarjeta debe tener 16 dígitos."
            ),
            onCardNumberChange = {},
            onCardHolderNameChange = {},
            onExpirationDateChange = {},
            onCvvChange = {},
            onRegisterClick = {},
            onNavigateBack = {}
        )
    }
}