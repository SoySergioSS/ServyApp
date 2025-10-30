package com.example.servyapp.ui.orderdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.PedidoStatus
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoItem
import com.example.servyapp.ui.theme.ServyAppTheme
import com.google.firebase.Timestamp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToCard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    LaunchedEffect(state.navigateToCard) {
        if (state.navigateToCard) {
            onNavigateToCard()
            viewModel.navigationToCardComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Pedido") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.errorMessage ?: "Error desconocido")
            }

            state.order != null -> {
                state.order?.let { order ->
                    OrderDetailContent(
                        order = order,
                        onConfirm = { viewModel.confirmOrder() },
                        onCancel = { viewModel.cancelOrder() },
                        onPay = { viewModel.handleCardPayment(order.id) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}


@Composable
fun OrderDetailContent(
    order: com.example.servyapp.domain.model.Order,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "N° Pedido: ${order.orderNumber}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Fecha: ${dateFormat.format(order.createdAt.toDate())}")
        Text("Estado: ${order.status}", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))

        Divider()
        Spacer(Modifier.height(8.dp))
        Text("Productos:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // 👇 Mostrar todos los pedidos y sus items
        order.pedidos.forEach { pedido ->
            Text(
                text = "Restaurante: ${pedido.restaurantName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn {
                items(pedido.items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.dishName, fontWeight = FontWeight.Bold)
                            Text("Cantidad: ${item.quantity}")
                        }
                        Text("S/ ${item.totalPrice}")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Subtotal: S/ ${pedido.subtotal}", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
        }

        Divider()
        Spacer(Modifier.height(8.dp))
        Text("Total: S/ ${order.totalAmount}", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        when (order.status) {
            OrderStatus.PENDING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                        Text("Confirmar")
                    }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Borrar")
                    }
                }
            }

            OrderStatus.IN_PROGRESS -> {
                Button(
                    onClick = { showPaymentDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pagar")
                }
            }

            else -> {
                Text(
                    "Pedido ${order.status}",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
        if (showPaymentDialog) {
            PaymentDialog(
                onDismiss = { showPaymentDialog = false },
                onCash = {
                    showPaymentDialog = false
                    onPay() // o una función específica si quieres diferenciar
                },
                onYape = {
                    showPaymentDialog = false
                    onPay()
                },
                onCard = {
                    showPaymentDialog = false
                    onPay()
                }
            )
        }
    }

}

@Composable
fun PaymentDialog(
    onDismiss: () -> Unit,
    onCash: () -> Unit,
    onYape: () -> Unit,
    onCard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecciona un método de pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCash, modifier = Modifier.fillMaxWidth()) { Text("Efectivo") }
                Button(onClick = onYape, modifier = Modifier.fillMaxWidth()) { Text("Yape / Plin") }
                Button(onClick = onCard, modifier = Modifier.fillMaxWidth()) { Text("Tarjeta") }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}


// --- Previews para OrderDetailScreen  ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreenContent(
    state: OrderDetailState,
    onBackClick: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Pedido") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
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

            state.order == null && state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
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

            state.order != null -> {
                // Reutiliza tu OrderDetailContent existente (ya maneja lazy column internamente)
                OrderDetailContent(
                    order = state.order,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                    onPay = onPay,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pedido no disponible.", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}


/* ----------------------------
   Datos de prueba para preview
   ---------------------------- */
private val previewOrderForPreview = com.example.servyapp.domain.model.Order(
    id = "order123",
    orderNumber = "0001",
    createdAt = com.google.firebase.Timestamp.now(),
    status = com.example.servyapp.domain.model.OrderStatus.PENDING,
    pedidos = listOf(
        com.example.servyapp.domain.model.Pedido(
            id = "p1",
            restaurantId = "r1",
            restaurantName = "Pizzería Roma",
            items = listOf(
                com.example.servyapp.domain.model.PedidoItem(
                    dishId = "d1",
                    dishName = "Pizza Margarita",
                    dishImageURL = "",
                    dishDescription = "Clásica",
                    quantity = 2,
                    pricePerUnit = 15.0,
                    totalPrice = 30.0
                ),
                com.example.servyapp.domain.model.PedidoItem(
                    dishId = "d2",
                    dishName = "Inca Kola 1.5L",
                    dishImageURL = "",
                    dishDescription = "",
                    quantity = 1,
                    pricePerUnit = 8.0,
                    totalPrice = 8.0
                )
            ),
            subtotal = 38.0,
            createdAt = com.google.firebase.Timestamp.now(),
            status = com.example.servyapp.domain.model.PedidoStatus.PREPARING
        ),
        com.example.servyapp.domain.model.Pedido(
            id = "p2",
            restaurantId = "r2",
            restaurantName = "Hamburguesas King",
            items = listOf(
                com.example.servyapp.domain.model.PedidoItem(
                    dishId = "d3",
                    dishName = "Hamburguesa Clásica",
                    dishImageURL = "",
                    dishDescription = "",
                    quantity = 1,
                    pricePerUnit = 18.0,
                    totalPrice = 18.0
                )
            ),
            subtotal = 18.0,
            createdAt = com.google.firebase.Timestamp.now(),
            status = com.example.servyapp.domain.model.PedidoStatus.PREPARING
        )
    ),
    totalAmount = 56.0
)

/* ----------------------------
   Previews
   ---------------------------- */
@Preview(showBackground = true, showSystemUi = true, name = "OrderDetail - Contenido")
@Composable
fun OrderDetailScreenContentPreview_Content() {
    ServyAppTheme {
        OrderDetailScreenContent(
            state = OrderDetailState(order = previewOrderForPreview, isLoading = false),
            onBackClick = {},
            onConfirm = {},
            onCancel = {},
            onPay = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "OrderDetail - Cargando")
@Composable
fun OrderDetailScreenContentPreview_Loading() {
    ServyAppTheme {
        OrderDetailScreenContent(
            state = OrderDetailState(isLoading = true),
            onBackClick = {},
            onConfirm = {},
            onCancel = {},
            onPay = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "OrderDetail - Error")
@Composable
fun OrderDetailScreenContentPreview_Error() {
    ServyAppTheme {
        OrderDetailScreenContent(
            state = OrderDetailState(isLoading = false, order = null, errorMessage = "No se pudo cargar el pedido"),
            onBackClick = {},
            onConfirm = {},
            onCancel = {},
            onPay = {}
        )
    }
}
