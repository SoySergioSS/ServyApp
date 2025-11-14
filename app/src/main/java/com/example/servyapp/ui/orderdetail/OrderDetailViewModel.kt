package com.example.servyapp.ui.orderdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.AnalyticsRepository
import com.example.servyapp.data.repository.CardRepository
import com.example.servyapp.data.repository.PedidoRepository
import com.example.servyapp.data.repository.RestaurantRepository
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val restaurantRepository: RestaurantRepository,
    private val cardRepository: CardRepository,
    private val auth: FirebaseAuth,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailState())
    val uiState: StateFlow<OrderDetailState> = _uiState

    /**
     * Carga una orden específica por su ID.
     */
    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.update {
                    it.copy(errorMessage = "Usuario no autenticado", isLoading = false)
                }
                return@launch
            }

            Log.d("OrderDebug", "Intentando cargar orden: $orderId")
            Log.d("OrderDebug", "User ID: ${currentUser.uid}")

            _uiState.update { it.copy(isLoading = true) }

            val result = pedidoRepository.getOrderById(orderId, currentUser.uid)
            result.fold(
                onSuccess = { order ->
                    Log.d("OrderDebug", "Orden encontrada: ${order.id}")
                    _uiState.update { it.copy(order = order, isLoading = false, errorMessage = null) }
                },
                onFailure = { e ->
                    Log.e("OrderDebug", "Error al cargar orden: ${e.message}", e)
                    _uiState.update { it.copy(errorMessage = "Error al cargar pedido: ${e.message}", isLoading = false) }
                }
            )
        }
    }

    fun validateQrAndConfirmOrder(scannedContent: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val order = _uiState.value.order
            val userId = auth.currentUser?.uid

            // 1. Validaciones básicas
            if (order == null || userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No se pudo verificar la orden.") }
                return@launch
            }
            if (scannedContent.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "QR inválido o vacío.") }
                return@launch
            }

            // 2. Se extrae el ID del restaurante del QR
            val prefix = "https://servy.app/checkin/"
            val scannedRestaurantId = if (scannedContent.startsWith(prefix)) {
                scannedContent.removePrefix(prefix)
            } else {
                null // El QR no tiene el formato esperado
            }

            // 3. Obtiene el id del restaurante de la orden actual
             val orderRestaurantId = order.pedidos.firstOrNull()?.restaurantId

            // 4. Compara los IDs
            if (scannedRestaurantId != null && scannedRestaurantId == orderRestaurantId) {
                // coincide


                val assigned = restaurantRepository.assignTableSecure(
                    restaurantId = orderRestaurantId!!,
                    requiredSeats = order.requiredSeats,
                    orderId = order.id
                )

                if (assigned == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No hay mesas disponibles en este momento."
                        )
                    }
                    return@launch
                }

                val updated = restaurantRepository.updateOrderWithTable(
                    orderId = order.id,
                    restaurantId = orderRestaurantId,
                    tableId = assigned.id,
                    tableNumber = assigned.number
                )

                if (!updated) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No se pudo asignar la mesa."
                        )
                    }
                    return@launch
                }


                updateStatus(
                    OrderStatus.IN_PROGRESS,
                    "¡Orden confirmada exitosamente! Mesa asignada: ${assigned.number}"
                )
            }else {
                // QR no coincide
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "QR Incorrecto. Esta orden no pertenece a este restaurante."
                    )
                }
            }
        }
    }

    fun cancelOrder() = updateStatus(OrderStatus.CANCELLED, "Pedido cancelado correctamente")

    fun completeOrder() = updateStatus(OrderStatus.COMPLETED, "Pago registrado correctamente")

    fun handleCashPayment(orderId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch


            pedidoRepository.updatePaymentMethod(orderId, "Efectivo", userId)
            pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, userId)

            reloadOrder(orderId, userId)

            // liberar mesa usando solo orderId
            val released = restaurantRepository.releaseTable(orderId)
            Log.e("PAYMENT", "Mesa liberada: $released")


            _uiState.value.order?.let { updateUserAnalytics(it, userId) }

            _uiState.update {
                it.copy(successMessage = "Pago completado en efectivo")
            }
        }
    }

    fun handleYapePayment(orderId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            pedidoRepository.updatePaymentMethod(orderId, "Yape/Plin", userId)
            pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, userId)
            reloadOrder(orderId, userId)


            // liberar mesa usando solo orderId
            val released = restaurantRepository.releaseTable(orderId)
            Log.e("PAYMENT", "Mesa liberada: $released")

            _uiState.value.order?.let { updateUserAnalytics(it, userId) }

            _uiState.update {
                it.copy(successMessage = "Pago completado con Yape/Plin")
            }
        }
    }

    fun handleCardPayment(orderId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val card = cardRepository.getCard(userId)

            if (card != null) {
                pedidoRepository.updatePaymentMethod(orderId, "Tarjeta", userId)
                pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, userId)
                reloadOrder(orderId, userId)


                // liberar mesa usando solo orderId
                val released = restaurantRepository.releaseTable(orderId)
                Log.e("PAYMENT", "Mesa liberada: $released")

                _uiState.value.order?.let { updateUserAnalytics(it, userId) }

                _uiState.update {
                    it.copy(successMessage = "Pago completado con tarjeta")
                }
            } else {
                _uiState.update { it.copy(navigateToCard = true) }
            }
        }
    }

    fun navigationToCardComplete() {
        _uiState.update { it.copy(navigateToCard = false) }
    }

    private suspend fun reloadOrder(orderId: String, userId: String) {
        val result = pedidoRepository.getOrderById(orderId, userId)
        result.onSuccess { updatedOrder ->
            _uiState.update { it.copy(order = updatedOrder) }
        }.onFailure { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "Error al recargar el pedido") }
        }
    }

    /**
     * Actualiza el estado del pedido en Firebase.
     */
    private fun updateStatus(newStatus: OrderStatus, successMessage: String) {
        viewModelScope.launch {
            val order = _uiState.value.order ?: return@launch
            val userId = auth.currentUser?.uid

            if (userId == null) {
                _uiState.update {
                    it.copy(errorMessage = "Usuario no autenticado", isLoading = false)
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = pedidoRepository.updateOrderStatus(order.id, newStatus, userId)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            order = order.copy(status = newStatus),
                            successMessage = successMessage,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "Error al actualizar estado: ${e.message}", isLoading = false) }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun cancelPedido(pedidoId: String) {
        viewModelScope.launch {
            val order = _uiState.value.order ?: return@launch
            val userId = auth.currentUser?.uid

            if (userId == null) {
                _uiState.update { it.copy(errorMessage = "Usuario no autenticado", isLoading = false) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            // Usamos la función del repositorio que modificamos
            val result = pedidoRepository.removePedidoFromOrder(order.id, pedidoId, userId)

            result.fold(
                onSuccess = {
                    // Recargamos la orden para obtener el estado actualizado
                    // (ya que la orden completa podría haberse cancelado)
                    reloadOrder(order.id, userId)
                    _uiState.update {
                        it.copy(
                            successMessage = "Pedido eliminado de la orden",
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Error al eliminar pedido: ${e.message}",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun updateUserAnalytics(order: Order, userId: String) {
        viewModelScope.launch {
            val result = analyticsRepository.updateUserAnalytics(userId, order)

            if (result.isFailure) {
                Log.e("StatsErrorVM", "Falló la actualización de analíticas: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    fun setRequiredSeats(seats: Int) {
        _uiState.update { it.copy(order = it.order?.copy(requiredSeats = seats)) }
    }



}



