// --- Asegúrate de tener todos estos imports ---
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.components.SpeechToSpeechState //
import com.example.servyapp.ui.components.SpeechToTextState //
import com.example.servyapp.ui.components.rememberAvailableVoices //
import com.example.servyapp.ui.components.rememberSpeechRecognizerManager //
import com.example.servyapp.ui.components.rememberTextToSpeech //
import com.example.servyapp.ui.components.speakText //

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onBackClick: () -> Unit
) {
    // --- 1. Lógica copiada de SpeechScreen.kt ---
    val context = LocalContext.current
    val ttsEngine = rememberTextToSpeech(context)

    var sttState by remember { mutableStateOf<SpeechToTextState>(SpeechToTextState.Idle) } //
    var ttsState by remember { mutableStateOf<SpeechToSpeechState>(SpeechToSpeechState.Idle) } //

    // --- Lógica de selección de voz (sin la UI del Picker) ---
    val voices = rememberAvailableVoices(ttsEngine) //
    // Heurística: selecciona por defecto una voz masculina si existe
    val selectedVoice by remember(voices) {
        mutableStateOf(
            voices.firstOrNull { it.voice.features?.contains("male") == true }
                ?: voices.firstOrNull()
        )
    } //

    // Aplica la voz seleccionada al TTS
    LaunchedEffect(ttsEngine, selectedVoice) {
        val engine = ttsEngine ?: return@LaunchedEffect
        selectedVoice?.let { v ->
            engine.language = v.voice.locale
            engine.voice = v.voice
        }
    } //

    // --- 2. Manager de Reconocimiento de Voz (STT) ---
    val sttManager = rememberSpeechRecognizerManager(
        onResult = { recognizedText ->
            sttState = SpeechToTextState.Result(recognizedText)
        },
        onError = { errorMessage ->
            sttState = SpeechToTextState.Error(errorMessage)
        },
        onReady = { /* No es necesario aquí */ },
        onListening = {
            sttState = SpeechToTextState.Listening
        }
    ) //

    // --- 3. Lanzador de Permisos ---
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            sttManager.startListening()
        } else {
            sttState = SpeechToTextState.Error("Permiso de micrófono denegado.")
        }
    } //

    // --- 4. Handlers (Funciones de control) ---
    fun handleSpeakText(text: String) {
        speakText(
            ttsEngine = ttsEngine,
            text = text,
            onStart = { ttsState = SpeechToSpeechState.Speaking },
            onDone = { ttsState = SpeechToSpeechState.Idle },
            onError = { msg -> ttsState = SpeechToSpeechState.Error(msg) }
        )
    } //

    fun handleListenClick() {
        if (sttState is SpeechToTextState.Listening) {
            sttManager.stopListening()
            sttState = SpeechToTextState.Idle
            return
        }
        // Verifica el permiso y lo solicita si es necesario
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            sttState = SpeechToTextState.Listening
            sttManager.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    } //


    // --- 5. UI (Tu Scaffold modificado) ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                // *** CONECTADO ***
                onClick = { handleListenClick() },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // *** ICONO DINÁMICO ***
                val icon = if (sttState is SpeechToTextState.Listening) {
                    Icons.Default.Stop
                } else {
                    Icons.Default.Mic
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Grabar",
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.primary
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            // --- TEXTO DINÁMICO (en lugar de solo "HOLA") ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Muestra el estado del STT
                val displayText = when (val state = sttState) {
                    SpeechToTextState.Idle -> "HOLA"
                    SpeechToTextState.Listening -> "Escuchando..."
                    is SpeechToTextState.Result -> state.text
                    is SpeechToTextState.Error -> "Error: ${state.message}"
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                // 2. Muestra el estado del TTS
                val ttsStatusText = when (ttsState) {
                    SpeechToSpeechState.Speaking -> "Hablando..."
                    else -> "" // No mostrar nada en Idle o Error para mantener limpia la UI
                }
                if (ttsStatusText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = ttsStatusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
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