package com.example.servyapp.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.servyapp.ui.utils.AppLoading

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    logoutButtonPressed: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        AppLoading()
    } else {
        Column {
            Text(
                text = "Email: ${state.email}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Phone: ${state.phone}",
                style = MaterialTheme.typography.bodyLarge
            )
            Row {
//            Button(
//                onClick = {
//                    viewModel.logout()
//                    logoutButtonPressed()
//                }
//            ) {
//                Text("Cambiar datos")
//            }
                Button(
                    onClick = {
                        viewModel.logout()
                        logoutButtonPressed()
                    }
                ) {
                    Text("Logout")
                }
            }

        }
    }
}