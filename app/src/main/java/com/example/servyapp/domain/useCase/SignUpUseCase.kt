package com.example.servyapp.domain.useCase

import com.example.servyapp.data.repository.AuthRepository
import com.example.servyapp.data.repository.CardRepository
import com.example.servyapp.data.repository.UserRepository
import com.example.servyapp.domain.model.Card
import com.example.servyapp.domain.model.User
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cardRepository: CardRepository
) {
    suspend fun execute(
        email: String,
        password: String,
        phone: String,
        cardNumber: String,
        cardHolderName: String,
        expirationDate: String,
        cvv: String
    ): Result<Unit> {
        return try {
            authRepository.signUp(email, password)
            val uid = authRepository.currentUser?.uid ?: throw Exception("No se pudo obtener UID")

            val user = User(uid, email, phone)
            userRepository.createUserProfile(user)

            if (cardNumber.isNotEmpty() && cardHolderName.isNotEmpty() && expirationDate.isNotEmpty() && cvv.isNotEmpty())
            {
                val card = Card(uid, cardNumber, cardHolderName, expirationDate, cvv)
                cardRepository.createCard(card)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}