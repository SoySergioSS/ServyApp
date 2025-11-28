package com.example.servyapp.ui.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.BuildConfig
import com.example.servyapp.data.repository.DishRepository
import com.example.servyapp.data.repository.RestaurantRepository
import com.example.servyapp.data.repository.UserRepository
import com.example.servyapp.domain.model.Dish
import com.example.servyapp.domain.model.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val dishRepository: DishRepository,
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotState())
    val uiState: StateFlow<ChatbotState> = _uiState

    // Lista en memoria de restaurantes para detección rápida
    private var cachedRestaurants: List<Restaurant> = emptyList()

    // Inicialización (Si usas API Key o Firebase Vertex AI, ajusta aquí)
    // Asumo que usas API Key con google-generativeai como acordamos al final
    private val generativeModel = com.google.ai.client.generativeai.GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val chat = generativeModel.startChat()

    init {
        loadRestaurants()
    }

    // Carga los nombres de restaurantes al inicio
    private fun loadRestaurants() {
        viewModelScope.launch {
            try {
                cachedRestaurants = restaurantRepository.getAllRestaurants()
            } catch (e: Exception) {
                // Error silencioso, reintentaremos luego si es necesario
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(text, true)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            try {
                // ESTRATEGIA INTELIGENTE:
                // 1. ¿El usuario mencionó un restaurante?
                val mentionedRestaurant = findRestaurantInText(text)

                val contextDishes: List<Dish> = if (mentionedRestaurant != null) {
                    // CASO A: Sí mencionó restaurante -> Traemos TODO su menú
                    val result = dishRepository.getDishesByRestaurant(mentionedRestaurant.id)
                    result.getOrNull() ?: emptyList()
                } else {
                    // CASO B: No mencionó restaurante -> Buscamos por nombre de plato (aprox)
                    val keyword = extractPossibleDishName(text)
                    val result = dishRepository.searchDishesByName(keyword)
                    result.getOrNull() ?: emptyList()
                }

                // 2. Construimos el Prompt con lo que encontramos
                val prompt = buildPrompt(text, contextDishes, mentionedRestaurant?.name)

                // 3. Enviamos a Gemini
                val response = chat.sendMessage(prompt)
                val botResponseText = response.text ?: "Lo siento, no tengo respuesta."

                val botMsg = ChatMessage(botResponseText, false)
                _uiState.update { it.copy(messages = it.messages + botMsg, isLoading = false) }

            } catch (e: Exception) {
                val errorMsg = ChatMessage("Error: ${e.message}", false)
                _uiState.update { it.copy(messages = it.messages + errorMsg, isLoading = false) }
            }
        }
    }

    // Busca si el texto del usuario contiene el nombre de algún restaurante
    private fun findRestaurantInText(text: String): Restaurant? {
        val normalizedText = normalizeString(text)
        return cachedRestaurants.find { restaurant ->
            val normalizedName = normalizeString(restaurant.name)
            // Buscamos coincidencia parcial (ej: "La Criolla" en "recomienda algo de La Criolla")
            normalizedText.contains(normalizedName)
        }
    }

    // Quita acentos y pone en minúsculas para comparar mejor
    private fun normalizeString(input: String): String {
        val text = Normalizer.normalize(input, Normalizer.Form.NFD)
        return text.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    // Tu función auxiliar existente (la dejamos por si acaso)
    private fun extractPossibleDishName(text: String): String {
        val stopWords = listOf("el", "la", "los", "las", "un", "una", "de", "que", "tienen", "tiene", "cuanto", "cuantas", "calorias", "dime", "sobre", "recomiendas", "platillo", "plato", "informacion", "info")
        val keywords = text.lowercase().split(" ")
            .filter { !stopWords.contains(it) }
            .filter { it.length > 3 }
        return if (keywords.isNotEmpty()) keywords.first().replaceFirstChar { it.uppercase() } else text
    }

    private fun buildPrompt(userQuery: String, dishes: List<Dish>, restaurantName: String?): String {
        val contextInfo = if (dishes.isNotEmpty()) {
            dishes.joinToString("\n\n") { dish ->
                """
                - ${dish.name} (${restaurantName ?: "General"}):
                  Desc: ${dish.description}
                  Precio: S/ ${dish.price}
                  Info Nutricional:
                    Calorías: ${dish.nutrition?.calories ?: "??"} kcal
                    Proteínas: ${dish.nutrition?.protein ?: "??"} g
                    Grasas: ${dish.nutrition?.fat ?: "??"} g
                    Carbohidratos: ${dish.nutrition?.carbs ?: "??"} g
                    Alérgenos: ${dish.nutrition?.allergens?.joinToString() ?: "Ninguno"}
                """.trimIndent()
            }
        } else {
            "No encontré información relevante en el menú actual."
        }

        return """
            Eres 'Servy', un experto nutricional de restaurantes.
            
            CONTEXTO (Menú disponible):
            $contextInfo
            
            PREGUNTA DEL USUARIO: "$userQuery"
            
            INSTRUCCIONES:
            1. Usa el CONTEXTO para responder. Si el usuario pide algo vago (ej: "Tallarin verde"), busca en el contexto el plato más parecido (ej: "Tallarines verdes") y asume que se refiere a ese.
            2. Si el usuario pide recomendaciones (ej: "bajos carbohidratos"), analiza los datos nutricionales de todos los platos listados arriba y sugiere el mejor.
            3. Sé amable y breve.
        """.trimIndent()
    }
}