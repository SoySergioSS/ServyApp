package com.example.servyapp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.servyapp.data.repository.AuthRepository
import com.example.servyapp.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileState(
        email = authRepository.currentUser?.email ?: "",
        phone = "nada"
    ))
    val uiState: StateFlow<ProfileState> = _uiState

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _uiState.update { it.copy(isLoading = true) }

        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            val userProfile = userRepository.getUserProfile(uid)

            userProfile?.let { user ->
                _uiState.update {
                    it.copy(
                        phone = user.phone,
                        email = user.email,

                    )
                }
            }
        }

        _uiState.update { it.copy(isLoading = false) }
    }

    fun logout(){
        authRepository.logout()
    }
}