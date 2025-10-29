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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
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
                        onPay = { viewModel.completeOrder() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

// ✅ Mueve esta función AQUÍ, fuera de OrderDetailScreen
@Composable
fun OrderDetailContent(
    order: com.example.servyapp.domain.model.Order,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

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
                Button(onClick = onPay, modifier = Modifier.fillMaxWidth()) {
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
    }
}
