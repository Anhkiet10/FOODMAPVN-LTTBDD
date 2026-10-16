package com.example.foodmapvn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodmapvn.ui.auth.ForgotPasswordRoute
import com.example.foodmapvn.ui.auth.LoginRoute
import com.example.foodmapvn.ui.auth.RegisterRoute
import com.example.foodmapvn.ui.explore.ExploreRoute
import com.example.foodmapvn.ui.map.MapRoute
import com.example.foodmapvn.ui.navigation.AppBottomBar
import com.example.foodmapvn.ui.navigation.MainTab
import com.example.foodmapvn.ui.navigation.Routes
import com.example.foodmapvn.ui.profile.ProfileRoute
import com.example.foodmapvn.ui.saved.SavedRoute
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

/**
 * Giao diện gốc của app: theme + điều hướng.
 * NavHost quản lý các màn toàn màn hình (đăng nhập, sau này: chi tiết quán, tìm kiếm...);
 * 4 tab chính nằm trong màn MAIN.
 */
@Composable
fun FoodMapApp() {
    FoodmapvnTheme {
        Surface(Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    MainTabs(onLoginClick = { navController.navigate(Routes.LOGIN) })
                }
                composable(Routes.LOGIN) {
                    LoginRoute(
                        onDone = { navController.popBackStack(Routes.MAIN, inclusive = false) },
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterRoute(
                        onDone = { navController.popBackStack(Routes.MAIN, inclusive = false) },
                        onBackToLogin = { navController.popBackStack() },
                    )
                }
                composable(Routes.FORGOT_PASSWORD) {
                    ForgotPasswordRoute(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun MainTabs(onLoginClick: () -> Unit) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.EXPLORE) }

    Scaffold(
        bottomBar = { AppBottomBar(selected = currentTab, onSelect = { currentTab = it }) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.EXPLORE -> ExploreRoute()
                MainTab.MAP -> MapRoute()
                MainTab.SAVED -> SavedRoute()
                MainTab.PROFILE -> ProfileRoute(onLoginClick = onLoginClick)
            }
        }
    }
}
