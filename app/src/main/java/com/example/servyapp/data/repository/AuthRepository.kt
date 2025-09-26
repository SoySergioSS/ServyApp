package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.AuthRemoteDataSource
import com.example.servyapp.data.datasource.UserRemoteDataSource
import com.example.servyapp.domain.model.User
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class AuthRepository @Inject constructor( //para iniciar sesion, crear cuenta, cambiar contraseña y desloguearse
    private val authRemoteDataSource: AuthRemoteDataSource
) {

    val currentUser: FirebaseUser? get() = authRemoteDataSource.currentUser

    suspend fun logIn(email: String, password: String) {
        authRemoteDataSource.logIn(email, password)
    }

    suspend fun signUp(email: String, password: String) {
        authRemoteDataSource.signUp(email, password)
    }

    fun logout() {
        authRemoteDataSource.logOut()
    }

    suspend fun sendPasswordReset(email: String) {
        authRemoteDataSource.sendPasswordReset(email)
    }

}