package com.example.servyapp.data.repository

import com.example.servyapp.data.datasource.AuthRemoteDataSource
import com.example.servyapp.data.datasource.CardRemoteDataSource
import com.example.servyapp.data.datasource.UserRemoteDataSource
import com.example.servyapp.domain.model.Card
import javax.inject.Inject

class CardRepository @Inject constructor(
    private val cardRemoteDataSource: CardRemoteDataSource
) {
    suspend fun createCard(card: Card) =
        cardRemoteDataSource.createCard(card)

    suspend fun getCard(cardId: String): Card? =
        cardRemoteDataSource.getCard(cardId)

    suspend fun deleteCard(cardId: String) =
        cardRemoteDataSource.deleteCard(cardId)
}