package com.example.servyapp.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.manager.CartManager
import com.example.servyapp.data.repository.PedidoRepository
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartState())
    val uiState: StateFlow<CartState> = _uiState

    init {
        loadCartItems()
    }

    private fun loadCartItems() {
        viewModelScope.launch {
            cartManager.cartItems.collectLatest { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun incrementQuantity(cartItemId: String) {
        cartManager.incrementQuantity(cartItemId)
    }

    fun decrementQuantity(cartItemId: String) {
        cartManager.decrementQuantity(cartItemId)
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
            cartManager.removeFromCart(item.id)
            dismissDeleteDialog()
        }
    }

    fun clearCart() {
        cartManager.clearCart()
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
            createOrAddToOrder()
        }
    }

    private fun createOrAddToOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Usuario no autenticado",
                            isLoading = false
                        )
                    }
                    return@launch
                }

                val cartItems = _uiState.value.items
                if (cartItems.isEmpty()) return@launch

                // Obtener el restaurantId del primer item del carrito
                val restaurantId = cartItems.first().restaurantId

                // Verificar si existe una orden activa
                val activeOrderResult = pedidoRepository.getActiveOrder(restaurantId, currentUser.uid)

                activeOrderResult.fold(
                    onSuccess = { existingOrder ->
                        if (existingOrder != null) {
                            addPedidosToExistingOrder(restaurantId, existingOrder, cartItems)
                        } else {
                            createNewOrder(restaurantId, currentUser.uid, cartItems)
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Error al verificar orden: ${exception.message}",
                                isLoading = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Error inesperado: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun createNewOrder(restaurantId: String, userId: String, cartItems: List<CartItem>) {
        // Agrupar items por restaurante (cada restaurante = 1 Pedido)
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }

        // Crear los Pedidos
        val pedidos = itemsByRestaurant.map { (restaurantId, items) ->
            createPedidoFromCartItems(restaurantId, items)
        }

        val totalAmount = pedidos.sumOf { it.subtotal }
        val orderNumber = generateOrderNumber()

        val order = Order(
            userId = userId,
            createdAt = Timestamp.now(),
            orderNumber = orderNumber,
            pedidos = pedidos,
            totalAmount = totalAmount,
            status = OrderStatus.PENDING
        )

        pedidoRepository.createOrder(restaurantId, order).fold(
            onSuccess = {
                clearCart()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        navigationEvent = NavigationEvent.NavigateToOrders
                    )
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        errorMessage = "Error al crear la orden: ${exception.message}",
                        isLoading = false
                    )
                }
            }
        )
    }

    private suspend fun addPedidosToExistingOrder(restaurantId: String, order: Order, cartItems: List<CartItem>) {
        // Agrupar items por restaurante
        val itemsByRestaurant = cartItems.groupBy { it.restaurantId }

        var allPedidosAdded = true

        // Agregar cada pedido a la orden existente
        for ((restaurantId, items) in itemsByRestaurant) {
            val newPedido = createPedidoFromCartItems(restaurantId, items)

            for ((_, items) in itemsByRestaurant) {
                val newPedido = createPedidoFromCartItems(restaurantId, items)

                pedidoRepository.addPedidoToOrder(restaurantId, order.id, newPedido).fold(
                    onSuccess = {
                        // Pedido agregado exitosamente
                    },
                    onFailure = { exception ->
                        allPedidosAdded = false
                        _uiState.update {
                            it.copy(
                                errorMessage = "Error al agregar pedido: ${exception.message}",
                                isLoading = false
                            )
                        }
                        return
                    }
                )
            }


        }

        if (allPedidosAdded) {
            clearCart()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    navigationEvent = NavigationEvent.NavigateToOrders
                )
            }
        }
    }

    private fun createPedidoFromCartItems(restaurantId: String, items: List<CartItem>): Pedido {
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

        // TODO: Obtener el nombre real del restaurante
        val restaurantName = "Restaurante $restaurantId"

        return Pedido(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            items = pedidoItems,
            subtotal = subtotal,
            createdAt = Timestamp.now(),
            status = PedidoStatus.PENDING
        )
    }

    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "ORD-$timestamp-$random"
    }

    fun onNavigationEventHandled() {
        _uiState.update { it.copy(navigationEvent = null) }
    }
}