package com.example.servyapp.data.datasource

import android.util.Log
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedidoRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    //private val ordersCollection = firestore.collection("orders")
    private fun ordersCollection(restaurantId: String) =
        firestore.collection("restaurants").document(restaurantId).collection("orders")

    suspend fun createOrder(order: Order, restaurantId: String): Result<String> {
        return try {
            val docRef = ordersCollection(restaurantId).document()
            val orderWithId = order.copy(id = docRef.id)
            docRef.set(orderWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserOrders(userId: String, restaurantId: String): Flow<Result<List<Order>>> = callbackFlow {
        val listenerRegistration = ordersCollection(restaurantId)
            .whereEqualTo("userId", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(orders))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getOrderById(orderId: String, restaurantId: String): Result<Order> {
        return try {
            val snapshot = ordersCollection(restaurantId).document(orderId).get().await()
            val order = snapshot.toObject(Order::class.java)?.copy(id = snapshot.id)
            if (order != null) {
                Result.success(order)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, restaurantId: String): Result<Unit> {
        return try {
            ordersCollection(restaurantId).document(orderId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPedidoToOrder(orderId: String, pedido: Pedido, restaurantId: String): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(restaurantId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val pedidoWithId = pedido.copy(id = generatePedidoId())
                val updatedPedidos = order.pedidos + pedidoWithId
                val newTotal = updatedPedidos.sumOf { it.subtotal }

                orderDoc.update(
                    mapOf(
                        "pedidos" to updatedPedidos,
                        "totalAmount" to newTotal,
                        "status" to OrderStatus.IN_PROGRESS.name
                    )
                ).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removePedidoFromOrder(orderId: String, pedidoId: String, restaurantId: String): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(restaurantId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val updatedPedidos = order.pedidos.filter { it.id != pedidoId }
                val newTotal = updatedPedidos.sumOf { it.subtotal }

                // Si no quedan pedidos, cambiar estado de la orden
                val newStatus = if (updatedPedidos.isEmpty()) {
                    OrderStatus.PENDING
                } else {
                    order.status
                }

                orderDoc.update(
                    mapOf(
                        "pedidos" to updatedPedidos,
                        "totalAmount" to newTotal,
                        "status" to newStatus.name
                    )
                ).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePedidoStatus(orderId: String, pedidoId: String, status: PedidoStatus, restaurantId: String): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(restaurantId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val updatedPedidos = order.pedidos.map { pedido ->
                    if (pedido.id == pedidoId) {
                        pedido.copy(status = status)
                    } else {
                        pedido
                    }
                }

                // Actualizar estado de la orden según los pedidos
                val allCompleted = updatedPedidos.all { it.status == PedidoStatus.DELIVERED }
                val newOrderStatus = if (allCompleted) {
                    OrderStatus.COMPLETED
                } else {
                    OrderStatus.IN_PROGRESS
                }

                orderDoc.update(
                    mapOf(
                        "pedidos" to updatedPedidos,
                        "status" to newOrderStatus.name
                    )
                ).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePedidoItemQuantity(
        orderId: String,
        pedidoId: String,
        dishId: String,
        newQuantity: Int,
        restaurantId: String
    ): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(restaurantId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val updatedPedidos = order.pedidos.map { pedido ->
                    if (pedido.id == pedidoId) {
                        val updatedItems = pedido.items.map { item ->
                            if (item.dishId == dishId) {
                                item.copy(
                                    quantity = newQuantity,
                                    totalPrice = item.pricePerUnit * newQuantity
                                )
                            } else {
                                item
                            }
                        }
                        val newSubtotal = updatedItems.sumOf { it.totalPrice }
                        pedido.copy(items = updatedItems, subtotal = newSubtotal)
                    } else {
                        pedido
                    }
                }

                val newTotal = updatedPedidos.sumOf { it.subtotal }

                orderDoc.update(
                    mapOf(
                        "pedidos" to updatedPedidos,
                        "totalAmount" to newTotal
                    )
                ).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Orden no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrder(orderId: String, restaurantId: String): Result<Unit> {
        return try {
            ordersCollection(restaurantId).document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generatePedidoId(): String {
        return "pedido_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}