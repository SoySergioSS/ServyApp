package com.example.servyapp.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SplashScreen(
    navigateToHome: () -> Unit,
    navigateToStart: () -> Unit,
    splashViewModel: SplashViewModel
)
{
    val navigateToHome by splashViewModel.navigateToHome.collectAsState()

    if(navigateToHome){
        navigateToHome()
    } else{
        navigateToStart()
    }
}