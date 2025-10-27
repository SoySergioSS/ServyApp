package com.example.servyapp.domain.model

data class CartItem(
    val id: String = "",
    val dish: Dish,
    val quantity: Int = 1,
    val restaurantId: String = ""
) {
    val totalPrice: Double
        get() = dish.price * quantity
}