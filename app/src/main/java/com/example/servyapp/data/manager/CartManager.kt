package com.example.servyapp.data.manager

import com.example.servyapp.data.local.CartDao // NUEVO
import com.example.servyapp.data.local.toDomain // NUEVO
import com.example.servyapp.data.local.toEntity // NUEVO
import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Dish
import kotlinx.coroutines.flow.Flow // Cambiado de StateFlow
import kotlinx.coroutines.flow.map // NUEVO
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.plus

@Singleton
class CartManager @Inject constructor(
    // INYECTAMOS EL DAO en lugar de solo construirlo en memoria
    private val cartDao: CartDao
) {

    // 1. Reemplazamos MutableStateFlow con el Flow de Room mapeado a dominio
    val cartItems: Flow<List<CartItem>> = cartDao.getAllCartItems()
        .map { entities ->
            entities.map { it.toDomain() }
        }

    // 2. Modificamos addToCart para que sea suspend y use el DAO
    suspend fun addToCart(dish: Dish, quantity: Int, restaurantId: String): Boolean {
        // 1. Obtener el ítem existente por Dish ID
        val existingItem = cartDao.getCartItemByDishId(dish.id)

        // 2. Comprobar conflicto de restaurante solo si es un ítem nuevo
        if (existingItem == null) {
            // Recolectamos la lista actual para la comprobación de restaurante
            val currentItems = cartItems.first()
            if (currentItems.isNotEmpty()) {
                val currentRestaurantId = currentItems.first().restaurantId
                if (restaurantId != currentRestaurantId) {
                    return false // Conflicto: diferentes restaurantes
                }
            }
        }

        // 3. Operación de DB
        if (existingItem != null) {
            // El ítem ya existe, incrementa la cantidad y actualiza los detalles del platillo
            cartDao.updateExistingItem(
                cartItemId = existingItem.id,
                quantityIncrement = quantity,
                newName = dish.name,
                newDescription = dish.description,
                newPrice = dish.price,
                newImageURL = dish.imageURL,
                newCategory = dish.category
            )
        } else {
            // Nuevo ítem
            val newCartItem = CartItem(
                id = generateCartItemId(),
                dish = dish,
                quantity = quantity,
                restaurantId = restaurantId
            )
            cartDao.insert(newCartItem.toEntity())
        }

        return true
    }

    // 3. Modificamos incrementQuantity para que sea suspend y use el DAO
    suspend fun incrementQuantity(cartItemId: String) {
        // Obtener el ítem actual para actualizar
        val currentItem = cartItems.first().firstOrNull { it.id == cartItemId }
        if (currentItem != null) {
            cartDao.updateQuantity(cartItemId, currentItem.quantity + 1)
        }
    }

    // 4. Modificamos decrementQuantity para que sea suspend y use el DAO
    suspend fun decrementQuantity(cartItemId: String) {
        val currentItem = cartItems.first().firstOrNull { it.id == cartItemId }

        if (currentItem != null) {
            val newQuantity = currentItem.quantity - 1
            if (newQuantity > 0) {
                cartDao.updateQuantity(cartItemId, newQuantity)
            } else {
                cartDao.deleteById(cartItemId)
            }
        }
    }

    // 5. Modificamos removeFromCart para que sea suspend y use el DAO
    suspend fun removeFromCart(cartItemId: String) {
        cartDao.deleteById(cartItemId)
    }

    // 6. Modificamos clearCart para que sea suspend y use el DAO
    suspend fun clearCart() {
        cartDao.clearCart()
    }

    private fun generateCartItemId(): String {
        return "cart_${System.currentTimeMillis()}_${(0..9999).random()}"
    }

    // getCartItemCount y getCartTotal ya no son necesarios aquí,
    // ya que CartState calcula estos valores a partir del Flow.
}