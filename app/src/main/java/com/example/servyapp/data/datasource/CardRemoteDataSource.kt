package com.example.servyapp.data.datasource

import com.example.servyapp.domain.model.Card
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val cardCollection = firestore.collection("cards")

    suspend fun createCard(card: Card) {
        cardCollection
            .document(card.id)
            .set(card)
            .await()
    }

    suspend fun getCard(cardId: String): Card? {
        val documentSnapshot = cardCollection
            .document(cardId)
            .get()
            .await()

        return documentSnapshot.toObject(Card::class.java)
    }

    suspend fun deleteCard(cardId: String) {
        cardCollection
            .document(cardId)
            .delete()
            .await()
    }
}