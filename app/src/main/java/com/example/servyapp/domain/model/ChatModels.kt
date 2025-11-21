package com.example.servyapp.domain.model

data class ChatRequest(val message: String, val history: List<HistoryItem> = emptyList())
data class ChatResponse(val reply: String)