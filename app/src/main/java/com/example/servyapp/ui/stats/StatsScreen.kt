package com.example.servyapp.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun StatsScreen(
    viewModel: StatsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    StatsContent(state = state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(state: StatsState) {

    val vibrantColors = remember {
        listOf(
            Color(0xFFF44336),
            Color(0xFF2196F3),
            Color(0xFF4CAF50),
            Color(0xFFFFEB3B),
            Color(0xFFFF9800),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4),
            Color(0xFFE91E63)
        )
    }

    val vicoColumnColors = remember(vibrantColors) {
        vibrantColors.map {
            LineComponent(
                color = it.toArgb(),
                thicknessDp = 20f
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Estadísticas") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.errorMessage ?: "Error desconocido",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    // Stats 1 y 2: Gasto Total y Órdenes
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard("Gasto Total", "S/ ${String.format("%.2f", state.totalSpent)}", Modifier.weight(1f))
                        StatCard("Órdenes", state.totalOrders.toString(), Modifier.weight(1f))
                    }


                    // Gráfico 1: Top 5 Platillos (Barras)
                    Text("Mis 5 Platillos Favoritos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.topDishes.isNotEmpty()) {
                        val chartData = state.topDishes.mapIndexed { index, (name, count) ->
                            entryOf(index.toFloat(), count.toFloat())
                        }
                        val entryModelProducer = ChartEntryModelProducer(chartData)


                        Chart(
                            chart = columnChart(
                                columns = vicoColumnColors
                            ),
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


                    // Gráfico 2: Gasto por Mes (Líneas)
                    Text("Gasto Mensual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.monthlySpent.isNotEmpty()) {
                        val lineData = state.monthlySpent.mapIndexed { index, (mes, gasto) ->
                            entryOf(index.toFloat(), gasto.toFloat())
                        }
                        val lineModelProducer = ChartEntryModelProducer(lineData)
                        Chart(
                            chart = lineChart(),
                            chartModelProducer = lineModelProducer,
                            startAxis = rememberStartAxis(title = "S/ Gastado"),
                            bottomAxis = rememberBottomAxis(
                                title = "Mes",
                                valueFormatter = { value, _ ->
                                    // Obtenemos el mes "YYYY-MM" de la lista
                                    state.monthlySpent.getOrNull(value.toInt())?.first?.substring(5) ?: ""
                                }
                            ),
                            modifier = Modifier.height(250.dp)
                        )
                    } else {
                        Text("Aún no tienes historial de gastos.")
                    }


                    // Gráfico 3: Gasto por Restaurante (Pastel)
                    Text("Gasto por Restaurante", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.restaurantSpent.isNotEmpty()) {

                        val pieChartData = PieChartData(
                            slices = state.restaurantSpent.mapIndexed { index, (name, gasto) ->
                                PieChartData.Slice(
                                    label = name,
                                    value = gasto.toFloat(),
                                    color = vibrantColors[index % vibrantColors.size]
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