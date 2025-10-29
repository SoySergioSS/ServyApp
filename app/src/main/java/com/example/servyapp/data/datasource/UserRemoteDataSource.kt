package com.example.servyapp.data.datasource

import com.example.servyapp.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createUserProfile(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun updateUserProfile(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    suspend fun getUserProfile(uid: String): User? {
        return firestore.collection("users")
            .document(uid)
            .get()
            .await()
            .toObject(User::class.java)
    }
}
