package com.example.foodmapvn.di

import android.content.Context
import android.util.Log
import com.example.foodmapvn.R
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.cloudinary.CloudinaryImageUploader
import com.example.foodmapvn.data.fake.FakeAdminRepository
import com.example.foodmapvn.data.fake.FakeNotificationRepository
import com.example.foodmapvn.data.fake.FakePlaceRepository
import com.example.foodmapvn.data.fake.FakeReportRepository
import com.example.foodmapvn.data.fake.FakeRepositories
import com.example.foodmapvn.data.fake.FakeSuggestionRepository
import com.example.foodmapvn.data.firebase.FirebaseAuthRepository
import com.example.foodmapvn.data.firebase.FirestoreAdminRepository
import com.example.foodmapvn.data.firebase.FirestoreBusinessRepository
import com.example.foodmapvn.data.firebase.FirestoreDishDetailRepository
import com.example.foodmapvn.data.firebase.FirestoreDishRepository
import com.example.foodmapvn.data.firebase.FirestoreNotificationRepository
import com.example.foodmapvn.data.firebase.FirestorePlaceRepository
import com.example.foodmapvn.data.firebase.FirestoreReportRepository
import com.example.foodmapvn.data.firebase.FirestoreSavedRepository
import com.example.foodmapvn.data.firebase.FirestoreSeeder
import com.example.foodmapvn.data.firebase.FirestoreSuggestionRepository
import com.example.foodmapvn.data.firebase.FirestoreUserRepository
import com.example.foodmapvn.data.local.ImageCompressor
import com.example.foodmapvn.data.local.PrefsAreaRepository
import com.example.foodmapvn.data.local.PrefsSearchHistoryRepository
import com.example.foodmapvn.data.location.FusedLocationRepository
import com.example.foodmapvn.data.repository.AdminRepository
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.BusinessRepository
import com.example.foodmapvn.data.repository.DishDetailRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.ImageUploader
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.data.repository.NotificationRepository
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.data.repository.ReportRepository
import com.example.foodmapvn.data.repository.SavedRepository
import com.example.foodmapvn.data.repository.SearchHistoryRepository
import com.example.foodmapvn.data.repository.SuggestionRepository
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.domain.BadgeCalculator
import com.example.foodmapvn.domain.BusinessRules
import com.example.foodmapvn.domain.CheckInValidator
import com.example.foodmapvn.domain.PriceRangeCalculator
import com.example.foodmapvn.domain.TrustScoreCalculator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Nơi duy nhất tạo các phụ thuộc dùng chung cho cả app (dependency injection thủ công).
 * - Có app/google-services.json -> dùng Firebase (Auth + Firestore).
 * - Chưa có                     -> dùng dữ liệu giả, app vẫn chạy để làm giao diện.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val isFirebaseEnabled: Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    /** Google Analytics for Firebase (dùng chung cả nhóm; chưa có Firebase thì không làm gì). */
    val analytics = AppAnalytics(appContext, enabled = isFirebaseEnabled)

    // Quy tắc nghiệp vụ (domain)
    val businessRules = BusinessRules()
    val checkInValidator = CheckInValidator(businessRules)
    val trustScoreCalculator = TrustScoreCalculator(businessRules)
    val priceRangeCalculator = PriceRangeCalculator(businessRules)
    val badgeCalculator = BadgeCalculator(businessRules)

    // ===== Người 1: khung chung, Khám phá, Bản đồ, Hồ sơ =====
    /** Khu vực (tỉnh/thành) đang chọn, dùng chung cho cả app; lưu lại khi mở lại app. */
    val areaRepository: AreaRepository = PrefsAreaRepository(appContext)
    /** Lịch sử tìm kiếm gần đây (màn Tìm kiếm). */
    val searchHistoryRepository: SearchHistoryRepository = PrefsSearchHistoryRepository(appContext)
    /** Thu nhỏ ảnh trước khi tải lên Cloudinary (ảnh đại diện…). */
    val imageCompressor = ImageCompressor(appContext)
    val authRepository: AuthRepository
    val dishRepository: DishRepository
    val userRepository: UserRepository
    /** Đăng ký doanh nghiệp; Người 3 dùng canSuggestPlace() (màn Đề xuất) và review() (Admin). */
    val businessRepository: BusinessRepository

    // ===== Người 2: món, quán, check-in, đánh giá =====
    val savedRepository: SavedRepository
    /** Chi tiết đặc sản + quán bán món (UC-08). */
    val dishDetailRepository: DishDetailRepository
    /** Chi tiết quán, đánh giá, check-in (UC-09, UC-12, UC-13). */
    val placeRepository: PlaceRepository

    // ===== Người 3: đề xuất, báo cáo, thông báo, admin =====
    // Nhật - Tuần 1: Đề xuất quán (UC-14)
    val suggestionRepository: SuggestionRepository
    /** Báo cáo vi phạm (đánh giá / quán) - UC-15. */
    val reportRepository: ReportRepository
    /** Thông báo người dùng - UC-16. */
    val notificationRepository: NotificationRepository
    /** Quản trị Admin (Đặc sản & vùng, Người dùng) - UC-18, UC-19. */
    val adminRepository: AdminRepository

    /** Nạp dữ liệu mẫu (chỉ bản debug, chỉ khi đã nối Firebase); null nếu không dùng được. */
    val seeder: FirestoreSeeder?

    val locationRepository: LocationRepository = FusedLocationRepository(appContext)
    val navigationRouteManager = com.example.foodmapvn.data.location.NavigationRouteManager()

    val imageUploader: ImageUploader = CloudinaryImageUploader(
        context = appContext,
        cloudName = appContext.getString(R.string.cloudinary_cloud_name),
        uploadPreset = appContext.getString(R.string.cloudinary_upload_preset),
    )

    init {
        if (isFirebaseEnabled) {
            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()
            // Người 1
            authRepository = FirebaseAuthRepository(auth, db)
            dishRepository = FirestoreDishRepository(db, areaRepository)
            userRepository = FirestoreUserRepository(db, auth, badgeCalculator)
            businessRepository = FirestoreBusinessRepository(db, auth)
            seeder = FirestoreSeeder(appContext, db, auth).takeIf { it.isAvailable }
            // Người 2
            savedRepository = FirestoreSavedRepository(db, auth)
            dishDetailRepository = FirestoreDishDetailRepository(db)
            placeRepository = FirestorePlaceRepository(db, auth, checkInValidator, trustScoreCalculator)
            // Người 3 – Nhật (khi đã có google-services.json)
            suggestionRepository = FirestoreSuggestionRepository(db, auth)
            reportRepository = FirestoreReportRepository(db, auth)
            notificationRepository = FirestoreNotificationRepository(db, auth)
            adminRepository = FirestoreAdminRepository(db, auth)
        } else {
            Log.i("FoodMap", "Chưa có google-services.json: đang dùng dữ liệu giả")
            val fake = FakeRepositories(areaRepository)
            // Người 1
            authRepository = fake.authRepository
            dishRepository = fake.dishRepository
            userRepository = fake.userRepository
            businessRepository = fake.businessRepository
            seeder = null
            // Người 2
            savedRepository = fake.savedRepository
            dishDetailRepository = fake.dishDetailRepository
            placeRepository = FakePlaceRepository(checkInValidator, trustScoreCalculator)
            // Người 3 – Nhật (khi chạy bằng dữ liệu giả không cần mạng/Firebase)
            suggestionRepository = FakeSuggestionRepository()
            reportRepository = FakeReportRepository()
            notificationRepository = FakeNotificationRepository()
            adminRepository = FakeAdminRepository()
        }
    }
}