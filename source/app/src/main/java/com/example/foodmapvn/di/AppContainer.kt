package com.example.foodmapvn.di

import android.content.Context
import android.util.Log
import com.example.foodmapvn.R
import com.example.foodmapvn.data.cloudinary.CloudinaryImageUploader
import com.example.foodmapvn.data.fake.FakeRepositories
import com.example.foodmapvn.data.firebase.FirebaseAuthRepository
import com.example.foodmapvn.data.firebase.FirestoreDishRepository
import com.example.foodmapvn.data.firebase.FirestoreSavedRepository
import com.example.foodmapvn.data.firebase.FirestoreUserRepository
import com.example.foodmapvn.data.location.FusedLocationRepository
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

/**
 * Nơi duy nhất tạo các phụ thuộc dùng chung cho cả app (dependency injection thủ công).
 *
 * - Có app/google-services.json  -> dùng Firebase (Auth + Firestore).
 * - Chưa có                       -> dùng dữ liệu giả, app vẫn chạy để làm giao diện.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Vùng mặc định khi mở app. TODO: lấy từ màn Chọn vùng. */
    val currentRegionId = "hue"

    val isFirebaseEnabled: Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    // Quy tắc nghiệp vụ (domain)
    val businessRules = BusinessRules()
    val checkInValidator = CheckInValidator(businessRules)
    val trustScoreCalculator = TrustScoreCalculator(businessRules)
    val priceRangeCalculator = PriceRangeCalculator(businessRules)
    val badgeCalculator = BadgeCalculator(businessRules)

    val authRepository: AuthRepository
    val dishRepository: DishRepository
    val savedRepository: SavedRepository
    val userRepository: UserRepository

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
            authRepository = FirebaseAuthRepository(auth, db)
            dishRepository = FirestoreDishRepository(db, currentRegionId)
            savedRepository = FirestoreSavedRepository(db, auth)
            userRepository = FirestoreUserRepository(db, auth, badgeCalculator)
        } else {
            Log.i("FoodMap", "Chưa có google-services.json: đang dùng dữ liệu giả")
            val fake = FakeRepositories()
            authRepository = fake.authRepository
            dishRepository = fake.dishRepository
            savedRepository = fake.savedRepository
            userRepository = fake.userRepository
        }
    }
}
