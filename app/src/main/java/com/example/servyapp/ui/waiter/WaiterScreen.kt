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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.servyapp.ui.theme.ServyAppTheme
import com.example.servyapp.ui.components.SpeechToSpeechState //
import com.example.servyapp.ui.components.SpeechToTextState //
import com.example.servyapp.ui.components.rememberAvailableVoices //
import com.example.servyapp.ui.components.rememberSpeechRecognizerManager //
import com.example.servyapp.ui.components.rememberTextToSpeech //
import com.example.servyapp.ui.components.speakText //
import com.example.servyapp.ui.waiter.WaiterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onBackClick: () -> Unit
) {
    // --- ViewModel para el backend ---
    val chatViewModel: WaiterViewModel = viewModel()

    val replyMessage = chatViewModel.replyMessage
    val isLoading = chatViewModel.isLoading
    val errorMessage = chatViewModel.errorMessage

    // --- 1. Lógica de TTS y STT que ya tenías ---
    val context = LocalContext.current
    val ttsEngine = rememberTextToSpeech(context)

    var sttState by remember { mutableStateOf<SpeechToTextState>(SpeechToTextState.Idle) }
    var ttsState by remember { mutableStateOf<SpeechToSpeechState>(SpeechToSpeechState.Idle) }

    val voices = rememberAvailableVoices(ttsEngine)
    val selectedVoice by remember(voices) {
        mutableStateOf(
            voices.firstOrNull { it.voice.features?.contains("male") == true }
                ?: voices.firstOrNull()
        )
    }

    LaunchedEffect(ttsEngine, selectedVoice) {
        val engine = ttsEngine ?: return@LaunchedEffect
        selectedVoice?.let { v ->
            engine.language = v.voice.locale
            engine.voice = v.voice
        }
    }

    val sttManager = rememberSpeechRecognizerManager(
        onResult = { recognizedText ->
            sttState = SpeechToTextState.Result(recognizedText)
        },
        onError = { errorMessageStt ->
            sttState = SpeechToTextState.Error(errorMessageStt)
        },
        onReady = { /* No es necesario aquí */ },
        onListening = {
            sttState = SpeechToTextState.Listening
        }
    )

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            sttManager.startListening()
        } else {
            sttState = SpeechToTextState.Error("Permiso de micrófono denegado.")
        }
    }

    fun handleSpeakText(text: String) {
        speakText(
            ttsEngine = ttsEngine,
            text = text,
            onStart = { ttsState = SpeechToSpeechState.Speaking },
            onDone = { ttsState = SpeechToSpeechState.Idle },
            onError = { msg -> ttsState = SpeechToSpeechState.Error(msg) }
        )
    }

    fun handleListenClick() {
        if (sttState is SpeechToTextState.Listening) {
            sttManager.stopListening()
            sttState = SpeechToTextState.Idle
            return
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sttState = SpeechToTextState.Listening
            sttManager.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // 👉 Cuando el STT produce un resultado, enviamos el texto al backend
    LaunchedEffect(sttState) {
        val state = sttState
        if (state is SpeechToTextState.Result) {
            chatViewModel.sendVoiceMessage(state.text)
        }
    }

    // 👉 Cuando llega la respuesta, la leemos en voz alta (opcional)
    LaunchedEffect(replyMessage) {
        if (replyMessage.isNotBlank()) {
            handleSpeakText(replyMessage)
        }
    }

    // --- 5. UI ---
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
                onClick = { handleListenClick() },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Texto del usuario (antes "HOLA")
                val userText = when (val state = sttState) {
                    SpeechToTextState.Idle -> "HOLA"
                    SpeechToTextState.Listening -> "Escuchando..."
                    is SpeechToTextState.Result -> state.text        // 👉 aquí se mantiene el mensaje
                    is SpeechToTextState.Error -> "Error: ${state.message}"
                }

                Text(
                    text = userText,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Estado de carga / error / respuesta de Gemini
                if (isLoading) {
                    Text(
                        text = "Consultando a Gemini...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = "Error: $errorMessage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (replyMessage.isNotEmpty()) {
                    // 👉 Respuesta con otro color para distinguirla
                    Text(
                        text = replyMessage,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiaryContainer, // diferente a onPrimary
                    )
                }

                // 3. Estado del TTS
                val ttsStatusText = when (ttsState) {
                    SpeechToSpeechState.Speaking -> "Hablando..."
                    is SpeechToSpeechState.Error -> "Error al hablar"
                    else -> ""
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