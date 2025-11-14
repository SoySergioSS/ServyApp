package com.example.servyapp.ui.components

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.Locale

/**
 * Inicializa y recuerda el motor TTS. Devuelve null hasta que esté listo.
 */
@Composable
fun rememberTextToSpeech(context: Context): TextToSpeech? {
    val ttsInitialized = remember { mutableStateOf(false) }
    val ttsEngine = remember { mutableStateOf<TextToSpeech?>(null) }

    LaunchedEffect(Unit) {
        ttsEngine.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Idioma por defecto (puedes cambiarlo a es-PE si tu motor lo soporta)
                val result = ttsEngine.value?.setLanguage(Locale("es", "ES"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Idioma no disponible.")
                    ttsInitialized.value = false
                } else {
                    ttsInitialized.value = true
                }
            } else {
                Log.e("TTS", "Fallo en la inicialización de TTS.")
            }
        }
    }

    DisposableEffect(ttsEngine.value) {
        onDispose {
            ttsEngine.value?.stop()
            ttsEngine.value?.shutdown()
            Log.d("TTS", "Motor TTS apagado.")
        }
    }

    return if (ttsInitialized.value) ttsEngine.value else null
}

/**
 * Habla el texto usando el motor TTS entregado.
 */
fun speakText(
    ttsEngine: TextToSpeech?,
    text: String,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    if (ttsEngine == null) {
        onError("Motor TTS no disponible.")
        return
    }

    onStart()

    ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { onDone() }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { onError("Error de TTS nativo") }
    })

    ttsEngine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "utteranceId")
}