package com.example.servyapp.ui.platedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.CartRepository
import com.example.servyapp.data.repository.DishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlateDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dishRepository: DishRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlateDetailState())
    val uiState: StateFlow<PlateDetailState> = _uiState

    private val dishId: String? = savedStateHandle["dishId"]
    private val restaurantId: String? = savedStateHandle["restaurantId"]

    init {
        loadDishDetail()
    }

    private fun loadDishDetail() {
        if (dishId == null || restaurantId == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Información del platillo no disponible",
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            dishRepository.getDishesFromRestaurant(restaurantId)
                .collect { result ->
                    result.fold(
                        onSuccess = { dishes ->
                            val dish = dishes.find { it.id == dishId }
                            _uiState.update {
                                it.copy(
                                    dish = dish,
                                    isLoading = false,
                                    errorMessage = if (dish == null) "Platillo no encontrado" else null
                                )
                            }
                        },
                        onFailure = { exception ->
                            _uiState.update {
                                it.copy(
                                    errorMessage = "Error al cargar el platillo: ${exception.message}",
                                    isLoading = false
                                )
                            }
                        }
                    )
                }
        }
    }

    fun incrementQuantity() {
        _uiState.update { it.copy(quantity = it.quantity + 1) }
    }

    fun decrementQuantity() {
        val currentQuantity = _uiState.value.quantity
        if (currentQuantity > 1) {
            _uiState.update { it.copy(quantity = currentQuantity - 1) }
        }
    }

    fun addToCart() {
        val dish = _uiState.value.dish ?: return
        val quantity = _uiState.value.quantity
        val restId = restaurantId ?: return

        // Agregar al carrito usando el repository
        cartRepository.addToCart(dish, quantity, restId)

        _uiState.update { it.copy(addedToCart = true) }

        // Resetear el estado después de un tiempo
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(addedToCart = false) }
        }
    }

    fun resetAddedToCartState() {
        _uiState.update { it.copy(addedToCart = false) }
    }
}