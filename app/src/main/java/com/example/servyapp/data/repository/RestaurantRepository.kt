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
}