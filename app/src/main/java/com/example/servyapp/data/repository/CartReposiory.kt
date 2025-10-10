package com.example.servyapp.data.repository

import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Dish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    /**
     * Agrega un platillo al carrito o incrementa su cantidad si ya existe
     */
    fun addToCart(dish: Dish, quantity: Int, restaurantId: String) {
        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.dish.id == dish.id }

            if (existingItem != null) {
                // Si ya existe, incrementar cantidad
                currentItems.map { item ->
                    if (item.dish.id == dish.id) {
                        item.copy(quantity = item.quantity + quantity)
                    } else {
                        item
                    }
                }
            } else {
                // Si no existe, agregar nuevo item
                currentItems + CartItem(
                    id = generateCartItemId(),
                    dish = dish,
                    quantity = quantity,
                    restaurantId = restaurantId
                )
            }
        }
    }

    /**
     * Actualiza la cantidad de un item en el carrito
     */
    fun updateQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }

        _cartItems.update { currentItems ->
            currentItems.map { item ->
                if (item.id == cartItemId) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
        }
    }

    /**
     * Incrementa la cantidad de un item
     */
    fun incrementQuantity(cartItemId: String) {
        _cartItems.update { currentItems ->
            currentItems.map { item ->
                if (item.id == cartItemId) {
                    item.copy(quantity = item.quantity + 1)
                } else {
                    item
                }
            }
        }
    }

    /**
     * Decrementa la cantidad de un item
     */
    fun decrementQuantity(cartItemId: String) {
        _cartItems.update { currentItems ->
            currentItems.mapNotNull { item ->
                if (item.id == cartItemId) {
                    val newQuantity = item.quantity - 1
                    if (newQuantity > 0) item.copy(quantity = newQuantity) else null
                } else {
                    item
                }
            }
        }
    }

    /**
     * Elimina un item del carrito
     */
    fun removeFromCart(cartItemId: String) {
        _cartItems.update { currentItems ->
            currentItems.filter { it.id != cartItemId }
        }
    }

    /**
     * Limpia todo el carrito
     */
    fun clearCart() {
        _cartItems.value = emptyList()
    }

    /**
     * Obtiene el número total de items en el carrito
     */
    fun getCartItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }

    /**
     * Obtiene el total del carrito
     */
    fun getCartTotal(): Double {
        return _cartItems.value.sumOf { it.totalPrice }
    }

    private fun generateCartItemId(): String {
        return "cart_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}