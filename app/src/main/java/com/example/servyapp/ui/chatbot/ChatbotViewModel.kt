package com.example.servyapp.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.DishRepository
import com.example.servyapp.domain.model.Dish
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.servyapp.BuildConfig
import android.util.Log

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val dishRepository: DishRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotState())
    val uiState: StateFlow<ChatbotState> = _uiState

    // Inicializa Gemini
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Mostrar mensaje del usuario inmediatamente
        val userMsg = ChatMessage(text, true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            try {
                // 2. Buscar información en Firestore (RAG - Retrieval Augmented Generation)
                // Usamos la función de búsqueda que definimos en DishRepository
                val searchResult = dishRepository.searchDishesByName(text)
                val relevantDishes = searchResult.getOrNull() ?: emptyList()

                // 3. Construir el Prompt con el contexto de los platillos encontrados
                val prompt = buildPrompt(text, relevantDishes)

                // 4. Enviar a Gemini
                val response = generativeModel.generateContent(prompt)
                val botResponseText = response.text ?: "Lo siento, no pude generar una respuesta."

                // 5. Mostrar respuesta del bot
                val botMsg = ChatMessage(botResponseText, false)
                _uiState.update { it.copy(messages = it.messages + botMsg, isLoading = false) }

            } catch (e: Exception) {
                //borrar, es para debug
                Log.e("SERVY_ERROR", "Ocurrió un error en el Chatbot: ${e.message}", e)

                val errorMsg = ChatMessage("Error de conexión: ${e.message}", false)
                _uiState.update { it.copy(messages = it.messages + errorMsg, isLoading = false) }
            }
        }
    }

    private fun buildPrompt(userQuery: String, dishes: List<Dish>): String {
        val contextInfo = if (dishes.isNotEmpty()) {
            dishes.joinToString("\n\n") { dish ->
                """
                PLATILLO: ${dish.name}
                - Descripción: ${dish.description}
                - Precio: S/ ${dish.price}
                - Calorías: ${dish.nutrition?.calories ?: "N/A"} kcal
                - Proteínas: ${dish.nutrition?.protein ?: "N/A"} g
                - Grasas: ${dish.nutrition?.fat ?: "N/A"} g
                - Alérgenos: ${dish.nutrition?.allergens?.joinToString() ?: "Ninguno"}
                """.trimIndent()
            }
        } else {
            "No encontré platillos con ese nombre exacto en el menú."
        }

        return """
            Eres 'Servy', un asistente nutricional experto y amable de la app de restaurantes ServyApp.
            
            CONTEXTO DE LA BASE DE DATOS (Lo que encontré en el menú):
            $contextInfo
            
            PREGUNTA DEL USUARIO: "$userQuery"
            
            INSTRUCCIONES:
            - Usa SOLO la información del contexto para responder sobre valores nutricionales.
            - Si el contexto dice "No encontré platillos", di amablemente que no tienes información sobre ese platillo específico.
            - Si hay datos, responde la duda del usuario de forma resumida y amigable.
            - No inventes datos que no estén en el contexto.
        """.trimIndent()
    }
}