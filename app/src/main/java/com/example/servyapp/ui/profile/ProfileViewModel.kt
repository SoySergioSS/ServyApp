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

    fun showEditDialog() {
        _uiState.update { it.copy(showEditDialog = true, errorMessage = null, updateSuccess = false) }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, errorMessage = null, updateSuccess = false) }
    }

    fun updateProfile(phone: String, newPassword: String?, currentPassword: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, errorMessage = null) }

            try {
                val uid = authRepository.currentUser?.uid
                if (uid == null) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Usuario no autenticado",
                            isUpdating = false
                        )
                    }
                    return@launch
                }

                // Actualizar teléfono si cambió
                val currentPhone = _uiState.value.phone
                if (phone != currentPhone && phone.isNotBlank()) {
                    userRepository.updateUserPhone(uid, phone)
                    _uiState.update { it.copy(phone = phone) }
                }

                // Actualizar contraseña si se proporcionó
                if (!newPassword.isNullOrBlank()) {
                    if (currentPassword.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Debes ingresar tu contraseña actual para cambiarla",
                                isUpdating = false
                            )
                        }
                        return@launch
                    }

                    if (newPassword.length < 6) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "La nueva contraseña debe tener al menos 6 caracteres",
                                isUpdating = false
                            )
                        }
                        return@launch
                    }

                    // Reautenticar usuario antes de cambiar contraseña
                    try {
                        authRepository.reauthenticateUser(currentPassword)
                        // Si la reautenticación es exitosa, cambiar la contraseña
                        authRepository.updatePassword(newPassword)
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                errorMessage = "Contraseña actual incorrecta",
                                isUpdating = false
                            )
                        }
                        return@launch
                    }
                }

                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        updateSuccess = true,
                        showEditDialog = false
                    )
                }

                // Recargar perfil para asegurar datos actualizados
                loadUserProfile()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Error al actualizar: ${e.message}",
                        isUpdating = false
                    )
                }
            }
        }
    }

    fun logout(){
        authRepository.logout()
    }
}