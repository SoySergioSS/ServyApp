package com.example.servyapp.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Order(
    val id: String = "",
    val userId: String = "",
    @JvmField @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(),
    val orderNumber: String = "",
    val pedidos: List<Pedido> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING //no se si las ordenes tienen status
)

enum class OrderStatus {
    PENDING,      // Pendiente
    IN_PROGRESS,  // En progreso (tiene pedidos activos)
    COMPLETED,    // Completada (todos los pedidos completados)
    CANCELLED     // Cancelada
}