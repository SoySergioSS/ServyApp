package com.example.servyapp.domain.model

data class Dish(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageURL: String = "",
    val enable: Boolean = true,
    val category: String = ""
)