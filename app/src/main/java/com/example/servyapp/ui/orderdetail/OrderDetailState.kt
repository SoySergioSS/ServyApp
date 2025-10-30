package com.example.servyapp.ui.orderdetail

import com.example.servyapp.domain.model.Order
import com.example.servyapp.ui.orders.NavigationEvent

data class OrderDetailState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigationEvent: NavigationEvent? = null
)

