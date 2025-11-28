import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.servyapp.domain.model.VisualItem
import com.example.servyapp.ui.waiter.WaiterNavigationEvent
import com.example.servyapp.ui.components.*
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.waiter.WaiterState
import com.example.servyapp.ui.waiter.WaiterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onBackClick: () -> Unit,
    onNavigateToOrders: () -> Unit,
) {
    val viewModel: WaiterViewModel = hiltViewModel()
    val context = LocalContext.current

    val state by viewModel.uiState.collectAsState()

    // 1. ESCUCHAR EL EVENTO DE NAVEGACIÓN (Igual que en CartScreen)
    LaunchedEffect(state.navigationEvent) {
        when (state.navigationEvent) {
            is WaiterNavigationEvent.NavigateToOrders -> {
                onNavigateToOrders()
                viewModel.onNavigationEventHandled()
            }
            null -> { }
        }
    }
    // --- Estados del ViewModel ---
    val aiMessage = viewModel.aiMessage
    val visualItems = viewModel.visualItems
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val currentPhase = viewModel.currentPhase
    val snackbarMessage = viewModel.snackbarMessage
    val localCartItems by viewModel.cartItems.collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage() // Limpiamos para que no se repita
        }
    }

    // --- Lógica de Audio (TTS / STT) ---
    val ttsEngine = rememberTextToSpeech(context)
    var sttState by remember { mutableStateOf<SpeechToTextState>(SpeechToTextState.Idle) }
    var ttsState by remember { mutableStateOf<SpeechToSpeechState>(SpeechToSpeechState.Idle) }

    // Configurar Voz TTS (intenta buscar una voz en español)
    LaunchedEffect(ttsEngine) {
        val voices = ttsEngine?.voices?.filter { it.locale.language == "es" }
        val bestVoice = voices?.firstOrNull { it.name.contains("es-us", ignoreCase = true) }
            ?: voices?.firstOrNull()
        bestVoice?.let { ttsEngine?.voice = it }
    }

    // Gestor de Reconocimiento de Voz
    val sttManager = rememberSpeechRecognizerManager(
        onResult = { text ->
            sttState = SpeechToTextState.Result(text)
            // ENVIAR AL BACKEND AUTOMÁTICAMENTE
            viewModel.sendToBackend(text)
        },
        onError = { msg -> sttState = SpeechToTextState.Error(msg) },
        onReady = {},
        onListening = { sttState = SpeechToTextState.Listening }
    )

    // Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) sttManager.startListening()
    }

    // Efecto: Leer respuesta del Backend cuando cambia
    LaunchedEffect(aiMessage) {
        if (aiMessage.isNotEmpty()) {
            speakText(
                ttsEngine = ttsEngine,
                text = aiMessage,
                onStart = { ttsState = SpeechToSpeechState.Speaking },
                onDone = { ttsState = SpeechToSpeechState.Idle },
                onError = { ttsState = SpeechToSpeechState.Error(it) }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Asistente Servy", style = MaterialTheme.typography.titleMedium)
                        if(isLoading) {
                            Text("Procesando...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (sttState is SpeechToTextState.Listening) {
                        sttManager.stopListening()
                        sttState = SpeechToTextState.Idle
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            sttManager.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                containerColor = if (sttState is SpeechToTextState.Listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (sttState is SpeechToTextState.Listening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Hablar"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // 1. Área de Chat / Estado (Superior)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (aiMessage.isNotEmpty()) {
                    Text(
                        text = "\"$aiMessage\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    Text("Presiona el micrófono y pide lo que se te antoje.", color = Color.Gray)
                }
            }

            // 2. ÁREA DE CONTENIDO DINÁMICO (AQUÍ ESTÁ EL CAMBIO)
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Decidimos qué mostrar según la fase que envía el Backend
                when (currentPhase) {
                    "summary" -> {
                        // MOSTRAR VISTA DE RESUMEN DE PEDIDO
                        val summaryVisuals = localCartItems.map { cartItem ->
                            VisualItem(
                                id = cartItem.id,
                                title = cartItem.dish.name,
                                // Mostramos cantidad y precio total de ese ítem
                                subtitle = "${cartItem.quantity} un. • Total: S/ ${String.format("%.2f", cartItem.totalPrice)}",
                                imageSeed = cartItem.dish.imageURL, // Usamos la imagen real
                                type = "cart_item"
                            )
                        }

                        OrderSummaryView(
                            items = summaryVisuals, // 👈 Usamos la lista local convertida
                            onActionClick = { viewModel.onNavigateToOrdersClick()},
                            modifier = Modifier.weight(1f)
                        )

                        // Opcional: Si el carrito está vacío, mostrar mensaje
                        if (localCartItems.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                Text("Tu carrito está vacío. ¡Pide algo rico!")
                            }
                        }
                    }
                    else -> {
                        // MOSTRAR VISTA DE GRILLA (Restaurantes / Platos)
                        // Esta es la lógica que ya tenías, envuelta en el else
                        if (visualItems.isNotEmpty()) {
                            Text(
                                text = if (currentPhase == "restaurants") "Restaurantes" else "Menú Disponible",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(visualItems) { item ->
                                    VisualItemCard(item = item) {
                                        viewModel.onVisualItemClicked(item)
                                    }
                                }
                            }
                        } else {
                            // Estado vacío por defecto
                            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Espacio para errores
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun VisualItemCard(item: VisualItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen de Fondo (Usamos Picsum con seed para consistencia, igual que en web)
            val modelUrl = if (item.imageSeed.startsWith("http")){
                item.imageSeed
            } else {
                "https://picsum.photos/seed/${item.imageSeed}/300/300"
            }
            AsyncImage(
                model = modelUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradiente para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            // Texto
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                // Badge tipo
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = when(item.type) {
                            "restaurant" -> "Restaurante"
                            "category" -> "Categoría"
                            "dish" -> "Platillo"
                            else -> item.type
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- NUEVO COMPOSABLE: VISTA DE RESUMEN ---
@Composable
fun OrderSummaryView(
    items: List<VisualItem>,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Resumen de tu Pedido",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (items.isEmpty()) {
            Text("Tu carrito está vacío por ahora.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    OrderItemRow(item)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón visual de "Listo" (Aunque la IA ya lo confirma verbalmente)
        Button(
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ir a Mi Orden / Escanear QR")
        }
    }
}

@Composable
fun OrderItemRow(item: VisualItem) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asumimos que el backend envía la imagen pequeña o un icono genérico
            val modelUrl = if (item.imageSeed.startsWith("http")){
                item.imageSeed
            } else {
                "https://picsum.photos/seed/${item.imageSeed}/100/100"
            }
            AsyncImage(
                model = modelUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title, // Ej: "Ramen Tonkotsu"
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle, // Ej: "Cantidad: 2" o "$16.00"
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceAssistantScreenPreview() {
    ServyAppTheme {
        VoiceAssistantScreen(onBackClick = { Unit },
            onNavigateToOrders = { Unit })
    }
}