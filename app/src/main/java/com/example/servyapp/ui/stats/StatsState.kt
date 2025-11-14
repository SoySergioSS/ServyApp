package com.example.servyapp.ui.stats

data class StatsState(
    val isLoading: Boolean = false,
    val totalSpent: Double = 0.0,
    val totalOrders: Long = 0,

    // (String = "Nombre Platillo", Long = Cantidad)
    val topDishes: List<Pair<String, Long>> = emptyList(),

    // (String = "YYYY-MM", Double = Gasto)
    val monthlySpent: List<Pair<String, Double>> = emptyList(),

    // (String = "Nombre Restaurante", Double = Gasto)
    val restaurantSpent: List<Pair<String, Double>> = emptyList(),

    val errorMessage: String? = null
)