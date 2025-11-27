package com.onfu.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onfu.app.ui.auth.LoginScreen
import com.onfu.app.ui.auth.RegisterScreen
import com.onfu.app.ui.home.HomeScreen
import com.onfu.app.ui.post.UploadScreen
import com.onfu.app.ui.profile.ProfileScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginCompleted = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }, onNavigateToRegister = {
                navController.navigate(Routes.REGISTER)
            })
        }
        composable(Routes.REGISTER) {
            RegisterScreen(onRegisterCompleted = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.REGISTER) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.PROFILE) { ProfileScreen() }
        composable(Routes.UPLOAD) { UploadScreen() }
    }
}
