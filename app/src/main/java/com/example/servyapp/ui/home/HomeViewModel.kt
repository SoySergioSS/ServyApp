package com.example.servyapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.RestaurantRepository
import com.example.servyapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState

    init {
        getAllRestaurants()
    }

    fun getAllRestaurants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val restaurants = restaurantRepository.getAllRestaurants()
                _uiState.update { it.copy(isLoading = false, restaurants = restaurants) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun saveSelectedRestaurant(restaurantId: String) {
        viewModelScope.launch {
            userRepository.saveSelectedRestaurantId(restaurantId)
            _uiState.update { it.copy(navigateToDishes = true) }
        }
    }

    fun navigationToDishesComplete() { //para que se pueda volver usando el boton de atras del sistema
        _uiState.update { it.copy(navigateToDishes = false) }
    }
}