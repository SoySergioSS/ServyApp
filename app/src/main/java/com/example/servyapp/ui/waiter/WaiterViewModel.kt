package com.example.servyapp.ui.waiter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.example.servyapp.domain.model.ChatRequest
import com.example.servyapp.domain.model.HistoryItem
import com.example.servyapp.network.ApiClient

class WaiterViewModel : ViewModel() {

    private val _history = mutableStateListOf<HistoryItem>()
    val history: List<HistoryItem> get() = _history

    var lastReply by mutableStateOf<String?>(null)
        private set

    var userMessage by mutableStateOf("")
        private set

    var replyMessage by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onMessageChange(newValue: String) {
        userMessage = newValue
    }

    fun sendMessage() {
        val text = userMessage.trim()
        if (text.isEmpty()) return

        isLoading = true
        errorMessage = null
        replyMessage = ""

        viewModelScope.launch {
            try {
                val request = ChatRequest(text, _history.toList())
                Log.d("ChatDebug", "⟹ REQUEST ENVIADO: $request")
                val response = ApiClient.chatApi.chat(request)

                _history.add(HistoryItem("user", text))
                _history.add(HistoryItem("model", response.reply))

                replyMessage = response.reply
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
            } finally {
                isLoading = false
            }
        }
    }

    fun sendVoiceMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        userMessage = clean
        sendToBackend(clean)
    }

    private fun sendToBackend(text: String) {
        isLoading = true
        errorMessage = null
        replyMessage = ""

        viewModelScope.launch {
            try {
                val request = ChatRequest(text, _history.toList())
                Log.d("ChatDebug", "⟹ REQUEST ENVIADO: $request")
                val response = ApiClient.chatApi.chat(request)

                _history.add(HistoryItem("user", text))
                _history.add(HistoryItem("model", response.reply))

                replyMessage = response.reply
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
            } finally {
                isLoading = false
            }
        }
    }
}