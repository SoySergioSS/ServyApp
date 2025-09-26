package com.example.servyapp.ui.signup

data class SignupState(
    val email: String = "",
    val password: String = "",
    val phone: String = "",

    val cardNumber: String = "",
    val cardHolderName: String = "",
    val expirationDate: String = "",
    val cvv: String = "",

    val mostrarPassword: Boolean = false,
    val errorMessage: String = "",
    val mostrarMensajeError: Boolean = false,
    val navigateToHome: Boolean = false,
    val navigateToSignup: Boolean = false,
    val isLoading: Boolean = false
)