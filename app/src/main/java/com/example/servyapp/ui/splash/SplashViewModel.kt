package com.example.servyapp.ui.splash

import androidx.lifecycle.ViewModel
import com.example.servyapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome: StateFlow<Boolean> = _navigateToHome

    init{
        checkUser()
    }

    private fun checkUser(){
        _navigateToHome.value = authRepository.currentUser != null
    }
}