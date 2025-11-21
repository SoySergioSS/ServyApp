import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.servyapp.domain.model.VisualItem
import com.example.servyapp.ui.components.*
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.waiter.WaiterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onBackClick: () -> Unit
) {
    val viewModel: WaiterViewModel = viewModel()
    val context = LocalContext.current

    // --- Estados del ViewModel ---
    val aiMessage = viewModel.aiMessage
    val visualItems = viewModel.visualItems
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

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

            // 2. Área Visual (Grilla de Resultados)
            if (visualItems.isNotEmpty()) {
                Text(
                    text = "Opciones Disponibles",
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
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
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
            AsyncImage(
                model = "https://picsum.photos/seed/${item.imageSeed}/300/300",
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

@Preview(showBackground = true)
@Composable
fun VoiceAssistantScreenPreview() {
    ServyAppTheme {
        VoiceAssistantScreen(onBackClick = { Unit })
    }
}