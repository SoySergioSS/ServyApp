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
    suspend fun createOrder(restaurantId: String, order: Order): Result<String> =
        pedidoRemoteDataSource.createOrder(restaurantId, order)

    suspend fun getActiveOrder(restaurantId: String, userId: String): Result<Order?> =
        pedidoRemoteDataSource.getActiveOrder(restaurantId, userId)

    fun getUserOrders(restaurantId: String, userId: String): Flow<Result<List<Order>>> =
        pedidoRemoteDataSource.getUserOrders(restaurantId, userId)

    suspend fun getOrderById(restaurantId: String, orderId: String): Result<Order> =
        pedidoRemoteDataSource.getOrderById(restaurantId, orderId)

    suspend fun updateOrderStatus(restaurantId: String, orderId: String, status: OrderStatus): Result<Unit> =
        pedidoRemoteDataSource.updateOrderStatus(restaurantId, orderId, status)

    suspend fun addPedidoToOrder(restaurantId: String, orderId: String, pedido: Pedido): Result<Unit> =
        pedidoRemoteDataSource.addPedidoToOrder(restaurantId, orderId, pedido)

    suspend fun removePedidoFromOrder(restaurantId: String, orderId: String, pedidoId: String): Result<Unit> =
        pedidoRemoteDataSource.removePedidoFromOrder(restaurantId, orderId, pedidoId)

    suspend fun updatePedidoStatus(
        restaurantId: String,
        orderId: String,
        pedidoId: String,
        status: PedidoStatus
    ): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoStatus(restaurantId, orderId, pedidoId, status)

    suspend fun deleteOrder(restaurantId: String, orderId: String): Result<Unit> =
        pedidoRemoteDataSource.deleteOrder(restaurantId, orderId)
}