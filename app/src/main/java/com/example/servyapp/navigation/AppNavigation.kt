package com.example.servyapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.servyapp.ui.home.HomeScreen
import com.example.servyapp.ui.login.LoginScreen
import com.example.servyapp.ui.login.LoginViewModel
import com.example.servyapp.ui.signup.CardRegistrationScreen
import com.example.servyapp.ui.signup.SignupScreen
import com.example.servyapp.ui.signup.SignupViewModel
import com.example.servyapp.ui.start.StartScreen

sealed class Screen(val route: String){
    object Start: Screen("start")
    object Signup: Screen("signup")
    object Login: Screen("login")
    object Home: Screen("home")
    object Card: Screen("card")
}

@Composable
fun AppNavigation(
    navHostController: NavHostController,
    modifier: Modifier = Modifier
){
    val signupViewModel: SignupViewModel = hiltViewModel()

    NavHost(
        navController = navHostController,
        startDestination = Screen.Start.route,
        modifier = modifier
    ){
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
                forgotPasswordButtonPressed = {
                /*TODO*/
                },
                signupButtonPressed = {
                    navHostController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(route = Screen.Home.route){
            HomeScreen()
        }

        composable(route = Screen.Card.route){
            CardRegistrationScreen(
                signupViewModel = signupViewModel,
                onNavigateBack = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}