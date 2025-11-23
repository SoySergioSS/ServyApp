package com.example.servyapp.ui.minigame

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MazeGameViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MazeGameState())
    val uiState: StateFlow<MazeGameState> = _uiState

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var isInitialized = false

    // Se llama cuando la pantalla carga para definir el tamaño del laberinto
    fun initGame(width: Float, height: Float) {
        if (isInitialized) return
        screenWidth = width
        screenHeight = height
        isInitialized = true

        // CONFIGURACIÓN DEL NIVEL
        val wallThickness = 25f
        val walls = mutableListOf(
            // Marco exterior
            Rect(0f, 0f, width, wallThickness), // Arriba
            Rect(0f, 0f, wallThickness, height), // Izquierda
            Rect(width - wallThickness, 0f, width, height), // Derecha
            Rect(0f, height - wallThickness, width, height), // Abajo

            // Paredes internas (Obstáculos)
            Rect(width * 0.2f, 0f, width * 0.25f, height * 0.6f),
            Rect(width * 0.5f, height * 0.3f, width * 0.55f, height),
            Rect(width * 0.75f, 0f, width * 0.8f, height * 0.7f)
        )

        // Meta en la esquina inferior derecha
        val goal = Rect(width - 180f, height - 180f, width - 50f, height - 50f)

        _uiState.update {
            it.copy(
                ballPosition = Pair(100f, 100f), // Posición inicial (arriba izquierda)
                walls = walls,
                goal = goal,
                hasWon = false
            )
        }
    }

    // Esta función se llama ~60 veces por segundo desde el sensor
    fun updateBallPosition(tiltX: Float, tiltY: Float) {
        _uiState.update { state ->
            if (state.hasWon) return@update state

            val (currentX, currentY) = state.ballPosition
            // Ajusta la velocidad (más alto = más rápido)
            val speed = 8f

            // Calculamos nueva posición
            // Restamos tiltX porque inclinar a la izquierda da positivo en algunos ejes
            val nextX = currentX - (tiltX * speed)
            val nextY = currentY + (tiltY * speed)

            // --- Detección de Colisiones Simple ---
            // Creamos "cajas" imaginarias donde estaría la bola
            val nextBallRectX = Rect(nextX - state.ballRadius, currentY - state.ballRadius, nextX + state.ballRadius, currentY + state.ballRadius)
            val nextBallRectY = Rect(currentX - state.ballRadius, nextY - state.ballRadius, currentX + state.ballRadius, nextY + state.ballRadius)

            var finalX = currentX
            var finalY = currentY

            // Si no choca en X, nos movemos
            if (state.walls.none { it.overlaps(nextBallRectX) }) {
                finalX = nextX
            }
            // Si no choca en Y, nos movemos
            if (state.walls.none { it.overlaps(nextBallRectY) }) {
                finalY = nextY
            }

            // Chequear si tocamos la meta
            val currentBallRect = Rect(finalX - state.ballRadius, finalY - state.ballRadius, finalX + state.ballRadius, finalY + state.ballRadius)
            val won = state.goal.overlaps(currentBallRect)

            state.copy(
                ballPosition = Pair(finalX, finalY),
                hasWon = won
            )
        }
    }
}