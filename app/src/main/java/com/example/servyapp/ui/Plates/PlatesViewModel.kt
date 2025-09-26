package com.example.servyapp.ui.Plates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlatesViewModel @Inject constructor(
    private val userRepository: UserRepository //solo estoy viendo si se guarda el idrestaurant, esto no irá
): ViewModel() {
    private val _uiState = MutableStateFlow(PlatesState())
    val uiState: StateFlow<PlatesState> = _uiState

    init {
        loadIdRestaurant()
    }

    private fun loadIdRestaurant(){
        viewModelScope.launch {
            userRepository.getSelectedRestaurantId()
                .collect { newId ->
                    _uiState.update { currentState ->
                        currentState.copy(idRestaurant = newId)
                    }
                }
        }
    }

}