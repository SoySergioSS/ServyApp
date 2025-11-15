package com.example.servyapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.manager.CartManager
import com.example.servyapp.data.repository.PedidoRepository
import com.example.servyapp.data.repository.RestaurantRepository
import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoItem
import com.example.servyapp.domain.model.PedidoStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartManager: CartManager,
    private val pedidoRepository: PedidoRepository,
    private val auth: FirebaseAuth,
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartState())
    val uiState: StateFlow<CartState> = _uiState

    init {
        loadCartItems()
    }

    private fun loadCartItems() {
        viewModelScope.launch {
            // Recogemos el Flow de Room
            cartManager.cartItems.collectLatest { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun incrementQuantity(cartItemId: String) {
        viewModelScope.launch {
            cartManager.incrementQuantity(cartItemId)
        }
    }

    fun decrementQuantity(cartItemId: String) {
        viewModelScope.launch {
            cartManager.decrementQuantity(cartItemId)
        }
    }

    fun showDeleteDialog(item: CartItem) {
        _uiState.update { it.copy(showDeleteDialog = item) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = null) }
    }

    fun confirmDeleteItem() {
        val item = _uiState.value.showDeleteDialog
        if (item != null) {
            viewModelScope.launch {
                cartManager.removeFromCart(item.id)
                dismissDeleteDialog()
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartManager.clearCart()
        }
    }

    fun onItemClick(item: CartItem) {
        _uiState.update {
            it.copy(
                navigationEvent = NavigationEvent.NavigateToDishDetail(
                    restaurantId = item.restaurantId,
                    dishId = item.dish.id
                )
            )
        }
    }

    fun onPedidoClick() {
        if (_uiState.value.items.isNotEmpty()) {
            viewModelScope.launch {
                createOrAddToOrder()
            }
        }
    }

    private suspend fun createOrAddToOrder() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.update {
                    it.copy(errorMessage = "Usuario no autenticado", isLoading = false)
                }
                return
            }

            val cartItems = _uiState.value.items
            if (cartItems.isEmpty()) return

            val currentCartRestaurants = cartItems.map { it.restaurantId }.distinct()
            val activeOrderResult = pedidoRepository.getActiveOrder(currentUser.uid)

            activeOrderResult.fold(
                onSuccess = { existingOrder ->
                    if (existingOrder != null) {
                        val orderRestaurants = existingOrder.pedidos.map { it.restaurantId }.distinct()

                        val hasConflict = currentCartRestaurants.any { cartRestaurant ->
                            !orderRestaurants.contains(cartRestaurant)
                        }

                        if (hasConflict) {
                            _uiState.update {
                                it.copy(
                                    errorMessage = "Ya tienes una orden activa en otro restaurante. Por favor, completa o cancela tu orden actual antes de pedir en otro lugar.",
                                    isLoading = false,
                                    showConflictDialog = true,
                                    conflictingRestaurants = currentCartRestaurants.filterNot { orderRestaurants.contains(it) }
                                )
                            }
                        } else {
                            addPedidosToExistingOrder(currentUser.uid, existingOrder, cartItems)
                        }
                    } else {
                        createNewOrder(currentUser.uid, cartItems)
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(errorMessage = "Error al verificar orden: ${exception.message}", isLoading = false)
                    }
                }
            )
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = "Error inesperado: ${e.message}", isLoading = false)
            }
        }
    }

    private suspend fun createNewOrder(userId: String, cartItems: List<CartItem>) {
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }

        val pedidos = itemsByRestaurant.map { (restaurantId, items) ->
            val restaurantName = try {
                restaurantRepository.getRestaurant(restaurantId)?.name ?: "Restaurante"
            } catch (e: Exception) {
                "Restaurante"
            }
            createPedidoFromCartItems(restaurantId, items, restaurantName)
        }

        val totalAmount = pedidos.sumOf { it.subtotal }

        // CORRECCIÓN: Accedemos a 'restaurantName' de forma segura.
        // Si el compilador seguía fallando aquí, era por un problema de caché.
        val orderRestaurantName = pedidos.firstOrNull()?.restaurantName ?: "General"
        val orderNumber = generateOrderNumber(orderRestaurantName)

        val order = Order(
            userId = userId,
            createdAt = Timestamp.now(),
            orderNumber = orderNumber,
            pedidos = pedidos,
            totalAmount = totalAmount,
            status = OrderStatus.PENDING
        )

        pedidoRepository.createOrder(order, userId).fold(
            onSuccess = {
                cartManager.clearCart()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        navigationEvent = NavigationEvent.NavigateToOrders,
                        errorMessage = null
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(errorMessage = "Error al crear la orden: ${exception.message}", isLoading = false)
                }
            }
        )
    }

    private suspend fun addPedidosToExistingOrder(
        userId: String,
        order: Order,
        cartItems: List<CartItem>
    ) {
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }
        var allPedidosAdded = true

        for ((restaurantId, items) in itemsByRestaurant) {
            val restaurantName = try {
                restaurantRepository.getRestaurant(restaurantId)?.name ?: "Restaurante"
            } catch (e: Exception) {
                "Restaurante"
            }

            val newPedido = createPedidoFromCartItems(restaurantId, items, restaurantName)

            pedidoRepository.addPedidoToOrder(order.id, newPedido, userId).fold(
                onSuccess = {
                    // Pedido agregado exitosamente
                },
                onFailure = { exception ->
                    allPedidosAdded = false
                    _uiState.update {
                        it.copy(errorMessage = "Error al agregar pedido: ${exception.message}", isLoading = false)
                    }
                    return
                }
            )
        }

        if (allPedidosAdded) {
            cartManager.clearCart()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = NavigationEvent.NavigateToOrders,
                    errorMessage = null
                )
            }
        }
    }

    private fun createPedidoFromCartItems(
        restaurantId: String,
        items: List<CartItem>,
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

        val subtotal = pedidoItems.sumOf { it.totalPrice }

        // CORRECCIÓN FINAL: Incluimos explícitamente el 'id' (vacío) para evitar
        // que el compilador posicione mal los siguientes argumentos y reporte error.
        return Pedido(
            id = "",
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            items = pedidoItems,
            subtotal = subtotal,
            createdAt = Timestamp.now(),
            status = PedidoStatus.PENDING
        )
    }

    private fun generateOrderNumber(restaurantName: String): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        val cleanName = restaurantName.replace(" ", "_")
        return "ORD-${cleanName}-${random}-${timestamp}"
    }

    fun dismissConflictDialog() {
        _uiState.update { it.copy(showConflictDialog = false, errorMessage = null) }
    }

    fun onNavigationEventHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}