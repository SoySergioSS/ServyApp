package com.example.servyapp.data.datasource

import com.example.servyapp.domain.model.Dish
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DishRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getDishesFromRestaurant(restaurantId: String): Flow<Result<List<Dish>>> = callbackFlow {
        val listenerRegistration = firestore
            .collection("restaurants")
            .document(restaurantId)
            .collection("dishes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val dishes = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Dish::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(dishes))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getDishesOnce(restaurantId: String): Result<List<Dish>> {
        return try {
            val snapshot = firestore
                .collection("restaurants")
                .document(restaurantId)
                .collection("dishes")
                .get()
                .await()

            val dishes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Dish::class.java)?.copy(id = doc.id)
            }
            Result.success(dishes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}