package com.example.servyapp.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.example.servyapp.R

@Composable
fun LogoApp(
    modifier: Modifier = Modifier
){
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_background), //Cambiar por imagen de logo
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun AppButton(
    textoBoton: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.teal_200) //Cambiar color
        ),
        modifier = modifier
    ) {
        Text(
            text = textoBoton,
            fontSize = 20.sp
        )
    }
}

@Composable
fun AppLoading(
    modifier: Modifier = Modifier
){
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator() //mas adelante cambiar por una personalizada
    }
}