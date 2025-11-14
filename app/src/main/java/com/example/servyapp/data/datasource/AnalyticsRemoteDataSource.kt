package com.example.servyapp.data.datasource

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.servyapp.domain.model.Order
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Transaction // <-- ¡Importante!
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

            val restaurantId = order.pedidos.firstOrNull()?.restaurantId ?: "unknown"
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM"))

            val monthlySpentField = "monthlySpent.$currentMonth"
            val restaurantSpentField = "restaurantSpent.$restaurantId"

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(analyticsDocRef)

                if (snapshot.exists()) {
                    // --- CASO 1: El documento SÍ existe (Actualizamos) ---
                    val updates = mutableMapOf<String, Any>(
                        "totalSpent" to FieldValue.increment(order.totalAmount),
                        "totalOrders" to FieldValue.increment(1),
                        monthlySpentField to FieldValue.increment(order.totalAmount),
                        restaurantSpentField to FieldValue.increment(order.totalAmount)
                    )

                    order.pedidos.forEach { pedido ->
                        pedido.items.forEach { item ->
                            if (item.dishId.isNotBlank()) {
                                // Para 'dishCount' (el contador)
                                val countFieldPath = "dishCount.${item.dishId}"
                                updates[countFieldPath] = FieldValue.increment(item.quantity.toLong())

                                // --- ESTA ES LA CORRECCIÓN ---
                                // Añadimos el nombre AL MISMO MAPA de updates
                                // usando notación de puntos.
                                val nameFieldPath = "dishNames.${item.dishId}"
                                updates[nameFieldPath] = item.dishName
                                // --- FIN DE LA CORRECCIÓN ---
                            }
                        }
                    }

                    // Ahora 'updates' contiene AMBOS los incrementos Y los nombres
                    transaction.update(analyticsDocRef, updates)

                    // ELIMINAMOS EL .set() SEPARADO QUE ESTABA CAUSANDO EL ERROR

                } else {
                    // --- CASO 2: El documento NO existe (Lo creamos) ---
                    // (Esta parte ya estaba correcta)
                    val initialDishCount = mutableMapOf<String, Long>()
                    val initialDishNames = mutableMapOf<String, String>()

                    order.pedidos.forEach { pedido ->
                        pedido.items.forEach { item ->
                            if (item.dishId.isNotBlank()) {
                                initialDishCount[item.dishId] = item.quantity.toLong()
                                initialDishNames[item.dishId] = item.dishName
                            }
                        }
                    }

                    val initialData = mapOf(
                        "totalSpent" to order.totalAmount,
                        "totalOrders" to 1L,
                        "dishCount" to initialDishCount,
                        "dishNames" to initialDishNames, // <-- El mapa de nombres
                        "monthlySpent" to mapOf(currentMonth to order.totalAmount),
                        "restaurantSpent" to mapOf(restaurantId to order.totalAmount)
                    )
                    transaction.set(analyticsDocRef, initialData)
                }
            }.await()

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