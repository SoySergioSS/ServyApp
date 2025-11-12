package com.example.servyapp.ui.stats

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryOf
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData

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
        topBar = { TopAppBar(title = { Text("Mis Estadísticas") }) }
    ) { padding ->
        // Hacemos la columna scrollable para que quepan todos los gráficos
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when {
                state.isLoading -> { /* ... */ }
                state.errorMessage != null -> { /* ... */ }
                else -> {
                    // Stats 1 y 2: Gasto Total y Órdenes
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard("Gasto Total", "S/ ${String.format("%.2f", state.totalSpent)}", Modifier.weight(1f))
                        StatCard("Órdenes", state.totalOrders.toString(), Modifier.weight(1f))
                    }

                    // Gráfico 1 (Existente): Top 5 Platillos (Barras)
                    Text("Mis 5 Platillos Favoritos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.topDishes.isNotEmpty()) {
                        val chartData = state.topDishes.mapIndexed { index, (name, count) ->
                            entryOf(index.toFloat(), count.toFloat())
                        }
                        val entryModelProducer = ChartEntryModelProducer(chartData)
                        Chart(
                            chart = columnChart(),
                            chartModelProducer = entryModelProducer,
                            startAxis = rememberStartAxis(title = "Cantidad"),
                            bottomAxis = rememberBottomAxis(
                                title = "Platillo",
                                valueFormatter = { value, _ ->
                                    val name = state.topDishes.getOrNull(value.toInt())?.first ?: ""
                                    name.take(15) + "..." // acortar nombre
                                }
                            ),
                            modifier = Modifier.height(250.dp)
                        )
                    } else {
                        Text("Aún no tienes platillos favoritos.")
                    }

                    // --- AÑADE LOS DOS NUEVOS GRÁFICOS ---

                    // Gráfico 2 (Nuevo): Gasto por Mes (Líneas)
                    Text("Gasto Mensual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.monthlySpent.isNotEmpty()) {
                        val lineData = state.monthlySpent.mapIndexed { index, (mes, gasto) ->
                            entryOf(index.toFloat(), gasto.toFloat())
                        }
                        val lineModelProducer = ChartEntryModelProducer(lineData)
                        Chart(
                            chart = lineChart(), // <-- TIPO LÍNEA
                            chartModelProducer = lineModelProducer,
                            startAxis = rememberStartAxis(title = "S/ Gastado"),
                            bottomAxis = rememberBottomAxis(
                                title = "Mes",
                                valueFormatter = { value, _ ->
                                    // "value" es el índice (0, 1, 2...)
                                    // Obtenemos el mes "YYYY-MM" de la lista
                                    state.monthlySpent.getOrNull(value.toInt())?.first?.substring(5) ?: "" // Muestra solo "MM"
                                }
                            ),
                            modifier = Modifier.height(250.dp)
                        )
                    } else {
                        Text("Aún no tienes historial de gastos.")
                    }

                    // Gráfico 3 (Nuevo): Gasto por Restaurante (Pastel)
                    Text("Gasto por Restaurante", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.restaurantSpent.isNotEmpty()) {

                        // 1. Prepara los datos para el PieChart
                        val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.outline)
                        val pieChartData = PieChartData(
                            slices = state.restaurantSpent.mapIndexed { index, (name, gasto) ->
                                PieChartData.Slice(
                                    label = name,
                                    value = gasto.toFloat(),
                                    color = colors[index % colors.size]
                                )
                            },
                            plotType = PlotType.Pie
                        )
                        val pieChartConfig = PieChartConfig(
                            strokeWidth = 1f,
                            labelVisible = true,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            showSliceLabels = true,
                            sliceLabelTextSize = 12.sp,
                            activeSliceAlpha = 0.8f,
                            isAnimationEnable = true
                        )

                        // 2. Dibuja el Gráfico de Pastel
                        PieChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(425.dp),
                            pieChartData = pieChartData,
                            pieChartConfig = pieChartConfig
                        )
                    } else {
                        Text("Aún no tienes gastos por restaurante.")
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