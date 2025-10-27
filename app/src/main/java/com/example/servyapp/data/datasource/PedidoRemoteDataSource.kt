package com.example.servyapp.data.datasource

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
    private fun getOrdersCollection(restaurantId: String) =
        firestore.collection("restaurants").document(restaurantId).collection("orders")

    /**
     * Crea una nueva orden con sus pedidos
     */
    suspend fun createOrder(restaurantId: String, order: Order): Result<String> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val docRef = ordersCollection.document()
            val orderWithId = order.copy(id = docRef.id)
            docRef.set(orderWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la orden activa (PENDING o IN_PROGRESS) de un usuario en un restaurante
     */
    suspend fun getActiveOrder(restaurantId: String, userId: String): Result<Order?> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val snapshot = ordersCollection
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf(OrderStatus.PENDING.name, OrderStatus.IN_PROGRESS.name))
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

    /**
     * Obtiene todas las órdenes de un usuario en un restaurante en tiempo real
     */
    fun getUserOrders(restaurantId: String, userId: String): Flow<Result<List<Order>>> = callbackFlow {
        val ordersCollection = getOrdersCollection(restaurantId)
        val listenerRegistration = ordersCollection
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

    /**
     * Obtiene una orden específica por ID
     */
    suspend fun getOrderById(restaurantId: String, orderId: String): Result<Order> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val snapshot = ordersCollection.document(orderId).get().await()
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

    /**
     * Actualiza el estado de una orden completa
     */
    suspend fun updateOrderStatus(restaurantId: String, orderId: String, status: OrderStatus): Result<Unit> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            ordersCollection.document(orderId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Agrega un pedido a una orden existente
     */
    suspend fun addPedidoToOrder(restaurantId: String, orderId: String, pedido: Pedido): Result<Unit> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val orderDoc = ordersCollection.document(orderId)
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

    /**
     * Elimina un pedido de una orden
     */
    suspend fun removePedidoFromOrder(restaurantId: String, orderId: String, pedidoId: String): Result<Unit> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val orderDoc = ordersCollection.document(orderId)
            val snapshot = orderDoc.get().await()
            val order = snapshot.toObject(Order::class.java)

            if (order != null) {
                val updatedPedidos = order.pedidos.filter { it.id != pedidoId }
                val newTotal = updatedPedidos.sumOf { it.subtotal }

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

    /**
     * Actualiza el estado de un pedido específico
     */
    suspend fun updatePedidoStatus(
        restaurantId: String,
        orderId: String,
        pedidoId: String,
        status: PedidoStatus
    ): Result<Unit> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            val orderDoc = ordersCollection.document(orderId)
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

    /**
     * Elimina una orden completa
     */
    suspend fun deleteOrder(restaurantId: String, orderId: String): Result<Unit> {
        return try {
            val ordersCollection = getOrdersCollection(restaurantId)
            ordersCollection.document(orderId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generatePedidoId(): String {
        return "pedido_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}