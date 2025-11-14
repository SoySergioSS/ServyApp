package com.example.servyapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.rememberNavController
import com.example.servyapp.navigation.AppNavigation

@Composable
fun ServyappApp(){

        val navController = rememberNavController()

        AppNavigation(
            navController,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServyappTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "ServyApp",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}