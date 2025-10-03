package com.example.servyapp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.servyapp.ui.utils.AppLoading

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    logoutButtonPressed: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Usamos Surface para darle un color de fondo consistente a toda la pantalla
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.isLoading) {
            // Reemplazamos AppLoading con un indicador de carga centrado para mejor UX
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Contenido principal de la pantalla de perfil
            ProfileContent(
                email = state.email,
                phone = state.phone,
                onLogout = {
                    viewModel.logout()
                    logoutButtonPressed()
                },
                onEditProfile = {
                    // Aquí puedes añadir la lógica para editar el perfil
                }
            )
        }
    }
}

@Composable
fun ProfileContent(
    email: String,
    phone: String,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // --- SECCIÓN DE AVATAR Y NOMBRE ---
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar de Perfil",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = email,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN DE INFORMACIÓN DEL USUARIO ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Correo",
                    value = email
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Teléfono",
                    value = phone
                )
                // Puedes agregar más filas de información aquí si es necesario
                // ProfileInfoRow(...)
            }
        }

        // Spacer que empuja los botones hacia la parte inferior
        Spacer(modifier = Modifier.weight(1f))

        // --- SECCIÓN DE BOTONES DE ACCIÓN ---
        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Editar")
            Spacer(Modifier.width(8.dp))
            Text("Cambiar datos")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.error // Color rojo para indicar acción destructiva
            )
            Spacer(Modifier.width(8.dp))
            Text("Logout", color = MaterialTheme.colorScheme.error)
        }
    }
}

/**
 * Un Composable reutilizable para mostrar una fila de información con un icono.
 */
@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Un color más sutil para la etiqueta
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
