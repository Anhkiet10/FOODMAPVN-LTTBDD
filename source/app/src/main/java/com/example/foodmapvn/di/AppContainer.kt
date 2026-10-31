package com.example.foodmapvn.di

import android.content.Context
import android.util.Log
import com.example.foodmapvn.R
import com.example.foodmapvn.data.cloudinary.CloudinaryImageUploader
import com.example.foodmapvn.data.fake.FakeRepositories
import com.example.foodmapvn.data.firebase.FirebaseAuthRepository
import com.example.foodmapvn.data.firebase.FirestoreDishRepository
import com.example.foodmapvn.data.firebase.FirestoreSavedRepository
import com.example.foodmapvn.data.firebase.FirestoreSeeder
import com.example.foodmapvn.data.firebase.FirestoreUserRepository
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.local.ImageCompressor
import com.example.foodmapvn.data.local.PrefsAreaRepository
import com.example.foodmapvn.data.local.PrefsSearchHistoryRepository
import com.example.foodmapvn.data.repository.SearchHistoryRepository
import com.example.foodmapvn.data.location.FusedLocationRepository
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.ImageUploader
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.data.repository.SavedRepository
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.domain.BadgeCalculator
import com.example.foodmapvn.domain.BusinessRules
import com.example.foodmapvn.domain.CheckInValidator
import com.example.foodmapvn.domain.PriceRangeCalculator
import com.example.foodmapvn.domain.TrustScoreCalculator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.foodmapvn.data.repository.SuggestionRepository // 3 thêm nhat
import com.example.foodmapvn.data.fake.FakeSuggestionRepository
import com.example.foodmapvn.data.firebase.FirestoreSuggestionRepository

/**
 * Nơi duy nhất tạo các phụ thuộc dùng chung cho cả app (dependency injection thủ công).
 *
 * - Có app/google-services.json  -> dùng Firebase (Auth + Firestore).
 * - Chưa có                       -> dùng dữ liệu giả, app vẫn chạy để làm giao diện.
 *
 * QUY ƯỚC NHÓM: file chia 3 vùng "Người 1 / Người 2 / Người 3".
 * Mỗi người chỉ THÊM dòng vào vùng của mình (khai báo + khởi tạo repository),
 * không sửa dòng của người khác -> Git tự ghép được, không xung đột.
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

    // ===== Người 2: món, quán, check-in, đánh giá =====
    val savedRepository: SavedRepository
    // (thêm PlaceRepository, ReviewRepository, CheckInRepository… ở đây)

    // ===== Người 3: đề xuất, báo cáo, thông báo, admin =====
   // Nhật - Tuần 1: Đề xuất quán (UC-14)
    val suggestionRepository: SuggestionRepository

    /** Nạp dữ liệu mẫu (chỉ bản debug, chỉ khi đã nối Firebase); null nếu không dùng được. */
    val seeder: FirestoreSeeder?

    val locationRepository: LocationRepository = FusedLocationRepository(appContext)

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
            seeder = FirestoreSeeder(appContext, db, auth).takeIf { it.isAvailable }
            // Người 2
            savedRepository = FirestoreSavedRepository(db, auth)
            // Người 3 – Nhật
            suggestionRepository = FirestoreSuggestionRepository(db, auth)
        } else {
            Log.i("FoodMap", "Chưa có google-services.json: đang dùng dữ liệu giả")
            val fake = FakeRepositories(areaRepository)
            // Người 1
            authRepository = fake.authRepository
            dishRepository = fake.dishRepository
            userRepository = fake.userRepository
            seeder = null
            // Người 2
            savedRepository = fake.savedRepository
            // Người 3 - Nhật
            suggestionRepository = FakeSuggestionRepository()
        }
    }
}
