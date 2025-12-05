package com.example.servyapp.domain.model

data class HistoryItem(
    val role: String,   // "user" o "model"
    val text: String
)