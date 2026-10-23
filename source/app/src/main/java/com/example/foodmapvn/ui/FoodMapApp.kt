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
import com.example.foodmapvn.ui.region.RegionSelectRoute
import com.example.foodmapvn.ui.saved.SavedRoute
import com.example.foodmapvn.ui.splash.SplashRoute
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

/**
 * Giao diện gốc của app: theme + điều hướng.
 * NavHost quản lý các màn toàn màn hình; 4 tab chính nằm trong màn MAIN.
 * Luồng mở app: Splash -> (lần đầu) Chọn vùng, xin quyền vị trí -> MAIN.
 *
 * QUY ƯỚC NHÓM: NavHost chia 3 vùng "Người 1 / Người 2 / Người 3".
 * Mỗi người chỉ THÊM composable(...) cho route của mình vào vùng của mình (tên route: Routes.kt).
 */
@Composable
fun FoodMapApp() {
    FoodmapvnTheme {
        Surface(Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                // ===== Người 1: khung chung, Khám phá, Bản đồ, Hồ sơ =====
                composable(Routes.SPLASH) {
                    SplashRoute(onFinished = { next ->
                        navController.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    })
                }
                composable(Routes.REGION_SELECT) {
                    RegionSelectRoute(
                        onDone = {
                            // Lần đầu (không có màn trước) -> vào app; mở từ Khám phá -> quay lại
                            if (navController.previousBackStackEntry == null) {
                                navController.navigate(Routes.MAIN) { popUpTo(Routes.REGION_SELECT) { inclusive = true } }
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.MAIN) {
                    MainTabs(
                        onLoginClick = { navController.navigate(Routes.LOGIN) },
                        onRegionClick = { navController.navigate(Routes.REGION_SELECT) },
                    )
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
                // (Người 1 thêm: Routes.SEARCH, Routes.PROFILE_EDIT)

                // ===== Người 2: món, quán, check-in, đánh giá =====
                // (thêm: Routes.DISH, Routes.PLACE, Routes.PLACE_REVIEWS, Routes.CHECKIN, Routes.REVIEW)

                // ===== Người 3: đề xuất, báo cáo, thông báo, admin =====
                // (thêm: Routes.SUGGESTION_NEW, Routes.SUGGESTION_MINE, Routes.REPORT, Routes.NOTIFICATIONS, Routes.ADMIN)
            }
        }
    }
}

@Composable
private fun MainTabs(onLoginClick: () -> Unit, onRegionClick: () -> Unit) {
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
                MainTab.EXPLORE -> ExploreRoute(onRegionClick = onRegionClick)
                MainTab.MAP -> MapRoute()
                MainTab.SAVED -> SavedRoute()
                MainTab.PROFILE -> ProfileRoute(onLoginClick = onLoginClick)
            }
        }
    }
}
