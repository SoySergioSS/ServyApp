package com.example.servyapp.ui.cart

import com.example.servyapp.domain.model.CartItem

data class CartState(
    val items: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteDialog: CartItem? = null,
    val navigationEvent: NavigationEvent? = null
) {
    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    val isEmpty: Boolean
        get() = items.isEmpty()
}

sealed class NavigationEvent {
    object NavigateToOrders : NavigationEvent()
    data class NavigateToDishDetail(val restaurantId: String, val dishId: String) : NavigationEvent()
}