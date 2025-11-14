package com.example.servyapp.ui.orderdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.servyapp.domain.model.Order
import com.example.servyapp.domain.model.OrderStatus
import com.example.servyapp.domain.model.Pedido
import com.example.servyapp.domain.model.PedidoItem
import com.example.servyapp.domain.model.PedidoStatus
import com.example.servyapp.ui.theme.ServyAppTheme
import com.google.firebase.Timestamp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToCard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showSeatSheet by remember { mutableStateOf(false) }

    var requiredSeats by remember { mutableStateOf(1) } // valor inicial 1 asiento

    val qrScannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.validateQrAndConfirmOrder(result.contents)
        } else {
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    LaunchedEffect(state.navigateToCard) {
        if (state.navigateToCard) {
            onNavigateToCard()
            viewModel.navigationToCardComplete()
        }
    }


    LaunchedEffect(state.errorMessage) {
        val errorMessage = state.errorMessage
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.successMessage) {
        val successMessage = state.successMessage
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            viewModel.clearMessages()
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        val order = state.order
        val errorMessage = state.errorMessage
        val isLoading = state.isLoading

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            errorMessage != null && order == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage)
            }

            order != null -> {

                OrderDetailContent(
                    order = order,
                    onConfirm = {
                        showSeatSheet = true
                    },
                    onCancel = { viewModel.cancelOrder() },
                    onPay = { method ->
                        when (method) {
                            PaymentMethod.CASH -> viewModel.handleCashPayment(order.id)
                            PaymentMethod.YAPE -> viewModel.handleYapePayment(order.id)
                            PaymentMethod.CARD -> viewModel.handleCardPayment(order.id)
                        }
                    },
                    onCancelPedido = { pedidoId ->
                        viewModel.cancelPedido(pedidoId)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
                if (showSeatSheet) {
                    SeatSelectionBottomSheet(
                        requiredSeats = requiredSeats,
                        onSeatsChange = { requiredSeats = it },
                        onConfirm = {
                            showSeatSheet = false
                            viewModel.setRequiredSeats(requiredSeats)

                            val options = ScanOptions()
                            options.setPrompt("Escanea el QR del restaurante")
                            options.setBeepEnabled(true)
                            options.setOrientationLocked(false)
                            qrScannerLauncher.launch(options)
                        },
                        onDismiss = { showSeatSheet = false }
                    )
                }
            }
        }
    }
}

enum class PaymentMethod {
    CASH, YAPE, CARD
}
@Composable
fun OrderDetailContent(
    order: Order,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPay: (PaymentMethod) -> Unit,
    onCancelPedido: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {

        item {
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
        }

        items(
            items = order.pedidos,
            key = { it.id }
        ) { pedido ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
            ) {
                pedido.items.forEach { item ->
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

            // El botón para cancelar el pedido individual
            if (order.status == OrderStatus.PENDING) {
                OutlinedButton(
                    onClick = { onCancelPedido(pedido.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancelar este Pedido")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
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
                            Text("Cancelar")
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pedido ${order.status}"
                        )
                    }
                }
            }
        }
    }

        if (showPaymentDialog) {
        PaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onCash = {
                showPaymentDialog = false
                onPay(PaymentMethod.CASH)
            },
            onYape = {
                showPaymentDialog = false
                onPay(PaymentMethod.YAPE)
            },
            onCard = {
                showPaymentDialog = false
                onPay(PaymentMethod.CARD)
            }
        )
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
    onPay: (PaymentMethod) -> Unit,
    onCancelPedido: (String) -> Unit,
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
                OrderDetailContent(
                    order = state.order,
                    onConfirm = onConfirm,
                    onCancel = onCancel,
                    onPay = onPay,
                    onCancelPedido = onCancelPedido,
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionBottomSheet(
    initialSeats: Int = 1,
    requiredSeats: Int,
    onSeatsChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var seatText by remember { mutableStateOf(initialSeats.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Número de Personas",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = seatText,
                onValueChange = { newText ->
                    // Solo permitir números
                    if (newText.all { it.isDigit() }) {
                        seatText = newText
                    }
                },
                label = { Text("Asientos requeridos") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val seats = seatText.toIntOrNull() ?: 1
                    onSeatsChange(seats)
                    onConfirm()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Escanear QR y Confirmar")
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }

}

/* ----------------------------
   Datos de prueba para preview
   ---------------------------- */
private val previewOrderForPreview = Order(
    id = "order123",
    orderNumber = "0001",
    createdAt = Timestamp.now(),
    status = OrderStatus.PENDING,
    pedidos = listOf(
        Pedido(
            id = "p1",
            restaurantId = "r1",
            items = listOf(
                PedidoItem(
                    dishId = "d1",
                    dishName = "Pizza Margarita",
                    dishImageURL = "",
                    dishDescription = "Clásica",
                    quantity = 2,
                    pricePerUnit = 15.0,
                    totalPrice = 30.0
                ),
                PedidoItem(
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
            createdAt = Timestamp.now(),
            status = PedidoStatus.PREPARING
        ),
        Pedido(
            id = "p2",
            restaurantId = "r2",
            items = listOf(
                PedidoItem(
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
            createdAt = Timestamp.now(),
            status = PedidoStatus.PREPARING
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
            onPay = {},
            onCancelPedido = {}
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
            onPay = {},
            onCancelPedido = {}
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
            onPay = {},
            onCancelPedido = {}
        )
    }
}
