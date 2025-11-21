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
import com.example.servyapp.domain.model.VisualItem
import com.example.servyapp.network.ApiClient

class WaiterViewModel : ViewModel() {

    private val _history = mutableStateListOf<HistoryItem>()
    val history: List<HistoryItem> get() = _history

    // Estado de la UI Visual (La Grilla)
    var visualItems by mutableStateOf<List<VisualItem>>(emptyList())
        private set

    var currentPhase by mutableStateOf("initial") // 'restaurants', 'categories', etc.
        private set

    var lastReply by mutableStateOf<String?>(null)
        private set

    var userMessage by mutableStateOf("")
        private set

    var aiMessage by mutableStateOf("") // Lo que el TTS debe leer
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

//    fun sendMessage() {
//        val text = userMessage.trim()
//        if (text.isEmpty()) return
//
//        isLoading = true
//        errorMessage = null
//        replyMessage = ""
//
//        viewModelScope.launch {
//            try {
//                val request = ChatRequest(text, _history.toList())
//                Log.d("ChatDebug", "⟹ REQUEST ENVIADO: $request")
//                val response = ApiClient.chatApi.chat(request)
//
//                _history.add(HistoryItem("user", text))
//                _history.add(HistoryItem("model", response.reply))
//
//                replyMessage = response.reply
//            } catch (e: Exception) {
//                errorMessage = e.message ?: "Error desconocido"
//            } finally {
//                isLoading = false
//            }
//        }
//    }

    fun sendVoiceMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        userMessage = clean
        sendToBackend(clean)
    }

    // Método principal para enviar texto (voz o escrito)
    fun sendToBackend(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        isLoading = true
        errorMessage = null
        userMessage = cleanText // Actualizamos UI inmediata

        viewModelScope.launch {
            try {
                val request = ChatRequest(cleanText, _history.toList())
                Log.d("ChatDebug", "Enviando: $cleanText")

                // Llamada a tu Backend (asegúrate que ApiClient apunte a tu nuevo backend)
                val response = ApiClient.chatApi.chat(request)

                // 1. Actualizar Historial
                _history.add(HistoryItem("user", cleanText))
                _history.add(HistoryItem("model", response.aiMessage))

                // 2. Actualizar Texto para TTS
                aiMessage = response.aiMessage

                // 3. Actualizar UI Visual (Si el backend mandó datos)
                response.screenData?.let { data ->
                    currentPhase = data.phase
                    visualItems = data.items
                    Log.d("ChatDebug", "UI Actualizada ${data.phase}: ${data.items.size} items")
                }

                Log.d("Historial", _history.toString())

                Log.d("ScreenData", response.screenData.toString())

            } catch (e: Exception) {
                errorMessage = e.message ?: "Error de conexión"
                Log.e("ChatError", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Cuando el usuario hace clic en una tarjeta visual
    fun onVisualItemClicked(item: VisualItem) {
        // Simulamos que el usuario dijo el nombre del item para navegar
        // Esto permite que la IA entienda el contexto ("Quiero ver [Nombre del Item]")
        val intentMessage = "Ver ${item.title}"
        sendToBackend(intentMessage)
    }
}