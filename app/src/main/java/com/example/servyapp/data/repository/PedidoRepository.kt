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
    suspend fun createOrder(order: Order): Result<String> =
        pedidoRemoteDataSource.createOrder(order)

    fun getUserOrders(userId: String): Flow<Result<List<Order>>> =
        pedidoRemoteDataSource.getUserOrders(userId)

    suspend fun getOrderById(orderId: String): Result<Order> =
        pedidoRemoteDataSource.getOrderById(orderId)

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> =
        pedidoRemoteDataSource.updateOrderStatus(orderId, status)

    suspend fun addPedidoToOrder(orderId: String, pedido: Pedido): Result<Unit> =
        pedidoRemoteDataSource.addPedidoToOrder(orderId, pedido)

    suspend fun removePedidoFromOrder(orderId: String, pedidoId: String): Result<Unit> =
        pedidoRemoteDataSource.removePedidoFromOrder(orderId, pedidoId)

    suspend fun updatePedidoStatus(orderId: String, pedidoId: String, status: PedidoStatus): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoStatus(orderId, pedidoId, status)

    suspend fun updatePedidoItemQuantity(
        orderId: String,
        pedidoId: String,
        dishId: String,
        newQuantity: Int
    ): Result<Unit> =
        pedidoRemoteDataSource.updatePedidoItemQuantity(orderId, pedidoId, dishId, newQuantity)

    suspend fun deleteOrder(orderId: String): Result<Unit> =
        pedidoRemoteDataSource.deleteOrder(orderId)
}