package com.example.servyapp.data.local


import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Dish

/**
 * Representa un ítem del carrito almacenado en Room.
 * Usamos @Embedded para incluir todos los detalles del Dish directamente en la tabla.
 */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey
    val id: String, // Usará el ID generado previamente por CartManager

    @Embedded(prefix = "dish_") // Prefijo para evitar colisiones de nombres de columna
    val dish: Dish,

    val quantity: Int,
    val restaurantId: String
)

/**
 * Extension functions para mapear entre la entidad de datos y el modelo de dominio.
 */
fun CartItemEntity.toDomain(): CartItem {
    // El modelo de dominio CartItem tiene una propiedad calculada para totalPrice.
    return CartItem(
        id = id,
        dish = dish,
        quantity = quantity,
        restaurantId = restaurantId
    )
}

fun CartItem.toEntity(): CartItemEntity {
    return CartItemEntity(
        id = id,
        dish = dish,
        quantity = quantity,
        restaurantId = restaurantId
    )
}