package com.example.servyapp.ui.platedetail

import com.example.servyapp.domain.model.Dish

data class PlateDetailState(
    val dish: Dish? = null,
    val quantity: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val addedToCart: Boolean = false,

    val navigateToCart: Boolean = false
)