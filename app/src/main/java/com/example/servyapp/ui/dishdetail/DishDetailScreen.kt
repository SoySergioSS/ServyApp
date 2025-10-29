package com.example.servyapp.ui.dishdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.servyapp.domain.model.Dish
import com.example.servyapp.ui.theme.ServyAppTheme

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Se encarga de la lógica: obtener el ViewModel, manejar
 * los side-effects (Snackbars) y recolectar el estado.
 */
@Composable
fun DishDetailScreen(
    viewModel: DishDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onCartClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Side-effect para notificar "Agregado al carrito"
    LaunchedEffect(state.addedToCart) {
        if (state.addedToCart) {
            snackbarHostState.showSnackbar("Agregado al carrito")
            viewModel.resetAddedToCartState()
        }
    }

    // Side-effect para notificar error al agregar (ej. carrito de otro restaurante)
    LaunchedEffect(state.errorMessage) {
        // Solo mostramos el snackbar de error si NO estamos en un estado de error de carga
        // (es decir, el error fue por una acción, no por cargar la página)
        val message = state.errorMessage
        if (state.dish != null && !state.isLoading && message != null) {
            snackbarHostState.showSnackbar(message)
        }
    }

    // Llamamos al Composable de UI (Contenido)
    DishDetailScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onCartClick = onCartClick,
        onIncrementQuantity = { viewModel.incrementQuantity() },
        onDecrementQuantity = { viewModel.decrementQuantity() },
        onAddToCart = { viewModel.addToCart() }
    )
}

/**
 * 2. CONTENIDO (Stateless)
 *
 * Se encarga SOLO de la UI. Contiene el Scaffold y el `when`
 * para decidir qué estado mostrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreenContent(
    state: DishDetailState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(state.dish?.name ?: "Detalle del Platillo")
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
                    IconButton(onClick = onCartClick) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "Carrito de Compras"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // Este `when` ahora maneja todos los estados de la UI
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

            // Si hay un error DE CARGA (el platillo es null)
            state.dish == null && state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            state.dish != null -> {
                // El contenido principal del platillo
                DishDetailBody(
                    dish = state.dish,
                    quantity = state.quantity,
                    onIncrementQuantity = onIncrementQuantity,
                    onDecrementQuantity = onDecrementQuantity,
                    onAddToCart = onAddToCart,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // Caso de borde (no loading, no error, no dish)
            else -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Platillo no disponible.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * 3. CONTENIDO DEL CUERPO (Stateless)
 *
 * Este era tu `DishDetailContent` original. Lo renombré a `DishDetailBody`
 * y ahora recibe los parámetros exactos que necesita, en lugar de todo el `state`.
 */
@Composable
fun DishDetailBody(
    dish: Dish,
    quantity: Int,
    onIncrementQuantity: () -> Unit,
    onDecrementQuantity: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = dish.imageURL,
                contentDescription = dish.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$${String.format("%.2f", dish.price)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Descripción",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dish.description.ifEmpty { "Sin descripción disponible" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cantidad",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = onDecrementQuantity,
                        enabled = quantity > 1, // Recibe 'quantity'
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (quantity > 1) // Recibe 'quantity'
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Disminuir cantidad",
                            tint = if (quantity > 1) // Recibe 'quantity'
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = quantity.toString(), // Recibe 'quantity'
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = onIncrementQuantity,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar cantidad",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", dish.price * quantity)}", // Recibe 'dish' y 'quantity'
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAddToCart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Agregar al Carrito",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


/**
 * 4. PREVIEWS FUNCIONALES
 */

// Datos de prueba
private val previewDish = Dish(
    id = "d1",
    name = "Platillo de Prueba",
    description = "Esta es una descripción larga y detallada del platillo para que ocupe un poco de espacio en el preview y se vea realista.",
    price = 25.99,
    imageURL = "https://placehold.co/600x400/png" // No importa para el preview
)

@Preview(showBackground = true, showSystemUi = true, name = "Estado Principal (Contenido)")
@Composable
fun DishDetailScreenContentPreview_Content() {
    ServyAppTheme {
        DishDetailScreenContent(
            state = DishDetailState(dish = previewDish, quantity = 2, isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onCartClick = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onAddToCart = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Estado de Carga")
@Composable
fun DishDetailScreenContentPreview_Loading() {
    ServyAppTheme {
        DishDetailScreenContent(
            state = DishDetailState(isLoading = true),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onCartClick = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onAddToCart = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Estado de Error de Carga")
@Composable
fun DishDetailScreenContentPreview_Error() {
    ServyAppTheme {
        DishDetailScreenContent(
            // El error de carga se da si el platillo es null Y hay un mensaje de error
            state = DishDetailState(isLoading = false, dish = null, errorMessage = "No se pudo cargar el platillo."),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onCartClick = {},
            onIncrementQuantity = {},
            onDecrementQuantity = {},
            onAddToCart = {}
        )
    }
}