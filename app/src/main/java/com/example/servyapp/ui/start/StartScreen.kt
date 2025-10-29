package com.example.servyapp.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.servyapp.R

// Aquí deberías tener tu propio R.drawable.servyapp_logo
// Por simplicidad, se usará un placeholder
// import com.tucodigo.R // Tu R.java

@Composable
fun StartScreen(
    loginButtonPressed: () -> Unit,
    signupButtonPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logorobot_servyapp), //Cambiar por el logo
            contentDescription = stringResource(R.string.servyapp_logo),
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 20.dp)
        )

        Text(
            text = stringResource(R.string.bienvenido_a_servyapp),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Descripción por ahora hardcodeada.", //Cambiar la  descripción
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        Button(
            onClick = loginButtonPressed,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = stringResource(R.string.iniciar_sesi_n))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = signupButtonPressed,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = stringResource(R.string.registrarse))
        }
    }
}

@Preview(
    showSystemUi = true,
    showBackground = true
)
@Composable
fun WelcomeScreenPreview() {
    StartScreen(
        loginButtonPressed = {},
        signupButtonPressed = {}
    )
}