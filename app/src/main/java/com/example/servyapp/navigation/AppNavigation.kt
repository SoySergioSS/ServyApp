package com.example.servyapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.servyapp.ui.plates.PlatesScreen
import com.example.servyapp.ui.home.HomeScreen
import com.example.servyapp.ui.home.HomeViewModel
import com.example.servyapp.ui.login.LoginScreen
import com.example.servyapp.ui.login.LoginViewModel
import com.example.servyapp.ui.platedetail.PlateDetailViewModel
import com.example.servyapp.ui.platedetail.PlateDetailScreen
import com.example.servyapp.ui.profile.ProfileScreen
import com.example.servyapp.ui.profile.ProfileViewModel
import com.example.servyapp.ui.signup.CardRegistrationScreen
import com.example.servyapp.ui.signup.SignupScreen
import com.example.servyapp.ui.signup.SignupViewModel
import com.example.servyapp.ui.splash.SplashScreen
import com.example.servyapp.ui.start.StartScreen

sealed class Screen(val route: String){
    object Splash: Screen("splash")
    object Start: Screen("start")
    object Signup: Screen("signup")
    object Login: Screen("login")
    object Home: Screen("home")
    object Card: Screen("card")
    object Plates: Screen("plates")
    object Profile: Screen("profile")
    // RUTA CORREGIDA: Ahora incluye los parámetros
    object PlateDetail: Screen("plateDetail/{restaurantId}/{dishId}") {
        // Función auxiliar para construir la ruta de navegación
        fun createRoute(restaurantId: String, dishId: String): String {
            return "plateDetail/$restaurantId/$dishId"
        }
    }
}

@Composable
fun AppNavigation(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
){
    val signupViewModel: SignupViewModel = hiltViewModel()

    NavHost(
        navController = navHostController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ){
        composable(route = Screen.Splash.route){
            SplashScreen(
                navigateToHome = {
                    navHostController.navigate(Screen.Home.route){
                        popUpTo(0){
                            inclusive = true
                        }
                    }
                },
                navigateToStart = {
                    navHostController.navigate(Screen.Start.route){
                        popUpTo(0){
                            inclusive = true
                        }
                    }
                },
                splashViewModel = hiltViewModel()
            )
        }

        composable(route = Screen.Start.route){
            StartScreen(
                loginButtonPressed = {
                    navHostController.navigate(Screen.Login.route)
                },
                signupButtonPressed = {
                    navHostController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(route = Screen.Signup.route){
            val state by signupViewModel.uiState.collectAsState()
            if(state.navigateToHome){
                navHostController.navigate(Screen.Home.route){
                    popUpTo(0){
                        inclusive = true
                    }
                }
            }
            SignupScreen(
                signupViewModel = signupViewModel,
                addCardButtonPressed = {
                    navHostController.navigate(Screen.Card.route)
                },
                loginButtonPressed = {
                    navHostController.navigate(Screen.Login.route)
                }
            )
        }

        composable(route = Screen.Login.route){
            val loginViewModel: LoginViewModel = hiltViewModel()
            val state by loginViewModel.uiState.collectAsState()
            if(state.navigateToHome){
                navHostController.navigate(Screen.Home.route){
                    popUpTo(0){
                        inclusive = true
                    }
                }
            }
            LoginScreen(
                loginViewModel = loginViewModel,
                signupButtonPressed = {
                    navHostController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(route = Screen.Home.route){
            val homeViewModel: HomeViewModel = hiltViewModel()
            val state by homeViewModel.uiState.collectAsState()
            LaunchedEffect(state.navigateToPlates) {
                if(state.navigateToPlates){
                    navHostController.navigate(Screen.Plates.route)
                    homeViewModel.navigationToPlatesComplete()
                }
            }

            HomeScreen(
                homeViewModel = hiltViewModel(),
                onProfileClick = {
                    navHostController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(route = Screen.Card.route){
            CardRegistrationScreen(
                signupViewModel = signupViewModel,
                onNavigateBack = {
                    navHostController.popBackStack()
                }
            )
        }

        composable(route = Screen.Plates.route){
            PlatesScreen(
                platesViewModel = hiltViewModel(),
                onBackClick = {
                    navHostController.popBackStack()
                },
                onCartClick = {
                    //navHostController.navigate(Screen.Cart.route)
                },
                onNavigateToDetail = { restaurantId, dishId ->
                    // Uso de la función auxiliar para construir la URL
                    navHostController.navigate(Screen.PlateDetail.createRoute(restaurantId, dishId))
                }
            )
        }

        composable(route = Screen.Profile.route){
            val profileViewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = profileViewModel,
                logoutButtonPressed = {
                    navHostController.navigate(Screen.Start.route){
                        popUpTo(0){
                            inclusive = true
                        }
                    }
                }
            )
        }

        // CONFIGURACIÓN CORREGIDA DEL DESTINO CON ARGUMENTOS
        composable(
            route = Screen.PlateDetail.route,
            arguments = listOf(
                navArgument("restaurantId") { type = NavType.StringType },
                navArgument("dishId") { type = NavType.StringType }
            )
        ){
            val plateDetailViewModel: PlateDetailViewModel = hiltViewModel()
            val state by plateDetailViewModel.uiState.collectAsState()
//            LaunchedEffect(state.navigateToCart) {
//                if(state.navigateToCart){
//                    navHostController.navigate(Screen.Cart.route)
//                    plateDetailViewModel.navigationToCartComplete()
//                }
//            }
            PlateDetailScreen(
                viewModel = plateDetailViewModel,
                onBackClick = {
                    navHostController.popBackStack()
                },
                onCartClick = {
                    //navHostController.navigate(Screen.Cart.route) no sé si se usará
                }
            )
        }
    }
}
