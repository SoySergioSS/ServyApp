package com.example.servyapp.ui.minigame

import androidx.compose.ui.geometry.Rect

data class MazeGameState(
    // Posición X, Y de la bola
    val ballPosition: Pair<Float, Float> = Pair(100f, 100f),
    val ballRadius: Float = 30f,
    // Lista de rectángulos que actúan como paredes
    val walls: List<Rect> = emptyList(),
    // Rectángulo de la meta
    val goal: Rect = Rect.Zero,
    val hasWon: Boolean = false,
    // Nuevo estado
    val currentLevel: Int = 1,
    val timeElapsed: Long = 0L, // En milisegundos
    val isGameOver: Boolean = false
)