package com.example.servyapp.ui.components

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Información amigable para mostrar una Voice en la UI */
data class VoiceDisplay(
    val label: String,
    val voice: Voice
)

/**
 * Convierte el set de voces del TTS en una lista filtrada por idioma y ordenada.
 * @param preferredLocales lista de locales preferidos en orden de prioridad.
 */
@Composable
fun rememberAvailableVoices(
    tts: TextToSpeech?,
    preferredLocales: List<Locale> = listOf(Locale("es","ES"), Locale("es","MX"), Locale("es","US"), Locale("es","AR"), Locale("es","PE"))
): List<VoiceDisplay> {
    val voices by remember(tts) {
        mutableStateOf(
            tts?.voices
                ?.filter { v -> preferredLocales.any { it.language == v.locale.language } }
                ?.sortedWith(compareBy<Voice>({ it.locale.toString() }, { it.name }))
                ?.map { v ->
                    val genderTag = when {
                        v.features?.contains("male") == true -> "♂"
                        v.features?.contains("female") == true -> "♀"
                        else -> "∼"
                    }
                    val quality = when (v.quality) {
                        Voice.QUALITY_VERY_HIGH -> "Muy alta"
                        Voice.QUALITY_HIGH -> "Alta"
                        Voice.QUALITY_NORMAL -> "Normal"
                        Voice.QUALITY_LOW -> "Baja"
                        else -> "—"
                    }
                    val latency = when (v.latency) {
                        Voice.LATENCY_VERY_LOW -> "VL"
                        Voice.LATENCY_LOW -> "L"
                        Voice.LATENCY_NORMAL -> "N"
                        Voice.LATENCY_HIGH -> "H"
                        else -> "?"
                    }
                    VoiceDisplay(
                        label = "${v.locale} · ${v.name} · $genderTag · $quality/$latency",
                        voice = v
                    )
                }
                ?: emptyList()
        )
    }
    return voices as List<VoiceDisplay>
}

@Composable
fun VoicePicker(
    voices: List<VoiceDisplay>,
    selected: VoiceDisplay?,
    onSelect: (VoiceDisplay) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = "Voz TTS", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selected?.label ?: if (voices.isEmpty()) "No hay voces disponibles" else "Elegir voz",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (voices.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin voces para el idioma seleccionado") },
                    onClick = { expanded = false }
                )
            } else {
                voices.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}