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
    private fun ordersCollection(userId: String) =
        firestore.collection("users").document(userId).collection("orders")

    suspend fun createOrder(userId: String, order: Order): Result<String> {
        return try {
            val docRef = ordersCollection(userId).document()
            val orderWithId = order.copy(id = docRef.id)
            docRef.set(orderWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveOrder(userId: String): Result<Order?> {
        return try {
            val snapshot = ordersCollection(userId)
                .whereIn("status", listOf(OrderStatus.PENDING.name, OrderStatus.IN_PROGRESS.name))
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val order = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserOrders(userId: String): Flow<Result<List<Order>>> = callbackFlow {
        val listenerRegistration = ordersCollection(userId)
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

    suspend fun getOrderById(orderId: String, userId: String): Result<Order> {
        return try {
            val snapshot = ordersCollection(userId).document(orderId).get().await()
            Log.d("OrderDebug", "Snapshot existe: ${snapshot.exists()}")
            Log.d("OrderDebug", "Datos brutos: ${snapshot.data}")
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

    suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            ordersCollection(userId).document(orderId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addPedidoToOrder(userId: String, orderId: String, pedido: Pedido): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(userId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val pedidoWithId = pedido.copy(id = generatePedidoId())
                val updatedPedidos = order.pedidos + pedidoWithId
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

    suspend fun removePedidoFromOrder(orderId: String, pedidoId: String, userId: String): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(userId).document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val updatedPedidos = order.pedidos.filter { it.id != pedidoId }
                val newTotal = updatedPedidos.sumOf { it.subtotal }

                val newStatus = if (updatedPedidos.isEmpty()) {
                    OrderStatus.CANCELLED
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

    suspend fun updatePedidoStatus(userId: String, orderId: String, pedidoId: String, status: PedidoStatus): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(userId).document(orderId)
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
        userId: String,
        orderId: String,
        pedidoId: String,
        dishId: String,
        newQuantity: Int
    ): Result<Unit> {
        return try {
            val orderDoc = ordersCollection(userId).document(orderId)
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

    suspend fun deleteOrder(orderId: String, userId: String): Result<Unit> {
        return try {
            ordersCollection(userId).document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentMethod(orderId: String, method: String, userId: String) {
        ordersCollection(userId).document(orderId)
            .update("paymentMethod", method)
            .await()
    }

    private fun generatePedidoId(): String {
        return "pedido_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}