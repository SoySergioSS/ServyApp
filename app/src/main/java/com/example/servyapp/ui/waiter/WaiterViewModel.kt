package com.example.servyapp.ui.waiter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.example.servyapp.data.manager.CartManager
import com.example.servyapp.data.repository.PedidoRepository
import com.example.servyapp.data.repository.RestaurantRepository
import com.example.servyapp.domain.model.ChatRequest
import com.example.servyapp.domain.model.Dish
import com.example.servyapp.domain.model.HistoryItem
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoItem
import com.example.servyapp.domain.model.PedidoStatus
import com.example.servyapp.domain.model.VisualItem
import com.example.servyapp.network.ApiClient
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class WaiterViewModel @Inject constructor(
    private val cartManager: CartManager, // <--- 1. Inyectamos CartManager
    private val pedidoRepository: PedidoRepository,
    private val restaurantRepository: RestaurantRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ... (El resto de tus estados: _history, visualItems, etc. se mantienen igual) ...
    private val _history = mutableStateListOf<HistoryItem>()
    val history: List<HistoryItem> get() = _history

    val cartItems = cartManager.cartItems // <--- 2. Accedemos a los ítems del carrito

    var visualItems by mutableStateOf<List<VisualItem>>(emptyList())
        private set

    var currentPhase by mutableStateOf("initial")
        private set

    var userMessage by mutableStateOf("")
        private set

    var aiMessage by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    // 2. Nuevo estado para el Snackbar
    var snackbarMessage by mutableStateOf<String?>(null)
        private set

    fun onMessageChange(newValue: String) {
        userMessage = newValue
    }

    fun clearSnackbarMessage() {
        snackbarMessage = null
    }

    fun sendVoiceMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        userMessage = clean
        sendToBackend(clean)
    }

    fun sendToBackend(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        isLoading = true
        errorMessage = null
        userMessage = cleanText

        viewModelScope.launch {
            try {
                val request = ChatRequest(cleanText, _history.toList())
                val response = ApiClient.chatApi.chat(request)

                _history.add(HistoryItem("user", cleanText))
                _history.add(HistoryItem("model", response.aiMessage))

                aiMessage = response.aiMessage

                // 3. Lógica principal: Interceptamos "ADD_TO_CART"
                response.screenData?.let { data ->
                    when (data.phase) {
                        "ADD_TO_CART" -> handleAddToCartSignal(data.items)
                        "EXECUTE_ORDER" -> executeOrderSignal() // <--- NUEVO CASO
                        else -> {
                            currentPhase = data.phase
                            visualItems = data.items
                        }
                    }
                }

            } catch (e: Exception) {
                errorMessage = e.message ?: "Error de conexión"
                Log.e("ChatError", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // 4. Función privada para procesar la señal del backend
    private suspend fun handleAddToCartSignal(items: List<VisualItem>) {
        val signalItem = items.firstOrNull() ?: return

        try {
            // El backend envió los metadatos en el campo 'subtitle' como JSON string
            val json = JSONObject(signalItem.subtitle)

            val dish = Dish(
                id = signalItem.id,
                name = signalItem.title,
                price = json.getDouble("price"),
                description = json.getString("description"),
                category = json.optString("category", "General"),
                imageURL = signalItem.imageSeed, // En tu backend mapeaste imageURL a imageSeed
                enable = true
            )

            val quantity = json.optInt("quantity", 1)
            val restaurantId = json.getString("restaurantId")

            // Usamos tu CartManager existente
            val added = cartManager.addToCart(dish, quantity, restaurantId)

            if (added) {
                snackbarMessage = "✅ Agregado: $quantity ${dish.name}"
                // Opcional: Cambiar fase a "summary" visualmente si quieres
                currentPhase = "summary"
            } else {
                snackbarMessage = "⚠️ Conflicto: Tienes pedidos de otro restaurante"
            }

        } catch (e: Exception) {
            Log.e("WaiterVM", "Error al procesar ADD_TO_CART: ${e.message}")
            snackbarMessage = "Error al procesar el pedido"
        }
    }

    private suspend fun executeOrderSignal() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            snackbarMessage = "Error: Usuario no autenticado"
            return
        }

        // 1. Obtener items actuales del carrito (snapshot)
        val currentItems = cartManager.cartItems.first()
        if (currentItems.isEmpty()) {
            snackbarMessage = "El carrito está vacío"
            return
        }

        // 2. Verificar si ya existe una orden activa
        val activeOrderResult = pedidoRepository.getActiveOrder(currentUser.uid)

        activeOrderResult.fold(
            onSuccess = { existingOrder ->
                if (existingOrder != null) {
                    // Si ya hay orden, agregamos los pedidos a esa orden
                    addPedidosToExistingOrder(currentUser.uid, existingOrder, currentItems)
                } else {
                    // Si no hay orden, creamos una nueva
                    createNewOrder(currentUser.uid, currentItems)
                }
            },
            onFailure = {
                snackbarMessage = "Error al verificar ordenes: ${it.message}"
            }
        )
    }

    private suspend fun createNewOrder(userId: String, cartItems: List<com.example.servyapp.domain.model.CartItem>) {
        // Agrupar por restaurante
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }

        // Obtener nombre del primer restaurante para el número de orden
        val firstRestId = cartItems.first().restaurantId
        val restaurantName = try {
            restaurantRepository.getRestaurant(firstRestId)?.name ?: "Restaurante"
        } catch (e: Exception) { "Restaurante" }

        // Crear objetos Pedido
        val pedidos = itemsByRestaurant.map { (restId, items) ->
            createPedidoObject(restId, items, restaurantName) // Ver función auxiliar abajo
        }

        val totalAmount = pedidos.sumOf { it.subtotal }
        val orderNumber = "ORD-${System.currentTimeMillis()}" // Generador simple

        val order = Order(
            userId = userId,
            createdAt = Timestamp.now(),
            orderNumber = orderNumber,
            pedidos = pedidos,
            totalAmount = totalAmount,
            status = OrderStatus.PENDING
        )

        // Guardar en Firebase
        pedidoRepository.createOrder(order, userId).fold(
            onSuccess = {
                cartManager.clearCart() // ¡Limpiamos el carrito local!
                snackbarMessage = "✅ ¡Pedido enviado a cocina!"
                currentPhase = "summary" // Mantenemos la vista limpia
                visualItems = emptyList() // Limpiamos visuales anteriores
            },
            onFailure = {
                snackbarMessage = "Error al crear orden: ${it.message}"
            }
        )
    }

    private suspend fun addPedidosToExistingOrder(userId: String, order: Order, cartItems: List<com.example.servyapp.domain.model.CartItem>) {
        // Lógica simplificada para añadir a orden existente
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }

        itemsByRestaurant.forEach { (restId, items) ->
            val restaurantName = try {
                restaurantRepository.getRestaurant(restId)?.name ?: "Restaurante"
            } catch (e: Exception) { "Restaurante" }

            val newPedido = createPedidoObject(restId, items, restaurantName)
            pedidoRepository.addPedidoToOrder(order.id, newPedido, userId)
        }

        cartManager.clearCart()
        snackbarMessage = "✅ Agregado a tu orden en curso"
    }

    // Función auxiliar para convertir CartItems a Pedido
    private fun createPedidoObject(
        restaurantId: String,
        items: List<com.example.servyapp.domain.model.CartItem>,
        restaurantName: String
    ): Pedido {
        val pedidoItems = items.map { cartItem ->
            PedidoItem(
                dishId = cartItem.dish.id,
                dishName = cartItem.dish.name,
                dishImageURL = cartItem.dish.imageURL,
                dishDescription = cartItem.dish.description,
                quantity = cartItem.quantity,
                pricePerUnit = cartItem.dish.price,
                totalPrice = cartItem.totalPrice
            )
        }

        return Pedido(
            id = "", // ID se genera en Firebase o Repositorio
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            items = pedidoItems,
            subtotal = pedidoItems.sumOf { it.totalPrice },
            createdAt = Timestamp.now(),
            status = PedidoStatus.PENDING
        )
    }

    fun onVisualItemClicked(item: VisualItem) {
        val intentMessage = "Ver ${item.title}"
        sendToBackend(intentMessage)
    }
}