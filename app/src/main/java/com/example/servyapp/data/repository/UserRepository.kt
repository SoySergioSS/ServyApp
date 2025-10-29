package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.AuthRemoteDataSource
import com.example.servyapp.data.manager.SessionManager
import com.example.servyapp.data.datasource.UserRemoteDataSource
import com.example.servyapp.domain.model.User
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend fun createUserProfile(user: User) =
        userRemoteDataSource.createUserProfile(user)

    suspend fun updateUserProfile(user: User) =
        userRemoteDataSource.updateUserProfile(user)

    suspend fun getUserProfile(uid: String): User? =
        userRemoteDataSource.getUserProfile(uid)

    fun saveSelectedRestaurantId(id: String) {
        sessionManager.setSelectedRestaurantId(id)
    }

    fun getSelectedRestaurantId(): StateFlow<String?> {
        return sessionManager.selectedRestaurantId
    }
}