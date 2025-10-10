package com.example.servyapp.data.datasource

import com.example.servyapp.domain.model.Restaurant
import com.google.firebase.firestore.FirebaseFirestore
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
}