package com.example.servyapp.domain.model

data class Table(
    val id: String = "",
    val number: Int = 0,
    val seats: Int = 4,
    val isOccupied: Boolean = false,
    val currentOrderId: String? = null
)
