package com.example.servyapp.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


data class Pedido(
    val id: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val items: List<PedidoItem> = emptyList(),
    val subtotal: Double = 0.0,
    @JvmField @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),
    val status: PedidoStatus = PedidoStatus.PENDING
)

enum class PedidoStatus {
    PENDING,      // Pendiente
    CONFIRMED,    // Confirmado por el restaurante
    PREPARING,    // En preparación
    DELIVERED,    // Entregado
    CANCELLED     // Cancelado
}