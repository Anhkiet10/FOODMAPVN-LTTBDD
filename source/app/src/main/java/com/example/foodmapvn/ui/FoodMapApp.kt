package com.example.foodmapvn.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.example.foodmapvn.FoodMapApplication
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.ui.auth.ForgotPasswordRoute
import com.example.foodmapvn.ui.auth.LocalLoginGate
import com.example.foodmapvn.ui.auth.LoginGate
import com.example.foodmapvn.ui.auth.LoginRoute
import com.example.foodmapvn.ui.auth.PhoneLoginRoute
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.example.foodmapvn.ui.auth.RegisterRoute
import com.example.foodmapvn.ui.explore.ExploreRoute
import com.example.foodmapvn.ui.map.MapRoute
import com.example.foodmapvn.ui.navigation.AppBottomBar
import com.example.foodmapvn.ui.navigation.MainTab
import com.example.foodmapvn.ui.navigation.Routes
import com.example.foodmapvn.ui.profile.EditProfileRoute
import com.example.foodmapvn.ui.profile.ProfileRoute
import com.example.foodmapvn.ui.region.RegionSelectRoute
import com.example.foodmapvn.ui.saved.SavedRoute
import com.example.foodmapvn.ui.search.SearchRoute
import com.example.foodmapvn.ui.navigation.navigateIfReady
import androidx.compose.ui.platform.LocalContext
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
            val context = LocalContext.current
            val container = (context.applicationContext as FoodMapApplication).container
            val analytics = container.analytics
            // Cổng đăng nhập dùng chung (LoginGate.kt): khách bấm việc cần tài khoản -> mở màn Đăng nhập
            val currentUser by container.authRepository.currentUser.collectAsState(initial = null)
            // Mỗi lần có người đăng nhập: bảo đảm có hồ sơ users/{uid}
            // (tài khoản tạo lúc Rules cũ từ chối sẽ được tạo bù ở đây)
            LaunchedEffect(currentUser?.uid) {
                if (currentUser != null) container.authRepository.ensureProfile()
            }
            val loginGate = remember(navController) {
                LoginGate(isLoggedIn = { currentUser != null }, openLogin = { navController.navigate(Routes.LOGIN) })
            }
            // Thống kê: mỗi lần chuyển màn gửi screen_view (tên = route, vd. "place/{placeId}")
            DisposableEffect(navController) {
                val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                    destination.route?.takeIf { it != Routes.MAIN }?.let(analytics::screen)
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose { navController.removeOnDestinationChangedListener(listener) }
            }
            CompositionLocalProvider(LocalLoginGate provides loginGate) {
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
                        onSearchClick = { navController.navigate(Routes.SEARCH) },
                        onEditProfileClick = { navController.navigate(Routes.PROFILE_EDIT) },
                        // Màn Chi tiết đặc sản là của Người 2: chưa có thì báo nhẹ, không crash
                        onDishClick = { navController.navigateIfReady(Routes.dish(it.id), context, "Chi tiết đặc sản") },
                    )
                }
                // Đăng nhập xong (mọi cách) -> đóng các màn đăng nhập, quay về màn trước khi mở Đăng nhập
                composable(Routes.LOGIN) {
                    LoginRoute(
                        onDone = { navController.popBackStack(Routes.LOGIN, inclusive = true) },
                        onPhoneClick = { navController.navigate(Routes.LOGIN_PHONE) },
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterRoute(
                        onDone = { navController.popBackStack(Routes.LOGIN, inclusive = true) },
                        onBackToLogin = { navController.popBackStack() },
                    )
                }
                composable(Routes.FORGOT_PASSWORD) {
                    ForgotPasswordRoute(onBack = { navController.popBackStack() })
                }
                composable(Routes.LOGIN_PHONE) {
                    PhoneLoginRoute(
                        onDone = { navController.popBackStack(Routes.LOGIN, inclusive = true) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchRoute(
                        onBack = { navController.popBackStack() },
                        onDishClick = { navController.navigateIfReady(Routes.dish(it.id), context, "Chi tiết đặc sản") },
                        onPlaceClick = { navController.navigateIfReady(Routes.place(it.id), context, "Chi tiết quán") },
                    )
                }
                composable(Routes.PROFILE_EDIT) {
                    EditProfileRoute(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                // ===== Người 2: món, quán, check-in, đánh giá =====
                // (thêm: Routes.DISH, Routes.PLACE, Routes.PLACE_REVIEWS, Routes.CHECKIN, Routes.REVIEW)

                // ===== Người 3: đề xuất, báo cáo, thông báo, admin =====
                // Nhật - Tuần 1: Đề xuất quán mới
                composable(Routes.SUGGESTION_NEW) {
                    com.example.foodmapvn.ui.suggestion.SuggestionNewRoute(
                        onBack = { navController.popBackStack() },
                        onFinished = { navController.popBackStack() },
                    )
                }
                // thêm ( Routes.SUGGESTION_MINE, Routes.REPORT, Routes.NOTIFICATIONS)

            }
            }
        }
    }
}

@Composable
private fun MainTabs(
    onLoginClick: () -> Unit,
    onRegionClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSuggestPlaceClick: () -> Unit = {},
    onDishClick: (Dish) -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.EXPLORE) }
    val analytics = (LocalContext.current.applicationContext as FoodMapApplication).container.analytics
    LaunchedEffect(currentTab) { analytics.screen("tab_" + currentTab.name.lowercase()) }

    Scaffold(
        bottomBar = { AppBottomBar(selected = currentTab, onSelect = { currentTab = it }) },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.EXPLORE -> ExploreRoute(
                    onRegionClick = onRegionClick,
                    onSearchClick = onSearchClick,
                    onDishClick = onDishClick,
                )
                MainTab.MAP -> MapRoute()
                MainTab.SAVED -> SavedRoute()
                MainTab.PROFILE -> ProfileRoute(
                    onLoginClick = onLoginClick,
                    onEditProfileClick = onEditProfileClick,
                    onSuggestPlaceClick = onSuggestPlaceClick,
                )
            }
        }
    }
}
