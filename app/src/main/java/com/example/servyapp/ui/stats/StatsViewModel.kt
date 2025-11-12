package com.example.servyapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.AnalyticsRepository
import com.example.servyapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsState())
    val uiState: StateFlow<StatsState> = _uiState

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Usuario no encontrado") }
                return@launch
            }

            try {
                val result = analyticsRepository.getUserAnalytics(uid)

                result.fold(
                    onSuccess = { dataMap ->
                        val totalSpent = (dataMap["totalSpent"] as? Double) ?: 0.0
                        val totalOrders = (dataMap["totalOrders"] as? Long) ?: 0L
                        val dishCountMap = (dataMap["dishCount"] as? Map<String, Long>) ?: emptyMap()

                        val topDishes = dishCountMap.toList()
                            .sortedByDescending { it.second }
                            .take(5)

                        val monthlySpentMap = (dataMap["monthlySpent"] as? Map<String, Double>) ?: emptyMap()
                        val sortedMonthlySpent = monthlySpentMap.toList().sortedBy { it.first }

                        // Gráfico de Pastel (Gasto por Restaurante)
                        val restaurantSpentMap = (dataMap["restaurantSpent"] as? Map<String, Double>) ?: emptyMap()
                        val sortedRestaurantSpent = restaurantSpentMap.toList()
                            .sortedByDescending { it.second }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                totalSpent = totalSpent,
                                totalOrders = totalOrders,
                                topDishes = topDishes,
                                monthlySpent = sortedMonthlySpent,
                                restaurantSpent = sortedRestaurantSpent
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = exception.message)
                        }
                    }
                )

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}