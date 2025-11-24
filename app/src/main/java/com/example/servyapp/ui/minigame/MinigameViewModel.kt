package com.example.servyapp.ui.minigame

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class MazeGameViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MazeGameState())
    val uiState: StateFlow<MazeGameState> = _uiState

    private var screenWidth = 0f
    private var screenHeight = 0f
    private var isInitialized = false
    
    private var timerJob: Job? = null
    private var startTime = 0L

    fun initGame(width: Float, height: Float) {
        if (isInitialized) return
        screenWidth = width
        screenHeight = height
        isInitialized = true
        
        loadLevel(1)
    }

    private fun loadLevel(level: Int) {
        val wallThickness = 25f
        val walls = mutableListOf<Rect>()
        val traps = mutableListOf<Rect>()
        var goal = Rect.Zero
        var startPos = Pair(100f, 100f)

        // Marco exterior
        walls.add(Rect(0f, 0f, screenWidth, wallThickness))
        walls.add(Rect(0f, 0f, wallThickness, screenHeight))
        walls.add(Rect(screenWidth - wallThickness, 0f, screenWidth, screenHeight))
        walls.add(Rect(0f, screenHeight - wallThickness, screenWidth, screenHeight))

        when (level) {
            1 -> {
                walls.add(Rect(screenWidth * 0.2f, 0f, screenWidth * 0.25f, screenHeight * 0.6f))
                walls.add(Rect(screenWidth * 0.5f, screenHeight * 0.3f, screenWidth * 0.55f, screenHeight))
                walls.add(Rect(screenWidth * 0.75f, 0f, screenWidth * 0.8f, screenHeight * 0.7f))
                goal = Rect(screenWidth - 180f, screenHeight - 180f, screenWidth - 50f, screenHeight - 50f)
            }
            2 -> {
                walls.add(Rect(0f, screenHeight * 0.2f, screenWidth * 0.6f, screenHeight * 0.25f))
                walls.add(Rect(screenWidth * 0.4f, screenHeight * 0.5f, screenWidth, screenHeight * 0.55f))
                walls.add(Rect(screenWidth * 0.2f, screenHeight * 0.75f, screenWidth * 0.8f, screenHeight * 0.8f))
                goal = Rect(screenWidth * 0.5f - 65f, screenHeight - 180f, screenWidth * 0.5f + 65f, screenHeight - 50f)
            }
            3 -> {
                walls.add(Rect(screenWidth * 0.3f, 0f, screenWidth * 0.35f, screenHeight * 0.8f))
                walls.add(Rect(screenWidth * 0.65f, screenHeight * 0.2f, screenWidth * 0.7f, screenHeight))
                walls.add(Rect(screenWidth * 0.15f, screenHeight * 0.6f, screenWidth * 0.3f, screenHeight * 0.65f))
                goal = Rect(screenWidth - 180f, 50f, screenWidth - 50f, 180f)
            }
            4 -> {
                walls.add(Rect(screenWidth * 0.4f, 0f, screenWidth * 0.45f, screenHeight * 0.7f))
                walls.add(Rect(screenWidth * 0.7f, screenHeight * 0.3f, screenWidth * 0.75f, screenHeight))
                traps.add(Rect(screenWidth * 0.1f, screenHeight * 0.4f, screenWidth * 0.3f, screenHeight * 0.5f))
                traps.add(Rect(screenWidth * 0.5f, screenHeight * 0.1f, screenWidth * 0.6f, screenHeight * 0.2f))
                goal = Rect(screenWidth - 150f, screenHeight - 150f, screenWidth - 50f, screenHeight - 50f)
            }
            5 -> {
                // Obstáculos más pequeños
                walls.add(Rect(0f, screenHeight * 0.2f, screenWidth * 0.7f, screenHeight * 0.24f))
                walls.add(Rect(screenWidth * 0.3f, screenHeight * 0.5f, screenWidth, screenHeight * 0.54f))
                walls.add(Rect(0f, screenHeight * 0.8f, screenWidth * 0.7f, screenHeight * 0.84f))
                traps.add(Rect(screenWidth * 0.8f, screenHeight * 0.3f, screenWidth * 0.9f, screenHeight * 0.4f))
                traps.add(Rect(screenWidth * 0.1f, screenHeight * 0.6f, screenWidth * 0.2f, screenHeight * 0.7f))
                goal = Rect(screenWidth * 0.5f - 50f, screenHeight - 150f, screenWidth * 0.5f + 50f, screenHeight - 50f)
            }
            else -> {
                _uiState.update { it.copy(isGameOver = true) }
                return
            }
        }

        _uiState.update {
            it.copy(
                ballPosition = startPos,
                walls = walls,
                goal = goal,
                hasWon = false,
                currentLevel = level,
                timeElapsed = 0L,
                traps = traps
            )
        }
        
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.update { it.copy(timeElapsed = elapsed) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
    }

    fun nextLevel() {
        val next = _uiState.value.currentLevel + 1
        loadLevel(next)
    }

    fun updateBallPosition(tiltX: Float, tiltY: Float) {
        _uiState.update { state ->
            if (state.hasWon) return@update state

            val (currentX, currentY) = state.ballPosition
            val speed = 8f

            val nextX = currentX - (tiltX * speed)
            val nextY = currentY + (tiltY * speed)

            val nextBallRectX = Rect(nextX - state.ballRadius, currentY - state.ballRadius, nextX + state.ballRadius, currentY + state.ballRadius)
            val nextBallRectY = Rect(currentX - state.ballRadius, nextY - state.ballRadius, currentX + state.ballRadius, nextY + state.ballRadius)

            var finalX = currentX
            var finalY = currentY

            if (state.walls.none { it.overlaps(nextBallRectX) }) {
                finalX = nextX
            }
            if (state.walls.none { it.overlaps(nextBallRectY) }) {
                finalY = nextY
            }

            val currentBallRect = Rect(finalX - state.ballRadius, finalY - state.ballRadius, finalX + state.ballRadius, finalY + state.ballRadius)
            if (state.traps.any { it.overlaps(currentBallRect) }) {
                return@update state.copy(ballPosition = Pair(100f, 100f))
            }

            val won = state.goal.overlaps(currentBallRect)

            if (won) {
                stopTimer()
            }

            state.copy(
                ballPosition = Pair(finalX, finalY),
                hasWon = won
            )
        }
    }
}