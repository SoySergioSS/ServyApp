package com.example.servyapp.ui.maps

import com.google.android.gms.maps.model.LatLng

data class MapState(
    val userLocation: LatLng? = null,
    val isLocationPermissionGranted: Boolean = false
)