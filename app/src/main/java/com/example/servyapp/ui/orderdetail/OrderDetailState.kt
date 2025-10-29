package com.example.servyapp.ui.orderdetail

import com.example.servyapp.domain.model.Order

data class OrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
