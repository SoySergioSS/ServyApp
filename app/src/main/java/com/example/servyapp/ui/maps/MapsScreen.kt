package com.example.servyapp.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.servyapp.R
import com.example.servyapp.domain.model.Restaurant
import com.google.android.gms.location.LocationServices // NUEVO IMPORT
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    restaurants: List<Restaurant>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // 1. Estado para el Permiso
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // NUEVO: Estado para guardar TU ubicación
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // 2. Lanzador de Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    // 3. Inicialización y Obtención de Ubicación
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        try {
            MapsInitializer.initialize(context)
            // NUEVO: Si hay permiso, pedimos la ubicación al sistema
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationClient = LocationServices.getFusedLocationProviderClient(context)
                locationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = LatLng(location.latitude, location.longitude)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // NUEVO: Efecto secundario para cuando nos dan el permiso después de iniciar
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val locationClient = LocationServices.getFusedLocationProviderClient(context)
                locationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = LatLng(location.latitude, location.longitude)
                    }
                }
            } catch (e: SecurityException) { /* Ignorar */ }
        }
    }

    // 4. Icono Personalizado
    var restaurantIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    LaunchedEffect(Unit) {
        restaurantIcon = bitmapDescriptorFromVector(context, R.drawable.ic_restaurant)
    }

    // 5. Configuración del Mapa
    val properties by remember(hasLocationPermission) {
        mutableStateOf(MapProperties(isMyLocationEnabled = hasLocationPermission))
    }
    val uiSettings by remember {
        mutableStateOf(MapUiSettings(myLocationButtonEnabled = true))
    }

    val defaultLocation = LatLng(-12.0464, -77.0428)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    // --- LÓGICA DE ZOOM ACTUALIZADA ---
    // Se ejecuta cuando cambian los restaurantes O cuando obtenemos tu ubicación
    LaunchedEffect(restaurants, userLocation) {
        val builder = LatLngBounds.builder()
        var hasPoints = false

        // A. Añadir Restaurantes
        restaurants.forEach {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                builder.include(LatLng(it.latitude, it.longitude))
                hasPoints = true
            }
        }

        // B. NUEVO: Añadir al Usuario (si tenemos su ubicación)
        if (userLocation != null) {
            builder.include(userLocation!!)
            hasPoints = true
        }

        if (hasPoints) {
            try {
                // Ajustamos el zoom para que quepa todo
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 150) // 150px de margen
                )
            } catch (e: Exception) {
                // El mapa puede no estar listo aún en el primer frame
            }
        }
    }
    // ------------------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Restaurantes") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings
            ) {
                restaurants.forEach { restaurant ->
                    if (restaurant.latitude != 0.0) {
                        Marker(
                            state = MarkerState(position = LatLng(restaurant.latitude, restaurant.longitude)),
                            title = restaurant.name,
                            snippet = restaurant.address,
                            icon = restaurantIcon
                        )
                    }
                }
            }
        }
    }
}

// Función auxiliar
fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}