package com.example.servyapp.ui.home

import com.example.servyapp.domain.model.Restaurant

data class HomeState (
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val errorMessage: String? = null,
    val navigateToPlates: Boolean = false
)