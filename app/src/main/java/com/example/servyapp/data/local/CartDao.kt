package com.example.servyapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    // Obtener todos los ítems como Flow, permitiendo actualizaciones reactivas
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): Flow<List<CartItemEntity>>

    // Insertar un nuevo ítem (usando REPLACE en caso de conflicto por si se intenta insertar el mismo ID)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItemEntity)

    // Obtener un ítem por su Dish ID (para encontrar si ya está en el carrito)
    @Query("SELECT * FROM cart_items WHERE dish_id = :dishId")
    suspend fun getCartItemByDishId(dishId: String): CartItemEntity?

    // Actualizar cantidad para un ítem existente
    @Query("UPDATE cart_items SET quantity = :newQuantity WHERE id = :cartItemId")
    suspend fun updateQuantity(cartItemId: String, newQuantity: Int)

    // Actualizar cantidad y detalles del platillo (para sumar cantidad a un ítem existente)
    @Query("""
        UPDATE cart_items 
        SET 
            quantity = quantity + :quantityIncrement, 
            dish_price = :newPrice, 
            dish_name = :newName, 
            dish_description = :newDescription, 
            dish_imageURL = :newImageURL,
            dish_category = :newCategory 
        WHERE id = :cartItemId
    """)
    suspend fun updateExistingItem(
        cartItemId: String,
        quantityIncrement: Int,
        newName: String,
        newDescription: String,
        newPrice: Double,
        newImageURL: String,
        newCategory: String
    )

    // Eliminar un ítem por su ID de carrito
    @Query("DELETE FROM cart_items WHERE id = :cartItemId")
    suspend fun deleteById(cartItemId: String)

    // Vaciar todo el carrito
    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}