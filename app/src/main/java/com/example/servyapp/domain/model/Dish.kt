package com.example.servyapp.domain.model

import com.google.firebase.firestore.PropertyName

data class Dish(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageURL: String = "",
    val enable: Boolean = true,
    val category: String = "",
    val nutrition: NutritionInfo?=null
)

data class NutritionInfo(
    val calories: Int = 0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val allergens: List<String> = emptyList() //["gluten", "lactosa"]
)