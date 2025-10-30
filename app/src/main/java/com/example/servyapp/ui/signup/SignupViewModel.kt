package com.example.servyapp.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.example.servyapp.data.repository.AuthRepository
import com.example.servyapp.domain.model.Card
import com.example.servyapp.domain.useCase.SignupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupState())
    val uiState: StateFlow<SignupState> = _uiState

    fun updateEmail(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun updatePassword(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun updatePhone(newPhone: String) {
        _uiState.update { it.copy(phone = newPhone) }
    }

    fun mostrarEsconderPassword(){
        val valorActual = _uiState.value.mostrarPassword
        _uiState.update { it.copy(mostrarPassword = !valorActual) }
    }

    fun updateErrorMessage(newMessage: String){
        _uiState.update { it.copy(errorMessage = newMessage) }
    }

    fun updateCardNumber(cardNumber: String) {
        _uiState.update { it.copy(cardNumber = cardNumber) }
    }

    fun updateCardHolderName(cardHolderName: String) {
        _uiState.update { it.copy(cardHolderName = cardHolderName) }
    }

    fun updateExpirationDate(expirationDate: String) {
        _uiState.update { it.copy(expirationDate = expirationDate) }
    }

    fun updateCvv(cvv: String) {
        _uiState.update { it.copy(cvv = cvv) }
    }

    fun clearForm() {
        _uiState.update {
            it.copy(
                cardNumber = "",
                cardHolderName = "",
                expirationDate = "",
                cvv = ""
            )
        }
    }

    fun addCardButtonPressed(){
        _uiState.update { it.copy(mostrarMensajeError = false, errorMessage = "") }

        val state = _uiState.value

        val errorMessage = when {
            state.cardNumber.isEmpty() || state.cardHolderName.isEmpty() || state.expirationDate.isEmpty() || state.cvv.isEmpty() -> {
                "Por favor, complete todos los campos."
            }
            state.cardNumber.length != 16 -> {
                "El número de tarjeta debe tener 16 dígitos."
            }
            state.expirationDate.length != 5 || !state.expirationDate.contains('/') -> {
                "La fecha de vencimiento debe estar en formato MM/AA."
            }
            state.cvv.length != 3 -> {
                "El CVV debe tener 3 dígitos."
            }
            else -> null
        }

        if (errorMessage != null) {
            _uiState.update { it.copy(mostrarMensajeError = true, errorMessage = errorMessage) }
        } else {
            _uiState.update { it.copy(navigateToSignup = true) }
        }
    }

    fun registerButtonPressed(){
        when {
            _uiState.value.email.isEmpty() || _uiState.value.password.isEmpty() || _uiState.value.phone.isEmpty() -> {
                _uiState.update { it.copy(mostrarMensajeError = true) }
                _uiState.update { it.copy(errorMessage = "Por favor, complete todos los campos") }

                return
            }

            !uiState.value.email.contains("@") || !uiState.value.email.contains(".") -> {
                _uiState.update { it.copy(mostrarMensajeError = true) }
                _uiState.update { it.copy(errorMessage = "El email no es válido") }

                return
            }

            uiState.value.password.length < 6 -> {
                _uiState.update { it.copy(mostrarMensajeError = true) }
                _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }

                return
            }

            _uiState.value.phone.length != 9 -> {
                _uiState.update { it.copy(mostrarMensajeError = true) }
                _uiState.update { it.copy(errorMessage = "El teléfono debe tener 9 dígitos") }

                return
            }
        }

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true) }

            val result = signupUseCase.execute(
                uiState.value.email,
                uiState.value.password,
                uiState.value.phone,
                uiState.value.cardNumber,
                uiState.value.cardHolderName,
                uiState.value.expirationDate,
                uiState.value.cvv
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    mostrarMensajeError = !result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.message.toString(),
                    navigateToHome = result.isSuccess
                )
            }

        }
    }

    fun resetNavigateToSignup() {
        _uiState.value = _uiState.value.copy(navigateToSignup = false)
    }


}