package com.example.servyapp.ui.Plates

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun PlatesScreen(
    platesViewModel: PlatesViewModel
){
    val state by platesViewModel.uiState.collectAsState()
    Column {
        Text(
            text = "Diferentes platos"
        )
        Text(
            text = "idRestaurant: ${state.idRestaurant}"
        )
    }
}