package com.onfu.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.onfu.app.data.auth.AuthRepository
import com.onfu.app.ui.auth.LoginScreen
import com.onfu.app.ui.auth.RegisterScreen
import com.onfu.app.ui.home.HomeScreen
import com.onfu.app.ui.post.UploadScreen
import com.onfu.app.ui.profile.ProfileScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository(FirebaseAuth.getInstance()) }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {

        // LOGIN SCREEN
        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                onNavigateHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigatePreHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        // REGISTER SCREEN
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterCompleted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // HOME SCREEN
        composable(Routes.HOME) {
            HomeScreen()
        }

        // PROFILE SCREEN
        composable(Routes.PROFILE) {
            ProfileScreen()
        }

        // UPLOAD SCREEN
        composable(Routes.UPLOAD) {
            UploadScreen()
        }
    }
}
