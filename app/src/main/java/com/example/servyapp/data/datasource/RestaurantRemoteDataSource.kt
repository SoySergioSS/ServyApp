package com.example.servyapp.data.datasource

import com.example.servyapp.domain.model.Restaurant
import com.example.servyapp.domain.model.Table
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("restaurants")

    suspend fun createRestaurant(restaurant: Restaurant) : String { //tal vez como admin se podrá hacer más adelante
        val docRef = collection.add(restaurant).await()
        return docRef.id
    }

    suspend fun getRestaurant(id: String): Restaurant? {
        val snapshot = collection.document(id).get().await()
        return snapshot.toObject(Restaurant::class.java)?.copy(id = snapshot.id)
    }

    suspend fun getAllRestaurants(): List<Restaurant> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Restaurant::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun updateRestaurant(restaurant: Restaurant) { //tal vez como admin se podrá hacer más adelante
        collection.document(restaurant.id).update(
            mapOf(
                "name" to restaurant.name,
                "address" to restaurant.address,
                "phone" to restaurant.phone,
                "rating" to restaurant.rating,
                "imageURL" to restaurant.imageURL
            )
        ).await()
    }

    suspend fun deleteRestaurant(id: String) { //tal vez como admin se podrá hacer más adelante
        collection.document(id).delete().await()
    }


    //ASIGNACION DE MESA

    suspend fun assignTableSecure(
        restaurantId: String,
        requiredSeats: Int,
        orderId: String
    ): Table? {

        val tablesRef = firestore
            .collection("restaurants")
            .document(restaurantId)
            .collection("tables")

        return try {
            // 1️⃣ Obtener mesas libres fuera de la transacción
            val freeTables = tablesRef
                .whereEqualTo("isOccupied", false)
                .get()
                .await()

            // Filtrar mesas que cumplen los asientos necesarios
            val suitableTables = freeTables.documents
                .filter { (it.getLong("seats") ?: 0L) >= requiredSeats }
                .sortedBy { it.getLong("seats") ?: Long.MAX_VALUE }

            if (suitableTables.isEmpty()) return null

            val selected = suitableTables.first()
            val selectedRef = selected.reference

            // 2️⃣ Transacción para evitar colisiones
            val assignedTable = firestore.runTransaction { transaction ->

                val fresh = transaction.get(selectedRef)

                val isStillFree = fresh.getBoolean("isOccupied") == false

                if (!isStillFree) {
                    return@runTransaction null // ⚠️ RETORNA NULL SI OTRO LA OCUPÓ
                }

                // Ocupa la mesa
                transaction.update(
                    selectedRef,
                    mapOf(
                        "isOccupied" to true,
                        "currentOrderId" to orderId
                    )
                )

                // Retornar la mesa asignada
                Table(
                    id = fresh.id,
                    number = fresh.getLong("number")!!.toInt(),
                    seats = fresh.getLong("seats")!!.toInt(),
                    isOccupied = true,
                    currentOrderId = orderId
                )
            }.await()

            assignedTable

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateOrderWithTable(
        orderId: String,
        restaurantId: String,
        tableId: String,
        tableNumber: Int
    ): Boolean {

        val orderRef = firestore.collection("orders").document(orderId)
        val tableRef = firestore.collection("restaurants")
            .document(restaurantId)
            .collection("tables")
            .document(tableId)

        return try {
            firestore.runTransaction { transaction ->

                val tableSnap = transaction.get(tableRef)
                val currentOrder = tableSnap.getString("currentOrderId")

                // Si por alguna razón currentOrderId no coincide, lo forzamos
                if (currentOrder != orderId) {
                    transaction.update(tableRef, mapOf(
                        "currentOrderId" to orderId,
                        "isOccupied" to true
                    ))
                }


                transaction.set(orderRef, mapOf(
                    "tableId" to tableId,
                    "tableNumber" to tableNumber
                ), SetOptions.merge())

            }.await()

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    suspend fun releaseTable(
        orderId: String
    ): Boolean {
        return try {
            // buscar dentro de todos los restaurantes
            val restaurants = firestore.collection("restaurants").get().await()

            var released = false

            for (restaurant in restaurants.documents) {

                val tablesRef = restaurant.reference.collection("tables")
                val tablesSnap = tablesRef
                    .whereEqualTo("currentOrderId", orderId)
                    .get()
                    .await()

                if (!tablesSnap.isEmpty) {
                    val tableDoc = tablesSnap.documents.first()
                    tableDoc.reference.update(
                        mapOf(
                            "isOccupied" to false,
                            "currentOrderId" to null
                        )
                    ).await()

                    released = true
                    break
                }
            }

            released
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


}