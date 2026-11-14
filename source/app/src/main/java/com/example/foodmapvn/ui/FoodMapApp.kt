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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
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
import com.example.foodmapvn.ui.business.BusinessRegisterRoute
import com.example.foodmapvn.ui.dish.DishDetailRoute
import com.example.foodmapvn.ui.explore.ExploreRoute
import com.example.foodmapvn.ui.map.MapRoute
import com.example.foodmapvn.ui.navigation.AppBottomBar
import com.example.foodmapvn.ui.navigation.MainTab
import com.example.foodmapvn.ui.navigation.Routes
import com.example.foodmapvn.ui.profile.MyReviewsRoute
import com.example.foodmapvn.ui.settings.SettingsRoute
import com.example.foodmapvn.ui.profile.EditProfileRoute
import com.example.foodmapvn.ui.profile.ProfileRoute
import com.example.foodmapvn.ui.region.RegionSelectRoute
import com.example.foodmapvn.ui.saved.SavedRoute
import com.example.foodmapvn.ui.search.SearchRoute
import com.example.foodmapvn.ui.navigation.navigateIfReady
import androidx.compose.ui.platform.LocalContext
import com.example.foodmapvn.ui.splash.SplashRoute
import com.example.foodmapvn.ui.onboarding.OnboardingScreen
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

