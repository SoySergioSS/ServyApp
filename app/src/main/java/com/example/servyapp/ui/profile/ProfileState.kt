package com.example.servyapp.ui.profile

data class ProfileState(
    val email: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showEditDialog: Boolean = false,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false
)