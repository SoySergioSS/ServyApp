package com.example.servyapp.ui.plates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.DishRepository
import com.example.servyapp.data.repository.UserRepository
import com.example.servyapp.domain.model.Dish
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@HiltViewModel
class PlatesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dishRepository: DishRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(PlatesState())
    val uiState: StateFlow<PlatesState> = _uiState

    init {
        loadRestaurantDishes()
    }

    private fun loadRestaurantDishes() {
        viewModelScope.launch {
            userRepository.getSelectedRestaurantId()
                .collectLatest { restaurantId ->
                    if (restaurantId != null) {
                        _uiState.update { it.copy(idRestaurant = restaurantId, isLoading = true) }
                        loadDishes(restaurantId)
                    } else {
                        _uiState.update {
                            it.copy(
                                errorMessage = "No se ha seleccionado un restaurante",
                                isLoading = false,
                                dishes = emptyList()
                            )
                        }
                    }
                }
        }
    }

    private suspend fun loadDishes(restaurantId: String) {
        dishRepository.getDishesFromRestaurant(restaurantId)
            .collectLatest { result ->
                result.fold(
                    onSuccess = { dishes ->
                        _uiState.update {
                            it.copy(
                                dishes = dishes,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Error al cargar platillos: ${exception.message}",
                                isLoading = false
                            )
                        }
                    }
                )
            }
    }

    fun onDishClick(dish: Dish) {
        val restaurantId = _uiState.value.idRestaurant ?: return
        _uiState.update {
            it.copy(navigatetoPlateDetails = NavigationEvent.NavigateToDetail(restaurantId, dish.id))
        }
    }

    fun onNavigationEventHandled() {
        _uiState.update { it.copy(navigatetoPlateDetails = null) }
    }
}