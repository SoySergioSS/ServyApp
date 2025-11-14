package com.example.servyapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.ui.Modifier

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Restaurantes",
        icon = Icons.Default.Home
    )
    
    object Cart : BottomNavItem(
        route = "orders",
        title = "Mi Orden",
        icon = Icons.Default.ShoppingCart
    )

    object VirtualWaiter : BottomNavItem(
        route = "virtual_waiter", // Esta será la nueva ruta
        title = "Mesero",
        icon = Icons.Default.SupportAgent
    )

    object Stats : BottomNavItem(
        route = "stats", // Esta será la nueva ruta
        title = "Estadísticas",
        icon = Icons.Default.BarChart
    )
    
    object Profile : BottomNavItem(
        route = "profile",
        title = "Perfil",
        icon = Icons.Default.Person
    )
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
){
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Cart,
        BottomNavItem.VirtualWaiter,
        BottomNavItem.Stats,
        BottomNavItem.Profile
    )

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                }
            )
        }
    }
}

