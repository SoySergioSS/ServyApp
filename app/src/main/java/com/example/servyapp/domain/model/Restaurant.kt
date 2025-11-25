package com.example.servyapp.domain.model

data class Restaurant (
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val imageURL: String = "",
    val rating: Double = 0.0,

    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)