package com.example.servyapp.ui.dishes

import com.example.servyapp.domain.model.Dish

data class DishesState (
    val idRestaurant: String? = null,

    val dishes: List<Dish> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigatetoDishDetails: NavigationEvent? = null
)

sealed class NavigationEvent {
    data class NavigateToDetail(val restaurantId: String, val dishId: String) : NavigationEvent()
}