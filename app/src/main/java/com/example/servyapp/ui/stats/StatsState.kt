package com.example.servyapp.ui.stats

data class StatsState(
    val isLoading: Boolean = false,
    val totalSpent: Double = 0.0,
    val totalOrders: Long = 0,
    // la lista guardará el ID del platillo y la cantidad pedida
    val topDishes: List<Pair<String, Long>> = emptyList(),
    val errorMessage: String? = null
)