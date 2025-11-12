package com.example.servyapp.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.servyapp.ui.cart.CartScreen
import com.example.servyapp.ui.cart.CartViewModel
import com.example.servyapp.ui.components.BottomNavigationBar
import com.example.servyapp.ui.dishes.DishesScreen
import com.example.servyapp.ui.home.HomeScreen
import com.example.servyapp.ui.home.HomeViewModel
import com.example.servyapp.ui.login.LoginScreen
import com.example.servyapp.ui.login.LoginViewModel
import com.example.servyapp.ui.orders.OrdersScreen
import com.example.servyapp.ui.dishdetail.DishDetailViewModel
import com.example.servyapp.ui.dishdetail.DishDetailScreen
import com.example.servyapp.ui.orderdetail.OrderDetailScreen
import com.example.servyapp.ui.orderdetail.OrderDetailViewModel
import com.example.servyapp.ui.profile.ProfileScreen
import com.example.servyapp.ui.profile.ProfileViewModel
import com.example.servyapp.ui.signup.CardRegistrationScreen
import com.example.servyapp.ui.signup.SignupScreen
import com.example.servyapp.ui.signup.SignupViewModel
import com.example.servyapp.ui.splash.SplashScreen
import com.example.servyapp.ui.start.StartScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Start : Screen("start")
    object Signup : Screen("signup")
    object Login : Screen("login")
    object Home : Screen("home")
    object Card : Screen("card")
    object Dishes : Screen("dishes")
    object Profile : Screen("profile")
    object DishDetail : Screen("dishDetail/{restaurantId}/{dishId}") {
        fun createRoute(restaurantId: String, dishId: String): String {
            return "dishDetail/$restaurantId/$dishId"
        }
    }

    object Cart : Screen("cart")
    object Orders : Screen("orders")

    object OrderDetail : Screen("orderDetail/{orderId}") {
        fun createRoute(orderId: String): String {
            return "orderDetail/$orderId"
        }
    }

    object VirtualWaiter : Screen("virtual_waiter")
    object Stats : Screen("stats")
}

