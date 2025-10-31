package com.example.servyapp.ui.cart

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.servyapp.domain.model.CartItem
import com.example.servyapp.domain.model.Dish
import com.example.servyapp.ui.theme.ServyAppTheme

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Se encarga de la lógica: obtener el ViewModel, manejar
 * los eventos de navegación y recolectar el estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onBackClick: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToDishDetail: (restaurantId: String, dishId: String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigationEvent) {
        when (val event = state.navigationEvent) {
            is NavigationEvent.NavigateToOrders -> {
                onNavigateToOrders()
                viewModel.onNavigationEventHandled()
            }
            is NavigationEvent.NavigateToDishDetail -> {
                onNavigateToDishDetail(event.restaurantId, event.dishId)
                viewModel.onNavigationEventHandled()
            }
            null -> { /* No hacer nada */ }
        }
    }

    // ✅ Diálogo de eliminación
    if (state.showDeleteDialog != null) {
        DeleteConfirmationDialog(
            itemName = state.showDeleteDialog!!.dish.name,
            onConfirm = { viewModel.confirmDeleteItem() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }

    // ✅ Diálogo de conflicto de restaurantes
    if (state.showConflictDialog) {
        RestaurantConflictDialog(
            errorMessage = state.errorMessage ?: "Ya tienes una orden activa en otro restaurante",
            onDismiss = { viewModel.dismissConflictDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Carrito de Compras")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isEmpty) {
            EmptyCartContent(
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            CartContent(
                state = state,
                onIncrementQuantity = { viewModel.incrementQuantity(it) },
                onDecrementQuantity = { viewModel.decrementQuantity(it) },
                onDeleteItem = { viewModel.showDeleteDialog(it) },
                onItemClick = { viewModel.onItemClick(it) },
                onPedidoClick = { viewModel.onPedidoClick() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// ✅ Nuevo diálogo para conflictos de restaurante
@Composable
fun RestaurantConflictDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Orden en progreso",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Entendido")
            }
        }
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
fun CartScreenContent(
    state: CartState,
    onBackClick: () -> Unit,
    onIncrementQuantity: (String) -> Unit,
    onDecrementQuantity: (String) -> Unit,
    onDeleteItem: (CartItem) -> Unit,
    onItemClick: (CartItem) -> Unit,
    onPedidoClick: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // El diálogo de confirmación ahora vive aquí
    if (state.showDeleteDialog != null) {
        DeleteConfirmationDialog(
            itemName = state.showDeleteDialog.dish.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Carrito de Compras")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isEmpty) {
            EmptyCartContent(
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // Este es el 'CartContent' que ya existía (LazyColumn + Resumen)
            CartContent(
                state = state,
                onIncrementQuantity = onIncrementQuantity,
                onDecrementQuantity = onDecrementQuantity,
                onDeleteItem = onDeleteItem,
                onItemClick = onItemClick,
                onPedidoClick = onPedidoClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}


// --- El resto de los Composables (EmptyCartContent, CartContent, etc.) ---
// --- no necesitan cambios, ya que eran stateless.                 ---

@Composable
fun EmptyCartContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Text(
                text = "Tu carrito está vacío",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Agrega platillos para comenzar tu pedido",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Este es el Composable que ya tenías, que contiene la LazyColumn
 * y el OrderSummaryCard. Ya era stateless, así que solo lo movemos aquí.
 */
@Composable
fun CartContent(
    state: CartState,
    onIncrementQuantity: (String) -> Unit,
    onDecrementQuantity: (String) -> Unit,
    onDeleteItem: (CartItem) -> Unit,
    onItemClick: (CartItem) -> Unit,
    onPedidoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = state.items,
                key = { it.id }
            ) { item ->
                CartItemCard(
                    item = item,
                    onIncrementQuantity = { onIncrementQuantity(item.id) },
                    onDecrementQuantity = { onDecrementQuantity(item.id) },
                    onDeleteItem = { onDeleteItem(item) },
                    onItemClick = { onItemClick(item) }
                )
            }
        }

        OrderSummaryCard(
            state = state,
            onPedidoClick = onPedidoClick
        )
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onDeleteItem: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = item.dish.imageURL,
                contentDescription = item.dish.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.dish.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDeleteItem,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "$${String.format("%.2f", item.dish.price)} c/u",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onDecrementQuantity,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Disminuir",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = onIncrementQuantity,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Aumentar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "$${String.format("%.2f", item.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    state: CartState,
    onPedidoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen del Pedido",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Items (${state.itemCount})",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$${String.format("%.2f", state.subtotal)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$${String.format("%.2f", state.subtotal)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPedidoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Generar pedido",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "¿Eliminar platillo?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas eliminar \"$itemName\" del carrito?",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


/**
 * 3. PREVIEWS FUNCIONALES
 */

// Datos de prueba para los Previews
private val previewDish = Dish(
    id = "d1",
    name = "Hamburguesa Doble Queso",
    price = 12.50,
    imageURL = "" // No es necesario para el preview si Coil no carga
)
private val previewCartItem = CartItem(
    id = "c1",
    dish = previewDish,
    quantity = 2,
    restaurantId = "r1"
)
private val previewCartItem2 = CartItem(
    id = "c2",
    dish = previewDish.copy(id = "d2", name = "Papas Fritas Grandes", price = 4.75),
    quantity = 1,
    restaurantId = "r1"
)

@Preview(showBackground = true, showSystemUi = true, name = "Carrito con Items")
@Composable
fun CartScreenContentPreview_WithItems() {
    ServyAppTheme {
        CartScreenContent(
            state = CartState(
                items = listOf(previewCartItem, previewCartItem2)
            ),
            onBackClick = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onDeleteItem = {},
            onItemClick = {},
            onPedidoClick = {},
            onConfirmDelete = {},
            onDismissDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Carrito Vacío")
@Composable
fun CartScreenContentPreview_Empty() {
    ServyAppTheme {
        CartScreenContent(
            state = CartState(items = emptyList()), // state.isEmpty será true
            onBackClick = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onDeleteItem = {},
            onItemClick = {},
            onPedidoClick = {},
            onConfirmDelete = {},
            onDismissDelete = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Diálogo de Borrar")
@Composable
fun CartScreenContentPreview_DeleteDialog() {
    ServyAppTheme {
        // Usamos el truco del Box para que el diálogo se muestre en el preview
        Box(modifier = Modifier.fillMaxSize()) {
            CartScreenContent(
                state = CartState(
                    items = listOf(previewCartItem),
                    showDeleteDialog = previewCartItem // <-- Aquí activamos el diálogo
                ),
                onBackClick = {},
                onIncrementQuantity = {},
                onDecrementQuantity = {},
                onDeleteItem = {},
                onItemClick = {},
                onPedidoClick = {},
                onConfirmDelete = {},
                onDismissDelete = {}
            )
        }
    }
}