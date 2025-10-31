package com.example.servyapp.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.manager.SessionManager
import com.example.servyapp.data.repository.PedidoRepository
import com.example.servyapp.domain.model.Order
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val pedidoRepository: PedidoRepository,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersState())
    val uiState: StateFlow<OrdersState> = _uiState

    init {
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Usuario no autenticado", isLoading = false
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            val restaurantId = sessionManager.selectedRestaurantId.value
            if (restaurantId == null) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Restaurante no seleccionado", isLoading = false
                    )
                }
                return@launch
            }

            pedidoRepository.getUserOrders(currentUser.uid).collectLatest { result ->
                    result.fold(onSuccess = { orders ->
                        _uiState.update {
                            it.copy(
                                orders = orders, isLoading = false, errorMessage = null
                            )
                        }
                    }, onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Error al cargar órdenes: ${exception.message}",
                                isLoading = false
                            )
                        }
                    })
            }
        }
    }

    fun onOrderClick(order: Order) {
        _uiState.update {
            it.copy(navigationEvent = NavigationEvent.NavigateToOrderDetail(order.id))
        }
    }

    fun onNavigationEventHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }

    fun refreshOrders() {
        loadOrders()
    }
}