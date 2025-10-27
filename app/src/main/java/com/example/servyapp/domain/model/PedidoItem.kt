package com.example.servyapp.domain.model

data class PedidoItem(
    val dishId: String = "",
    val dishName: String = "",
    val dishImageURL: String = "",
    val dishDescription: String = "",
    val quantity: Int = 0,
    val pricePerUnit: Double = 0.0,
    val totalPrice: Double = 0.0
)