package com.example.servyapp.ui.plates

import com.example.servyapp.domain.model.Dish

data class PlatesState (
    val idRestaurant: String? = null,

    val dishes: List<Dish> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigatetoPlateDetails: NavigationEvent? = null
)

sealed class NavigationEvent {
    data class NavigateToDetail(val restaurantId: String, val dishId: String) : NavigationEvent()
}