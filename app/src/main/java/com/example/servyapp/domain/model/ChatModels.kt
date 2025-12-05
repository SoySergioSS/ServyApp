package com.example.servyapp.domain.model

data class ChatRequest(val message: String, val history: List<HistoryItem> = emptyList())
// Respuesta enriquecida del Backend
data class ChatResponse(
    val aiMessage: String,          // Texto para que el robot lo lea
    val screenData: ScreenData? = null // Datos para actualizar la UI
)

data class ScreenData(
    val phase: String, // "restaurants", "categories", "dishes"
    val items: List<VisualItem>
)

// Elemento genérico para mostrar en la grilla (igual que en la web)
data class VisualItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageSeed: String,
    val type: String // "restaurant", "category", "dish"
)