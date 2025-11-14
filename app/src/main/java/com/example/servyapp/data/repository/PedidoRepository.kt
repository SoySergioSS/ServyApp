package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.PedidoRemoteDataSource
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PedidoRepository @Inject constructor(
    private val pedidoRemoteDataSource: PedidoRemoteDataSource
) {
    suspend fun createOrder(order: Order, userId: String): Result<String> =
        pedidoRemoteDataSource.createOrder(userId, order)

    suspend fun getActiveOrder(userId: String): Result<Order?> =
        pedidoRemoteDataSource.getActiveOrder(userId)

    fun getUserOrders(userId: String): Flow<Result<List<Order>>> =
        pedidoRemoteDataSource.getUserOrders(userId)

    suspend fun getOrderById(orderId: String, userId: String): Result<Order> =
        pedidoRemoteDataSource.getOrderById(orderId, userId)

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, userId: String): Result<Unit> =
        pedidoRemoteDataSource.updateOrderStatus(userId, orderId, status)

    suspend fun addPedidoToOrder(orderId: String, pedido: Pedido, userId: String): Result<Unit> =
        pedidoRemoteDataSource.addPedidoToOrder(userId, orderId, pedido)

    suspend fun removePedidoFromOrder(orderId: String, pedidoId: String, userId: String): Result<Unit> =
        pedidoRemoteDataSource.removePedidoFromOrder(orderId, pedidoId, userId)

    suspend fun updatePedidoStatus(orderId: String, pedidoId: String, status: PedidoStatus, userId: String): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoStatus(userId, orderId, pedidoId, status)

    suspend fun updatePedidoItemQuantity(
        orderId: String,
        pedidoId: String,
        dishId: String,
        newQuantity: Int,
        userId: String
    ): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoItemQuantity(userId, orderId, pedidoId, dishId, newQuantity)

    suspend fun deleteOrder(orderId: String, userId: String): Result<Unit> =
        pedidoRemoteDataSource.deleteOrder(orderId, userId)

    suspend fun updatePaymentMethod(orderId: String, method: String, userId: String) =
        pedidoRemoteDataSource.updatePaymentMethod(orderId, method, userId)
}