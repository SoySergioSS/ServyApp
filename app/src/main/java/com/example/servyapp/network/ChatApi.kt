package com.example.servyapp.network

import com.example.servyapp.domain.model.ChatRequest
import com.example.servyapp.domain.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApi {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}