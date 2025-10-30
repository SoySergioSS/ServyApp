package com.example.servyapp.ui.orderdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.manager.SessionManager
import com.example.servyapp.data.repository.CardRepository
import com.example.servyapp.data.repository.PedidoRepository
import com.example.servyapp.data.repository.UserRepository
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val sessionManager: SessionManager,
    private val cardRepository: CardRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailState())
    val uiState: StateFlow<OrderDetailState> = _uiState

    /**
     * Carga una orden específica por su ID.
     */
    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            val restaurantId = sessionManager.selectedRestaurantId.value
            Log.d("OrderDebug", "Intentando cargar orden: $orderId")
            Log.d("OrderDebug", "Restaurant ID actual: $restaurantId")

            if (restaurantId == null) {
                _uiState.update {
                    it.copy(errorMessage = "Restaurante no seleccionado", isLoading = false)
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = pedidoRepository.getOrderById(orderId, restaurantId)
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

    fun confirmOrder() = updateStatus(OrderStatus.IN_PROGRESS, "Pedido confirmado correctamente")

    fun cancelOrder() = updateStatus(OrderStatus.CANCELLED, "Pedido cancelado correctamente")

    fun completeOrder() = updateStatus(OrderStatus.COMPLETED, "Pago registrado correctamente")

    fun handleCashPayment(orderId: String) {
        viewModelScope.launch {
            val restaurantId = userRepository.getSelectedRestaurantId().value ?: return@launch
            pedidoRepository.updatePaymentMethod(orderId, "Efectivo", restaurantId)
            pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED,restaurantId)
            reloadOrder(orderId, restaurantId)
        }
    }

    fun handleYapePayment(orderId: String) {
        viewModelScope.launch {
            val restaurantId = userRepository.getSelectedRestaurantId().value ?: return@launch
            pedidoRepository.updatePaymentMethod(orderId, "Yape/Plin", restaurantId)
            pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, restaurantId)
            reloadOrder(orderId, restaurantId)
        }
    }

    fun handleCardPayment(orderId: String) {
        viewModelScope.launch {
            val restaurantId = userRepository.getSelectedRestaurantId().value ?: return@launch
            val userId = userRepository.getCurrentUserId()
            val card = userId?.let { cardRepository.getCard(it) }

            if (card != null) {
                pedidoRepository.updatePaymentMethod(orderId, "Tarjeta", restaurantId)
                pedidoRepository.updateOrderStatus(orderId, OrderStatus.COMPLETED, restaurantId)
                _uiState.update {
                    it.copy(successMessage = "Pago completado con tarjeta")
                }
                reloadOrder(orderId, restaurantId)
            } else {

                _uiState.update { it.copy(navigateToCard = true) }
            }
        }
    }

    fun navigationToCardComplete() {
        _uiState.update { it.copy(navigateToCard = false) }
    }

    private suspend fun reloadOrder(orderId: String, restaurantId: String) {
        val result = pedidoRepository.getOrderById(orderId, restaurantId)
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
            val restaurantId = sessionManager.selectedRestaurantId.value
            if (restaurantId == null) {
                _uiState.update {
                    it.copy(errorMessage = "Restaurante no seleccionado", isLoading = false)
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val result = pedidoRepository.updateOrderStatus(order.id, newStatus, restaurantId)
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
}