import com.example.foodmapvn.ui.checkin.CheckInRoute
import com.example.foodmapvn.ui.place.PlaceDetailRoute
import com.example.foodmapvn.ui.place.PlaceReviewsRoute
import com.example.foodmapvn.ui.review.WriteReviewRoute

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
            // "Cổng doanh nghiệp" cho Đề xuất quán (UC-14): chỉ doanh nghiệp đã được duyệt mới mở được
            // màn Đề xuất (Người 3); chưa đăng ký / đang chờ duyệt -> mở màn Đăng ký doanh nghiệp.
            val canSuggestPlace by container.businessRepository.canSuggestPlace().collectAsState(initial = false)
            val openNewSuggestion: () -> Unit = {
                loginGate.requireLogin {
                    if (canSuggestPlace) navController.navigateIfReady(Routes.SUGGESTION_NEW, context, "Đề xuất quán")
                    else navController.navigate(Routes.BUSINESS_REGISTER)
                }
            }
            // Thống kê: mỗi lần chuyển màn gửi screen_view (tên = route, vd. "place/{placeId}")
            DisposableEffect(navController) {
                val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                    destination.route?.takeIf { it != Routes.MAIN }?.let(analytics::screen)
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose { navController.removeOnDestinationChangedListener(listener) }
            }
            // Luồng mở app lần đầu theo Figma: Splash -> Giới thiệu (+ hỏi quyền vị trí) -> Đăng nhập -> Chọn vùng -> app.
            // Khác null = đang trong luồng này (giá trị = người dùng có cho phép vị trí ở màn Giới thiệu không).
            val onboardingPrefs = remember { context.getSharedPreferences("onboarding", android.content.Context.MODE_PRIVATE) }
            var startupAsk by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val startupNext: () -> String = {
                if (container.areaRepository.selectedArea.value == null) Routes.regionSelect(startupAsk ?: true) else Routes.MAIN
            }
            // Xong màn Đăng nhập/Đăng ký (hoặc bấm "Dùng thử không cần tài khoản"):
            // đang trong luồng mở app -> sang bước tiếp; mở từ trong app -> quay về màn trước
            val finishAuth: () -> Unit = {
                if (startupAsk != null) {
                    val next = startupNext()
                    startupAsk = null
                    navController.navigate(next) { popUpTo(navController.graph.id) { inclusive = true } }
                } else {
                    navController.popBackStack(Routes.LOGIN, inclusive = true)
                }
            }
            CompositionLocalProvider(LocalLoginGate provides loginGate) {
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                // ===== Người 1: khung chung, Khám phá, Bản đồ, Hồ sơ =====
                composable(Routes.SPLASH) {
                    SplashRoute(onFinished = { next ->
                        // Chưa xem Giới thiệu (kể cả máy đã chọn khu vực từ bản cũ) -> hiện các bước như Figma
                        val target = if (!onboardingPrefs.getBoolean("done", false)) Routes.ONBOARDING else next
                        navController.navigate(target) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    })
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onFinished = { ask ->
                        onboardingPrefs.edit().putBoolean("done", true).apply()
                        startupAsk = ask
                        val next = if (currentUser == null) Routes.LOGIN else startupNext()
                        if (next != Routes.LOGIN) startupAsk = null
                        navController.navigate(next) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(
                    Routes.REGION_SELECT_PATTERN,
                    arguments = listOf(navArgument(Routes.ARG_ASK_LOCATION) {
                        type = NavType.BoolType
                        defaultValue = true
                    }),
                ) { entry ->
                    RegionSelectRoute(
                        askLocationOnStart = entry.arguments?.getBoolean(Routes.ARG_ASK_LOCATION) ?: true,
                        onDone = {
                            // Lần đầu (không có màn trước) -> vào app; mở từ Khám phá -> quay lại
                            if (navController.previousBackStackEntry == null) {
                                navController.navigate(Routes.MAIN) { popUpTo(Routes.REGION_SELECT_PATTERN) { inclusive = true } }
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
                        onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                        onMyReviewsClick = { navController.navigate(Routes.MY_REVIEWS) },
                        onBadgesClick = { navController.navigate(Routes.BADGES) },
                        // 3 màn của Người 3: chưa có thì báo nhẹ, không crash
                        onNotificationsClick = { navController.navigateIfReady(Routes.NOTIFICATIONS, context, "Thông báo") },
                        onMySuggestionsClick = { navController.navigateIfReady(Routes.SUGGESTION_MINE, context, "Đề xuất của tôi") },
                        onAdminClick = { navController.navigateIfReady(Routes.ADMIN, context, "Quản trị") },
                        onBusinessClick = { navController.navigate(Routes.BUSINESS_REGISTER) },
                        onNewSuggestionClick = openNewSuggestion,
                        onPlaceClick = { navController.navigate(Routes.place(it)) },
                        onDishClick = { navController.navigate(Routes.dish(it.id)) },
                        onDishIdClick = { navController.navigate(Routes.dish(it)) },
                    )
                }
                // Đăng nhập xong (mọi cách) -> đóng các màn đăng nhập, quay về màn trước khi mở Đăng nhập
                composable(Routes.LOGIN) {
                    LoginRoute(
                        onDone = finishAuth,
                        onPhoneClick = { navController.navigate(Routes.LOGIN_PHONE) },
                        onRegisterClick = { navController.navigate(Routes.REGISTER) },
                        onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterRoute(
                        onDone = finishAuth,
                        onBackToLogin = { navController.popBackStack() },
                    )
                }
                composable(Routes.FORGOT_PASSWORD) {
                    ForgotPasswordRoute(onBack = { navController.popBackStack() })
                }
                composable(Routes.LOGIN_PHONE) {
                    PhoneLoginRoute(
                        onDone = finishAuth,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.SEARCH) {
                    SearchRoute(
                        onBack = { navController.popBackStack() },
                        onDishClick = { navController.navigate(Routes.dish(it.id)) },
                        onPlaceClick = { navController.navigate(Routes.place(it.id)) },
                    )
                }
                composable(Routes.PROFILE_EDIT) {
                    EditProfileRoute(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.BADGES) {
                    com.example.foodmapvn.ui.profile.BadgesRoute(
                        onBack = { navController.popBackStack() },
                        onPlaceClick = { navController.navigate(Routes.place(it)) },
                    )
                }
                composable(Routes.MY_REVIEWS) {
                    MyReviewsRoute(
                        onBack = { navController.popBackStack() },
                        onPlaceClick = { navController.navigateIfReady(Routes.place(it), context, "Chi tiết quán") },
                    )
                }
                composable(Routes.BUSINESS_REGISTER) {
                    BusinessRegisterRoute(
                        onBack = { navController.popBackStack() },
                        onNewSuggestionClick = openNewSuggestion,
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsRoute(
                        onBack = { navController.popBackStack() },
                        onRegionClick = { navController.navigate(Routes.REGION_SELECT) },
                    )
                }

                // ===== Người 2: món, quán, check-in, đánh giá =====
                // UC-08 Chi tiết đặc sản + UC-10 Lưu vào Món muốn ăn
                composable(
                    route = Routes.DISH,
                    arguments = listOf(
                        navArgument(Routes.ARG_DISH_ID) { type = NavType.StringType },
                        navArgument(Routes.ARG_FROM_PLACE_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) { entry ->
                    DishDetailRoute(
                        onBack = { navController.popBackStack() },
                        onPlaceClick = { navController.navigate(Routes.place(it)) },
                        fromPlaceId = entry.arguments?.getString(Routes.ARG_FROM_PLACE_ID),
                        // "Xem quán trên bản đồ": sang tab Bản đồ và mở sẵn thẻ của quán đó
                        onShowOnMap = { placeId ->
                            if (placeId != null) container.navigationRouteManager.showPlaceOnMap(placeId)
                            else container.navigationRouteManager.requestMapTab()
                            navController.popBackStack(Routes.MAIN, inclusive = false)
                        },
                    )
                }
                // UC-09 Chi tiết quán, danh sách đánh giá; UC-12 Check-in; UC-13 Viết đánh giá; chỉ đường
                composable(Routes.PLACE) { entry ->
                    val placeId = entry.arguments?.getString(Routes.ARG_PLACE_ID)
                    PlaceDetailRoute(
                        onBack = { navController.popBackStack() },
                        // "Món của quán" -> Chi tiết đặc sản (nhớ quán đang xem để "Xem quán trên bản đồ" chọn đúng quán)
                        onDishClick = { dishId -> navController.navigate(Routes.dish(dishId, placeId)) },
                        onCheckInClick = { placeId -> navController.navigate(Routes.checkin(placeId)) },
                        onWriteReviewClick = { placeId -> navController.navigate(Routes.review(placeId)) },
                        onAllReviewsClick = { placeId -> navController.navigate(Routes.placeReviews(placeId)) },
                        onDirectionsClick = { place ->
                            container.navigationRouteManager.startNavigation(
                                placeId = place.id,
                                placeName = place.name,
                                address = place.address,
                                location = place.location,
                            )
                            navController.popBackStack(Routes.MAIN, inclusive = false)
                        },
                    )
                }
                composable(Routes.PLACE_REVIEWS) {
                    PlaceReviewsRoute(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.CHECKIN) {
                    CheckInRoute(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.REVIEW) {
                    WriteReviewRoute(
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

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
    onSettingsClick: () -> Unit,
    onMyReviewsClick: () -> Unit,
    onBadgesClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onMySuggestionsClick: () -> Unit,
    onAdminClick: () -> Unit,
    onBusinessClick: () -> Unit,
    onNewSuggestionClick: () -> Unit,
    onPlaceClick: (placeId: String) -> Unit,
    onDishClick: (Dish) -> Unit,
    onDishIdClick: (dishId: String) -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.EXPLORE) }
    val context = LocalContext.current
    val container = (context.applicationContext as FoodMapApplication).container
    val analytics = container.analytics

    val requestedTabSwitch by container.navigationRouteManager.requestedTabSwitch.collectAsState()
    LaunchedEffect(requestedTabSwitch) {
        if (requestedTabSwitch) {
            currentTab = MainTab.MAP
            container.navigationRouteManager.consumeTabSwitch()
        }
    }

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
                    onPlaceClick = onPlaceClick,
                )
                MainTab.MAP -> MapRoute(onPlaceDetail = onPlaceClick)
                MainTab.SAVED -> SavedRoute(
                    onDishClick = onDishIdClick,
                    onPlaceClick = onPlaceClick,
                )
                MainTab.PROFILE -> ProfileRoute(
                    onLoginClick = onLoginClick,
                    onEditProfileClick = onEditProfileClick,
                    onNotificationsClick = onNotificationsClick,
                    onSettingsClick = onSettingsClick,
                    onSeeAllReviewsClick = onMyReviewsClick,
                    onBadgesClick = onBadgesClick,
                    onReviewPlaceClick = onPlaceClick,
                    // "Món muốn ăn" = danh sách món đã lưu -> chuyển sang tab Đã lưu
                    onWishlistClick = { currentTab = MainTab.SAVED },
                    onMySuggestionsClick = onMySuggestionsClick,
                    onAdminClick = onAdminClick,
                    onBusinessClick = onBusinessClick,
                    onNewSuggestionClick = onNewSuggestionClick,
                )
            }
        }
    }
}
