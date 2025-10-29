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
    suspend fun createOrder(order: Order, restaurantId: String): Result<String> =
        pedidoRemoteDataSource.createOrder(order, restaurantId)

    fun getUserOrders(userId: String, restaurantId: String): Flow<Result<List<Order>>> =
        pedidoRemoteDataSource.getUserOrders(userId, restaurantId)

    suspend fun getOrderById(orderId: String, restaurantId: String): Result<Order> =
        pedidoRemoteDataSource.getOrderById(orderId, restaurantId)

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, restaurantId: String): Result<Unit> =
        pedidoRemoteDataSource.updateOrderStatus(orderId, status, restaurantId)

    suspend fun addPedidoToOrder(orderId: String, pedido: Pedido, restaurantId: String): Result<Unit> =
        pedidoRemoteDataSource.addPedidoToOrder(orderId, pedido, restaurantId)

    suspend fun removePedidoFromOrder(orderId: String, pedidoId: String, restaurantId: String): Result<Unit> =
        pedidoRemoteDataSource.removePedidoFromOrder(orderId, pedidoId, restaurantId)

    suspend fun updatePedidoStatus(orderId: String, pedidoId: String, status: PedidoStatus, restaurantId: String): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoStatus(orderId, pedidoId, status, restaurantId)

    suspend fun updatePedidoItemQuantity(
        orderId: String,
        pedidoId: String,
        dishId: String,
        newQuantity: Int,
        restaurantId: String
    ): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoItemQuantity(orderId, pedidoId, dishId, newQuantity, restaurantId)

    suspend fun deleteOrder(orderId: String, restaurantId: String): Result<Unit> =
        pedidoRemoteDataSource.deleteOrder(orderId, restaurantId)
}