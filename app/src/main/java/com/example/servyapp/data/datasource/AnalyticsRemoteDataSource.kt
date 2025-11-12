package com.example.servyapp.data.datasource

import android.util.Log
import com.example.servyapp.domain.model.Order
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction // <-- ¡Importante!
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
// (Quita SetOptions si ya no lo usas en otro lado)

@Singleton
class AnalyticsRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun analyticsCollection() = firestore.collection("user_analytics")

    /**
     * Escribe o actualiza las analíticas de un usuario después de una orden
     * USANDO UNA TRANSACCIÓN para evitar sobrescrituras.
     */
    suspend fun updateUserAnalytics(userId: String, order: Order): Result<Unit> {
        return try {
            val analyticsDocRef = analyticsCollection().document(userId)

            // 1. Ejecutamos todo dentro de una transacción
            firestore.runTransaction { transaction ->
                // 2. Leemos el documento de analíticas DENTRO de la transacción
                val snapshot = transaction.get(analyticsDocRef)

                if (snapshot.exists()) {
                    // --- CASO 1: El documento SÍ existe ---
                    // Preparamos los incrementos para .update()
                    val updates = mutableMapOf<String, Any>(
                        "totalSpent" to FieldValue.increment(order.totalAmount),
                        "totalOrders" to FieldValue.increment(1)
                    )
                    order.pedidos.forEach { pedido ->
                        pedido.items.forEach { item ->
                            if (item.dishId.isNotBlank()) {
                                val fieldPath = "dishCount.${item.dishId}"
                                updates[fieldPath] = FieldValue.increment(item.quantity.toLong())
                            }
                        }
                    }

                    // Actualizamos el documento existente
                    transaction.update(analyticsDocRef, updates)

                } else {
                    // --- CASO 2: El documento NO existe ---
                    // No podemos usar increment(), así que creamos el documento
                    // con los valores de esta primera orden.

                    val initialDishCount = mutableMapOf<String, Long>()
                    order.pedidos.forEach { pedido ->
                        pedido.items.forEach { item ->
                            if (item.dishId.isNotBlank()) {
                                val currentCount = initialDishCount.getOrDefault(item.dishId, 0L)
                                initialDishCount[item.dishId] = currentCount + item.quantity
                            }
                        }
                    }

                    val initialData = mapOf(
                        "totalSpent" to order.totalAmount, // El primer gasto
                        "totalOrders" to 1L,             // La primera orden
                        "dishCount" to initialDishCount  // El primer mapa de platillos
                    )

                    // Creamos el documento por primera vez
                    transaction.set(analyticsDocRef, initialData)
                }

                // La transacción termina exitosamente
            }.await() // Esperamos a que la transacción se complete

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("StatsErrorDS", "Fallo en la transacción de analíticas: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Lee las analíticas de un usuario.
     */
    suspend fun getUserAnalytics(userId: String): Result<Map<String, Any>> {
        // ... (Esta función no cambia y ya es correcta)
        return try {
            val doc = analyticsCollection().document(userId).get().await()
            if (doc.exists() && doc.data != null) {
                Result.success(doc.data!!)
            } else {
                Result.failure(Exception("No se encontraron estadísticas para este usuario."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}