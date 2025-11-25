package com.example.servyapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.servyapp.domain.model.Restaurant
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.utils.AppLoading

/**
 * 1. CONTENEDOR (State-Holder)
 *
 * Recibe el ViewModel (inyectado desde AppNavigation),
 * recolecta el estado y lo pasa al composable de UI (HomeScreenContent).
 */
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onChatbotClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val state by homeViewModel.uiState.collectAsState()

    // Llama al Composable de UI (Contenido)
    HomeScreenContent(
        state = state,
        onRestaurantClick = { restaurantId ->
            homeViewModel.saveSelectedRestaurant(restaurantId)
        },
        onChatbotClick = onChatbotClick,
        onMapClick = onMapClick
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
fun HomeScreenContent(
    state: HomeState,
    onRestaurantClick: (String) -> Unit,
    onChatbotClick: () -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Usamos Scaffold como el contenedor raíz
    Scaffold(
        modifier = modifier,

        // 2. Movemos tu TopAppBar al slot 'topBar'
        topBar = {
            TopAppBar(
                title = {
                    Text("Restaurantes")
                },
                actions = {
                    // Botón para ir al Mapa
                    IconButton(onClick = onMapClick) {
                        Icon(
                            imageVector = Icons.Default.Place, // O Icons.Default.Map
                            contentDescription = "Ver Mapa"
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onChatbotClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Asistente Nutricional"
                )
            }
        }
    ) { innerPadding ->

        // 4. Tu contenido original va aquí, dentro del 'padding' del Scaffold
        Box(
            modifier = Modifier
                .padding(innerPadding) // Importante: respetar el padding del Scaffold
                .fillMaxSize()
        ) {
            // El 'when' maneja los 3 estados: Carga, Error, Contenido
            val errorMessage = state.errorMessage

            when {
                state.isLoading -> {
                    AppLoading(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    // Contenido principal (lista de restaurantes)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.restaurants) { restaurant ->
                            RestaurantItem(
                                restaurant = restaurant,
                                onRestaurantClick = {
                                    onRestaurantClick(restaurant.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * RestaurantItem (Stateless Sub-component)
 * No necesita cambios, ya era stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantItem(
    restaurant: Restaurant,
    onRestaurantClick: () -> Unit
) {
    Card(
        onClick = onRestaurantClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            AsyncImage(
                model = restaurant.imageURL,
                contentDescription = restaurant.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 3. PREVIEWS FUNCIONALES
 *
 * Creamos vistas previas para los diferentes estados de HomeScreenContent.
 */

// Datos de prueba para los Previews
private val previewRestaurants = listOf(
    Restaurant(id = "1", name = "El Gran Sabor", imageURL = ""),
    Restaurant(id = "2", name = "La Pizzería", imageURL = ""),
    Restaurant(id = "3", name = "Sushi Express", imageURL = "")
)

@Preview(showBackground = true, showSystemUi = true, name = "Con Restaurantes")
@Composable
fun HomeScreenContentPreview_WithData() {
    ServyAppTheme {
        HomeScreenContent(
            state = HomeState(isLoading = false, restaurants = previewRestaurants),
            onRestaurantClick = {},
            onChatbotClick = {},
            onMapClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Cargando")
@Composable
fun HomeScreenContentPreview_Loading() {
    ServyAppTheme {
        HomeScreenContent(
            state = HomeState(isLoading = true),
            onRestaurantClick = {},
            onChatbotClick = {},
            onMapClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Error")
@Composable
fun HomeScreenContentPreview_Error() {
    ServyAppTheme {
        HomeScreenContent(
            state = HomeState(isLoading = false, errorMessage = "No se pudo conectar al servidor."),
            onRestaurantClick = {},
            onChatbotClick = {},
            onMapClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Vacío")
@Composable
fun HomeScreenContentPreview_Empty() {
    ServyAppTheme {
        HomeScreenContent(
            state = HomeState(isLoading = false, restaurants = emptyList()),
            onRestaurantClick = {},
            onChatbotClick = {},
            onMapClick = {}
        )
    }
}