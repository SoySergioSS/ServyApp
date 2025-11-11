package com.example.servyapp.ui.orders

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.ui.theme.ServyAppTheme
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Se encarga de la lógica: obtener el ViewModel, manejar
 * los eventos de navegación y recolectar el estado.
 */
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // El LaunchedEffect para navegar se queda aquí,
    // ya que es lógica de navegación.
    LaunchedEffect(state.navigationEvent) {
        when (val event = state.navigationEvent) {
            is NavigationEvent.NavigateToOrderDetail -> {
                onNavigateToOrderDetail(event.orderId)
                viewModel.onNavigationEventHandled()
            }
            null -> { /* No hacer nada */ }
        }
    }

    // Llamamos al Composable de UI (Contenido)
    OrdersContent(
        state = state,
        onBackClick = onBackClick,
        onRefreshClick = { viewModel.refreshOrders() },
        onOrderClick = { viewModel.onOrderClick(it) },
        onRetryClick = { viewModel.refreshOrders() }
    )
}

/**
 * 2. CONTENIDO (Stateless)
 *
 * Se encarga SOLO de la UI. Recibe el estado y las lambdas
 * para notificar eventos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersContent(
    state: OrdersState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOrderClick: (Order) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis Órdenes")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // El `when` ahora vive en el Composable de Contenido
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.errorMessage ?: "Error desconocido",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Button(onClick = onRetryClick) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            state.orders.isEmpty() -> {
                EmptyOrdersContent(
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                OrdersList(
                    orders = state.orders,
                    onOrderClick = onOrderClick, // Se pasa la lambda
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

// --- El resto de los Composables (EmptyOrdersContent, OrdersList, etc.) ---
// --- no necesitan cambios, ya que eran stateless.                 ---

@Composable
fun EmptyOrdersContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Text(
                text = "No tienes órdenes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Cuando realices un pedido, aparecerá aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OrdersList(
    orders: List<Order>,
    onOrderClick: (Order) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = orders,
            key = { it.id }
        ) { order ->
            OrderCard(
                order = order,
                onClick = { onOrderClick(order) }
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(order.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Número de orden y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orden #${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(order.createdAt.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OrderStatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Spacer(modifier = Modifier.height(16.dp))

            // Pedidos de la orden
            Text(
                text = "Pedidos (${order.pedidos.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista resumida de pedidos
            order.pedidos.take(3).forEach { pedido ->
                PedidoSummaryItem(
                    itemCount = pedido.items.size,
                    totalItems = pedido.items.sumOf { it.quantity },
                    subtotal = pedido.subtotal
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (order.pedidos.size > 3) {
                Text(
                    text = "+ ${order.pedidos.size - 3} pedido(s) más",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Total y botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", order.totalAmount)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        Log.d("OrderDebug", "ID de orden clickeada: ${order.id}")
                        onClick(order.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "Ver Detalles",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PedidoSummaryItem(
    itemCount: Int,
    totalItems: Int,
    subtotal: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {Text(
                text = "$itemCount platillo(s) • $totalItems item(s)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Subtotal
        Text(
            text = "$${String.format("%.2f", subtotal)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OrderStatusBadge(status: OrderStatus) {
    val (text, backgroundColor, textColor) = when (status) {
        OrderStatus.PENDING -> Triple(
            "Pendiente",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        OrderStatus.IN_PROGRESS -> Triple(
            "En Progreso",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        OrderStatus.COMPLETED -> Triple(
            "Completada",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        OrderStatus.CANCELLED -> Triple(
            "Cancelada",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}


/**
 * 3. PREVIEW FUNCIONAL
 *
 * Creamos un preview para `OrdersContent` pasándole un estado de prueba.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OrdersContentPreview_WithOrders() {
    val previewOrder = Order(
        id = "1",
        orderNumber = "12345",
        createdAt = Timestamp.now(),
        status = OrderStatus.PENDING,
        totalAmount = 150.50,
        pedidos = listOf(
            Pedido(
                id = "p1",
                items = emptyList(),
                subtotal = 150.50
            )
        )
    )

    ServyAppTheme {
        OrdersContent(
            state = OrdersState(
                orders = listOf(previewOrder, previewOrder.copy(id = "2", status = OrderStatus.IN_PROGRESS)),
                isLoading = false,
                errorMessage = null
            ),
            onBackClick = {},
            onRefreshClick = {},
            onOrderClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OrdersContentPreview_Empty() {
    ServyAppTheme {
        OrdersContent(
            state = OrdersState(
                orders = emptyList(),
                isLoading = false,
                errorMessage = null
            ),
            onBackClick = {},
            onRefreshClick = {},
            onOrderClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OrdersContentPreview_Error() {
    ServyAppTheme {
        OrdersContent(
            state = OrdersState(
                isLoading = false,
                errorMessage = "No se pudieron cargar las órdenes. Revisa tu conexión."
            ),
            onBackClick = {},
            onRefreshClick = {},
            onOrderClick = {},
            onRetryClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OrdersContentPreview_Loading() {
    ServyAppTheme {
        OrdersContent(
            state = OrdersState(isLoading = true),
            onBackClick = {},
            onRefreshClick = {},
            onOrderClick = {},
            onRetryClick = {}
        )
    }
}