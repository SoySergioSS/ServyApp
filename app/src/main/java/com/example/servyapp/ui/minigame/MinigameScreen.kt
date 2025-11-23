package com.example.servyapp.ui.minigame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MazeGameScreen(
    viewModel: MazeGameViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit // Para volver al pedido al ganar
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- LÓGICA DEL SENSOR ---
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    // values[0] = Eje X, values[1] = Eje Y
                    viewModel.updateBallPosition(event.values[0], event.values[1])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // SENSOR_DELAY_GAME es la velocidad ideal para juegos
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    // -------------------------

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray) // Fondo del tablero
            .onSizeChanged { size ->
                // Avisamos al ViewModel del tamaño de pantalla para construir los muros
                viewModel.initGame(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Dibujar la Meta (Cuadrado Verde)
            drawRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(state.goal.left, state.goal.top),
                size = Size(state.goal.width, state.goal.height)
            )

            // 2. Dibujar Paredes (Muros Blancos)
            state.walls.forEach { rect ->
                drawRect(
                    color = Color.White,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height)
                )
            }

            // 3. Dibujar la Bola (Círculo Rojo)
            drawCircle(
                color = Color(0xFFF44336),
                radius = state.ballRadius,
                center = Offset(state.ballPosition.first, state.ballPosition.second)
            )
        }

        // Diálogo de Victoria
        if (state.hasWon) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = "¡Felicidades!") },
                text = { Text(text = "Has completado el laberinto. ¡Tu orden ya debe estar lista!") },
                confirmButton = {
                    Button(onClick = onNavigateBack) {
                        Text("Volver")
                    }
                }
            )
        }
    }
}