@Composable
fun AppNavigation(
    navHostController: NavHostController, modifier: Modifier = Modifier
) {
    val signupViewModel: SignupViewModel = hiltViewModel()
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determinar si mostrar la barra de navegación inferior
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Orders.route,
        Screen.Profile.route,
        Screen.VirtualWaiter.route,
        Screen.Stats.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute, onNavigate = { route ->
                        navHostController.navigate(route) {
                            // Evitar múltiples copias de la misma ruta en el back stack
                            popUpTo(navHostController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }
        }) { innerPadding ->
        NavHost(
            navController = navHostController,
            startDestination = Screen.Splash.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = Screen.Splash.route) {
                SplashScreen(
                    navigateToHome = {
                        navHostController.navigate(Screen.Home.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }, navigateToStart = {
                        navHostController.navigate(Screen.Start.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }, splashViewModel = hiltViewModel()
                )
            }

            composable(route = Screen.Start.route) {
                StartScreen(loginButtonPressed = {
                    navHostController.navigate(Screen.Login.route)
                }, signupButtonPressed = {
                    navHostController.navigate(Screen.Signup.route)
                })
            }

            composable(route = Screen.Signup.route) {
                val state by signupViewModel.uiState.collectAsState()
                if (state.navigateToHome) {
                    navHostController.navigate(Screen.Home.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
                SignupScreen(signupViewModel = signupViewModel, addCardButtonPressed = {
                    navHostController.navigate(Screen.Card.route)
                }, loginButtonPressed = {
                    navHostController.navigate(Screen.Login.route)
                })
            }

            composable(route = Screen.Login.route) {
                val loginViewModel: LoginViewModel = hiltViewModel()
                val state by loginViewModel.uiState.collectAsState()
                if (state.navigateToHome) {
                    navHostController.navigate(Screen.Home.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
                LoginScreen(
                    loginViewModel = loginViewModel, signupButtonPressed = {
                        navHostController.navigate(Screen.Signup.route)
                    })
            }

            composable(route = Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val state by homeViewModel.uiState.collectAsState()
                LaunchedEffect(state.navigateToDishes) {
                    if (state.navigateToDishes) {
                        navHostController.navigate(Screen.Dishes.route)
                        homeViewModel.navigationToDishesComplete()
                    }
                }

                HomeScreen(
                    homeViewModel = hiltViewModel(), onProfileClick = {
                        navHostController.navigate(Screen.Profile.route)
                    })
            }

            composable(route = Screen.Card.route) {
                CardRegistrationScreen(
                    signupViewModel = signupViewModel, onNavigateBack = {
                        navHostController.popBackStack()
                    })
            }

            composable(route = Screen.Dishes.route) {
                DishesScreen(dishesViewModel = hiltViewModel(), onBackClick = {
                    navHostController.popBackStack()
                }, onCartClick = {
                    navHostController.navigate(Screen.Cart.route)
                }, onNavigateToDetail = { restaurantId, dishId ->
                    navHostController.navigate(
                        Screen.DishDetail.createRoute(
                            restaurantId, dishId
                        )
                    )
                })
            }

            composable(route = Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel, logoutButtonPressed = {
                        navHostController.navigate(Screen.Start.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    })
            }

            composable(
                route = Screen.DishDetail.route,
                arguments = listOf(
                    navArgument("restaurantId") { type = NavType.StringType },
                    navArgument("dishId") { type = NavType.StringType })) {
                val dishDetailViewModel: DishDetailViewModel = hiltViewModel()
                val state by dishDetailViewModel.uiState.collectAsState()
//            LaunchedEffect(state.navigateToCart) {
//                if(state.navigateToCart){
//                    navHostController.navigate(Screen.Cart.route)
//                    dishDetailViewModel.navigationToCartComplete()
//                }
//            }
                DishDetailScreen(viewModel = dishDetailViewModel, onBackClick = {
                    navHostController.popBackStack()
                }, onCartClick = {
                    navHostController.navigate(Screen.Cart.route)
                })
            }

            composable(route = Screen.Cart.route) {
                val cartViewModel: CartViewModel = hiltViewModel()
                CartScreen(viewModel = cartViewModel, onBackClick = {
                    // Si hay una ruta anterior, volver; si no, ir a Home
                    if (!navHostController.popBackStack()) {
                        navHostController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }, onNavigateToOrders = {
                    navHostController.navigate(Screen.Orders.route)
                }, onNavigateToDishDetail = { restaurantId, dishId ->
                    navHostController.navigate(
                        Screen.DishDetail.createRoute(
                            restaurantId, dishId
                        )
                    )
                })
            }

            composable(route = Screen.Orders.route) {
                OrdersScreen(viewModel = hiltViewModel(), onBackClick = {
                    // Si hay una ruta anterior, volver; si no, ir a Home
                    if (!navHostController.popBackStack()) {
                        navHostController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }, onNavigateToOrderDetail = { orderId ->
                    navHostController.navigate(Screen.OrderDetail.createRoute(orderId))
                })
            }

            composable(
                route = Screen.OrderDetail.route, arguments = listOf(
                navArgument("orderId") { type = NavType.StringType })) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                val orderDetailViewModel: OrderDetailViewModel = hiltViewModel()

                OrderDetailScreen(
                    orderId = orderId,
                    viewModel = orderDetailViewModel,
                    onBackClick = { navHostController.popBackStack() },
                    onNavigateToCard = {
                        navHostController.navigate(Screen.Card.route)
                    })
            }

            composable(route = Screen.VirtualWaiter.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Hola")
                }
            }

            composable(route = Screen.Stats.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Pantalla de Estadísticas") //por ahora
                }
            }

        }
    }
}
