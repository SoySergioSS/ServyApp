package com.example.servyapp.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.AnalyticsRepository
import com.example.servyapp.data.repository.AuthRepository
import com.example.servyapp.data.repository.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val auth: AuthRepository,
    private val restaurantRepository: RestaurantRepository
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
            if (uid == null) { /* ... */ return@launch }

            try {
                // primero se obtiene la lista de todos los restaurantes para traducir
                val restaurantList = restaurantRepository.getAllRestaurants()

                // se convierte la lista en un mapa de búsqueda (ID -> Nombre)
                val restaurantNameMap = restaurantList.associate { it.id to it.name }

                // obtiene los datos de analitica del user
                val result = analyticsRepository.getUserAnalytics(uid)

                result.fold(
                    onSuccess = { dataMap ->
                        val totalSpent = (dataMap["totalSpent"] as? Double) ?: 0.0
                        val totalOrders = (dataMap["totalOrders"] as? Long) ?: 0L

                        // para el grafico de barras
                        val dishCountMap = (dataMap["dishCount"] as? Map<String, Long>) ?: emptyMap()
                        val dishNameMap = (dataMap["dishNames"] as? Map<String, String>) ?: emptyMap()

                        val topDishes = dishCountMap.toList()
                            .sortedByDescending { it.second }
                            .take(5)
                            .map { (dishId, count) ->
                                val name = dishNameMap[dishId] ?: "Plato Desconocido"
                                Pair(name, count)
                            }

                        // --- TRADUCCIÓN DE GASTO MENSUAL (Gráfico de Líneas) ---
                        val monthlySpentMap = (dataMap["monthlySpent"] as? Map<String, Double>) ?: emptyMap()
                        val sortedMonthlySpent = monthlySpentMap.toList().sortedBy { it.first }

                        // --- TRADUCCIÓN DE RESTAURANTES (Gráfico de Pastel) ---
                        val restaurantSpentMap = (dataMap["restaurantSpent"] as? Map<String, Double>) ?: emptyMap()
                        val sortedRestaurantSpent = restaurantSpentMap.toList()
                            .sortedByDescending { it.second }
                            .map { (restId, gasto) ->
                                // Traducimos el ID a Nombre
                                val name = restaurantNameMap[restId] ?: "Rest. Desconocido"
                                Pair(name, gasto)
                            }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                totalSpent = totalSpent,
                                totalOrders = totalOrders,
                                topDishes = topDishes, // Lista traducida
                                monthlySpent = sortedMonthlySpent,
                                restaurantSpent = sortedRestaurantSpent // Lista traducida
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