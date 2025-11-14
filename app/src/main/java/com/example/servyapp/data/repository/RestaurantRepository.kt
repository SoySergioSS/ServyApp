package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.RestaurantRemoteDataSource
import com.example.servyapp.domain.model.Restaurant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepository @Inject constructor(
    private val remoteDataSource: RestaurantRemoteDataSource
) {
    suspend fun createRestaurant(restaurant: Restaurant) {
        remoteDataSource.createRestaurant(restaurant)
    }

    suspend fun getRestaurant(id: String): Restaurant? {
        return remoteDataSource.getRestaurant(id)
    }

    suspend fun getAllRestaurants(): List<Restaurant> {
        return remoteDataSource.getAllRestaurants()
    }

    suspend fun updateRestaurant(restaurant: Restaurant) {
        remoteDataSource.updateRestaurant(restaurant)
    }

    suspend fun deleteRestaurant(id: String) {
        remoteDataSource.deleteRestaurant(id)
    }

    suspend fun assignTableSecure(restaurantId: String, requiredSeats: Int)
            = remoteDataSource.assignTableSecure(restaurantId, requiredSeats)

    suspend fun updateOrderWithTable(orderId: String, restaurantId: String, tableId: String, tableNumber: Int) =
        remoteDataSource.updateOrderWithTable(orderId, restaurantId, tableId, tableNumber)

    suspend fun releaseTable(restaurantId: String, tableId: String) =
        remoteDataSource.releaseTable(restaurantId, tableId)
}