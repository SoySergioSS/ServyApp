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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardRegistrationScreen(
    signupViewModel: SignupViewModel,
    onNavigateBack: () -> Unit
) {
    val state by signupViewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToSignup) {
        if (state.navigateToSignup) {
            onNavigateBack()
            signupViewModel.resetNavigateToSignup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Registro de Tarjeta") },
                navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                        signupViewModel.clearForm()
                    }) {
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
                onCardNumberChange = { signupViewModel.updateCardNumber(it) },
                cardHolderName = state.cardHolderName,
                onCardHolderNameChange = { signupViewModel.updateCardHolderName(it) },
                expirationDate = state.expirationDate,
                onExpirationDateChange = { signupViewModel.updateExpirationDate(it) },
                cvv = state.cvv,
                onCvvChange = { signupViewModel.updateCvv(it) }
            )

            if (state.mostrarMensajeError) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    signupViewModel.addCardButtonPressed()
                    if(state.navigateToSignup) onNavigateBack()
                          },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Registrar Tarjeta")
            }
        }
    }
}

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

