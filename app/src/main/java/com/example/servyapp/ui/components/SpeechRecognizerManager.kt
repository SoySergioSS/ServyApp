package com.example.servyapp.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Clase que gestiona la API nativa SpeechRecognizer para reconocimiento en segundo plano.
 * Permite controlar el proceso sin mostrar la ventana de diálogo de Google.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onListening: () -> Unit,
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null

    // Configuración del Intent (usa el mismo RecognizerIntent, pero sin lanzarlo)
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES") // Idioma español
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Opción útil para ver resultados parciales
    }

    init {
        // 1. Verificar si el dispositivo soporta el reconocimiento de voz
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Reconocimiento de voz no disponible en este dispositivo.")
            speechRecognizer = null
        }

        // 2. Crear la instancia de SpeechRecognizer y establecer este manager como listener
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@SpeechRecognizerManager)
        }
    }

    fun startListening() {
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // --- Implementación de RecognitionListener ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("STT_BG", "Listo para escuchar.")
        onReady()
    }

    override fun onBeginningOfSpeech() {
        Log.d("STT_BG", "Comenzando a escuchar.")
        onListening()
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Puedes usar esto para animar una barra de sonido en la UI
    }

    override fun onBufferReceived(buffer: ByteArray?) { /* Ignorado por ahora */ }

    override fun onEndOfSpeech() {
        Log.d("STT_BG", "Fin del habla.")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red."
            SpeechRecognizer.ERROR_NETWORK -> "Error de red."
            SpeechRecognizer.ERROR_SERVER -> "Error del servidor."
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz."
            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes."
            else -> "Error STT desconocido: $error"
        }
        onError(errorMessage)
        Log.e("STT_BG", errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val finalResult = matches?.get(0) ?: "No se reconoció la voz."
        onResult(finalResult)
        Log.d("STT_BG", "Resultado final: $finalResult")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Útil para mostrar al usuario lo que se está reconociendo en tiempo real
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        // Opcional: onResult(matches?.get(0) ?: "")
    }

    override fun onEvent(eventType: Int, params: Bundle?) { /* Ignorado por ahora */ }
}

@Composable
fun rememberSpeechRecognizerManager(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onReady: () -> Unit,
    onListening: () -> Unit
): SpeechRecognizerManager {
    val context = LocalContext.current
    val manager = remember {
        SpeechRecognizerManager(context, onResult, onError, onReady, onListening)
    }

    // Limpia el recurso cuando el Composable se destruye
    DisposableEffect(Unit) {
        onDispose {
            manager.destroy()
        }
    }
    return manager
}