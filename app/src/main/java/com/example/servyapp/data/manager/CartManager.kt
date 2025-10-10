package com.example.servyapp.data.manager

import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Dish
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.plus

@Singleton
class CartManager @Inject constructor() {

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(dish: Dish, quantity: Int, restaurantId: String) {
        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.dish.id == dish.id }

            if (existingItem != null) {
                currentItems.map { item ->
                    if (item.dish.id == dish.id) {
                        item.copy(quantity = item.quantity + quantity)
                    } else {
                        item
                    }
                }
            } else {
                currentItems + CartItem(
                    id = generateCartItemId(),
                    dish = dish,
                    quantity = quantity,
                    restaurantId = restaurantId
                )
            }
        }
    }

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

    fun removeFromCart(cartItemId: String) {
        _cartItems.update { currentItems ->
            currentItems.filter { it.id != cartItemId }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getCartItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }

    fun getCartTotal(): Double {
        return _cartItems.value.sumOf { it.totalPrice }
    }

    private fun generateCartItemId(): String {
        return "cart_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}