package com.example.servyapp.ui.components

// --- Clases de Estado para gestionar la UI y las interacciones ---
// Estas clases selladas definen los posibles estados de la conversión de voz a texto (STT).
sealed class SpeechToTextState {
    object Idle : SpeechToTextState() // Estado inicial o listo
    object Listening : SpeechToTextState() // Micrófono activo
    data class Result(val text: String) : SpeechToTextState() // Resultado de texto exitoso
    data class Error(val message: String) : SpeechToTextState() // Error durante el STT
}

// Estas clases selladas definen los posibles estados del motor de texto a voz (TTS).
sealed class SpeechToSpeechState {
    object Idle : SpeechToSpeechState() // Motor TTS listo
    object Speaking : SpeechToSpeechState() // Motor TTS reproduciendo audio
    data class Error(val message: String) : SpeechToSpeechState() // Error durante el TTS
}