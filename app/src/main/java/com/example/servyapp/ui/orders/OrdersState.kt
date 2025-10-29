package com.example.servyapp.ui.orders

import com.example.servyapp.domain.model.Order

data class OrdersState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigationEvent: NavigationEvent? = null
)

sealed class NavigationEvent {
    data class NavigateToOrderDetail(val orderId: String) : NavigationEvent()
}