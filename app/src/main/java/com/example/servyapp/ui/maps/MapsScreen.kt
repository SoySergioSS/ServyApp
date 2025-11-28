package com.example.servyapp.ui.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.servyapp.R
import com.example.servyapp.domain.model.Restaurant
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
    onBackClick: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    // Inyectamos el ViewModel aquí
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState() // Observamos el estado

    // 1. Launcher de Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Avisamos al ViewModel del resultado
            viewModel.updatePermissionStatus(isGranted)
        }
    )

    // 2. Efecto Inicial (Permisos + Inicialización Gráfica)
    LaunchedEffect(Unit) {
        // Revisamos permiso inicial
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updatePermissionStatus(isGranted)

        if (!isGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Inicialización de Google Maps (Esto DEBE estar en la UI)
        try {
            MapsInitializer.initialize(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 3. Carga del Icono (Esto es UI pura, se queda aquí)
    var restaurantIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    LaunchedEffect(Unit) {
        restaurantIcon = bitmapDescriptorFromVector(context, R.drawable.ic_restaurant)
    }

    // 4. Configuración del Mapa (Reactiva al estado)
    val properties = remember(state.isLocationPermissionGranted) {
        MapProperties(isMyLocationEnabled = state.isLocationPermissionGranted)
    }
    val uiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = true)
    }

    val defaultLocation = LatLng(-12.0464, -77.0428)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    // 5. Lógica de Zoom (Reactiva al estado userLocation)
    LaunchedEffect(restaurants, state.userLocation) {
        val builder = LatLngBounds.builder()
        var hasPoints = false

        restaurants.forEach {
            if (it.latitude != 0.0 && it.longitude != 0.0) {
                builder.include(LatLng(it.latitude, it.longitude))
                hasPoints = true
            }
        }

        // Usamos la ubicación que viene del ViewModel
        if (state.userLocation != null) {
            builder.include(state.userLocation!!)
            hasPoints = true
        }

        if (hasPoints) {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(builder.build(), 150)
                )
            } catch (e: Exception) { }
        }
    }

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
                            icon = restaurantIcon,
                            onClick = {
                                onRestaurantClick(restaurant.id)
                                true
                            }
                        )
                    }
                }
            }
        }
    }
}

// (La función auxiliar bitmapDescriptorFromVector se mantiene igual al final)
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    // ... (mismo código de antes)
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}