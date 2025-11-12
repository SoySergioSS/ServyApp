package com.example.servyapp.ui.stats

data class StatsState(
    val isLoading: Boolean = false,
    val totalSpent: Double = 0.0,
    val totalOrders: Long = 0,
    val topDishes: List<Pair<String, Long>> = emptyList(), // Para el Gráfico de Barras

    // (String = "YYYY-MM", Double = Gasto)
    val monthlySpent: List<Pair<String, Double>> = emptyList(), // Para el Gráfico de Líneas

    // (String = "ID Restaurante", Double = Gasto)
    val restaurantSpent: List<Pair<String, Double>> = emptyList(), // Para el Gráfico de Pastel

    val errorMessage: String? = null
)