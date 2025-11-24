package com.example.servyapp.ui.chatbot

data class ChatbotState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean, // true = usuario, false = bot
    val timestamp: Long = System.currentTimeMillis()
)