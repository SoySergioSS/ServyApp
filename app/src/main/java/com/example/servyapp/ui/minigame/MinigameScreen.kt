package com.example.servyapp.ui.minigame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    viewModel.updateBallPosition(event.values[0], event.values[1])
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar con botón de volver
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80000000))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Nivel: ${state.currentLevel}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Tiempo: ${state.timeElapsed / 1000}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Volver", fontSize = 14.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        viewModel.initGame(size.width.toFloat(), size.height.toFloat())
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Meta
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(state.goal.left, state.goal.top),
                        size = Size(state.goal.width, state.goal.height)
                    )

                    // Paredes
                    state.walls.forEach { rect ->
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height)
                        )
                    }

                    // Trampas
                    state.traps.forEach { rect ->
                        drawRect(
                            color = Color(0x80FF0000),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width, rect.height)
                        )
                    }

                    // Bola
                    drawCircle(
                        color = Color(0xFFF44336),
                        radius = state.ballRadius,
                        center = Offset(state.ballPosition.first, state.ballPosition.second)
                    )
                }
            }
        }
    }

    // Diálogo de Victoria
    if (state.hasWon) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "¡Nivel Completado!") },
            text = { Text(text = "Tiempo: ${state.timeElapsed / 1000} segundos") },
            confirmButton = {
                Button(onClick = { viewModel.nextLevel() }) {
                    Text("Siguiente Nivel")
                }
            }
        )
    }

    // Diálogo de Fin del Juego
    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "¡Juego Terminado!") },
            text = { Text(text = "Has completado todos los niveles. ¡Tu orden está lista!") },
            confirmButton = {
                Button(onClick = onNavigateBack) {
                    Text("Volver al Pedido")
                }
            }
        )
    }
}