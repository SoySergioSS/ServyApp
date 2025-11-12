package com.example.servyapp.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    StatsContent(state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(state: StatsState) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Estadísticas") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.errorMessage != null -> {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Stats 1 y 2: Gasto Total y Órdenes
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatCard(
                                "Gasto Total",
                                "S/ ${String.format("%.2f", state.totalSpent)}",
                                Modifier.weight(1f)
                            )
                            StatCard(
                                "Órdenes",
                                state.totalOrders.toString(),
                                Modifier.weight(1f)
                            )
                        }

                        // Stat 3: Top 5 Platillos
                        Text(
                            "Mis 5 Platillos Favoritos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.topDishes.isNotEmpty()) {
                            // 1. Prepara los datos para el gráfico
                            val chartData = state.topDishes.mapIndexed { index, (dishId, count) ->
                                entryOf(index.toFloat(), count.toFloat())
                            }
                            val entryModelProducer = ChartEntryModelProducer(chartData)

                            // 2. Dibuja el Gráfico de Barras
                            Chart(
                                chart = columnChart(),
                                chartModelProducer = entryModelProducer,
                                startAxis = rememberStartAxis(title = "Cantidad"),
                                bottomAxis = rememberBottomAxis(
                                    title = "Platillo (ID)",
                                    // Formateador para las etiquetas del eje X
                                    valueFormatter = { value, _ ->
                                        // Usamos el 'value' (que es el 'index') para
                                        // obtener el ID del platillo de la lista original
                                        val dishId = state.topDishes.getOrNull(value.toInt())?.first ?: ""
                                        // Acortamos el ID para que quepa en la etiqueta
                                        dishId.take(6) + "..."
                                    }
                                ),
                                modifier = Modifier.height(250.dp)
                            )
                        } else {
                            Text("Aún no tienes platillos favoritos. ¡Sigue pidiendo!")
                        }
                    }
                }
            }
        }
    }
}

// Composable reutilizable para mostrar las estadísticas 1 y 2
@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}