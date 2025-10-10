package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.CardRemoteDataSource
import com.example.servyapp.data.datasource.DishRemoteDataSource
import com.example.servyapp.domain.model.Card
import com.example.servyapp.domain.model.Dish
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DishRepository @Inject constructor(
    private val dishRemoteDataSource: DishRemoteDataSource
) {
    fun getDishesFromRestaurant(restaurantId: String): Flow<Result<List<Dish>>> =
        dishRemoteDataSource.getDishesFromRestaurant(restaurantId)


}