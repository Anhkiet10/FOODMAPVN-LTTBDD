package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.cleanDataText
import com.example.foodmapvn.data.firebase.mapper.toDish
import com.example.foodmapvn.data.firebase.mapper.toPlaceSummary
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.domain.CheckInResult
import com.example.foodmapvn.domain.CheckInValidator
import com.example.foodmapvn.domain.ReviewInput
import com.example.foodmapvn.domain.TrustScoreCalculator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class FirestorePlaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val checkInValidator: CheckInValidator = CheckInValidator(),
    private val trustScoreCalculator: TrustScoreCalculator = TrustScoreCalculator(),
) : PlaceRepository {

    private fun userDoc(uid: String) = db.collection(FirestorePaths.USERS).document(uid)

    override fun getPlaceDetail(placeId: String): Flow<PlaceDetail?> =
        auth.uidFlow().flatMapLatest { uid ->
            val placeFlow = db.collection(FirestorePaths.PLACES).document(placeId).asFlow()
            val savedFlow = if (uid == null) flowOf(false)
            else userDoc(uid).collection(FirestorePaths.SAVED_PLACES).document(placeId).asFlow()
                .map { it.exists() }

            combine(placeFlow, savedFlow) { doc, isSaved ->
                if (!doc.exists()) return@combine null
                val geo = doc.getGeoPoint("location") ?: GeoPoint(16.4655, 107.5905)
                val dishIds = (doc.get("dishIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val photos = (doc.get("photos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val minPricesRaw = doc.get("minPriceByDish") as? Map<*, *>
                val minPrices = minPricesRaw?.entries?.associate {
                    (it.key as String) to ((it.value as? Number)?.toLong() ?: 0L)
                } ?: emptyMap()

                PlaceDetail(
                    id = doc.id,
                    name = cleanDataText(doc.getString("name")),
                    address = cleanDataText(doc.getString("address")),
                    location = LatLng(geo.latitude, geo.longitude),
                    openTime = doc.getString("openTime"),
                    closeTime = doc.getString("closeTime"),
                    trustScore = doc.getLong("trustScore")?.toInt() ?: 80,
                    isVerified = doc.getBoolean("isVerified") ?: false,
                    hasPriceWarning = doc.getBoolean("priceWarning") ?: false,
                    ratingAvg = doc.getDouble("ratingAvg") ?: 0.0,
                    reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0,
                    checkInCount = doc.getLong("checkInCount")?.toInt() ?: 0,
                    imageUrl = photos.firstOrNull(),
                    photos = photos,
                    dishIds = dishIds,
                    minPriceByDish = minPrices,
                    isSaved = isSaved,
                    regionId = doc.getString("regionId") ?: "",
                )
            }
        }

    override fun getDishDetail(dishId: String): Flow<DishDetail?> =
        auth.uidFlow().flatMapLatest { uid ->
            val dishFlow = db.collection(FirestorePaths.DISHES).document(dishId).asFlow()
            val savedFlow = if (uid == null) flowOf(false)
            else userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId).asFlow()
                .map { it.exists() }
            val placesFlow = db.collection(FirestorePaths.PLACES)
                .whereArrayContains("dishIds", dishId)
                .whereEqualTo("status", "active")
                .asFlow()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toPlaceSummary() } }

            combine(dishFlow, savedFlow, placesFlow) { doc, isSaved, places ->
                if (!doc.exists()) return@combine null
                val dish = doc.toDish()
                val description = doc.getString("description")
                val bestTime = doc.getString("bestTime")
                val nearestHint = doc.getString("nearestHint")
                val tips = (doc.get("tips") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val photos = (doc.get("photos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                DishDetail(
                    dish = dish,
                    description = description,
                    bestTime = bestTime,
                    nearestHint = nearestHint,
                    tips = tips,
                    photos = photos,
                    nearbyPlaces = places,
                    isSaved = isSaved,
                )
            }
        }

    override fun getPlaceReviews(placeId: String): Flow<List<ReviewItem>> =
        db.collection(FirestorePaths.REVIEWS)
            .whereEqualTo("placeId", placeId)
            .asFlow()
            .map { snapshot ->
                snapshot.documents
                    .filter { (it.getString("status") ?: "visible") == "visible" }
                    .sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
                    .map { doc ->
                        val photos = (doc.get("photos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val ts = doc.getTimestamp("createdAt")
                        ReviewItem(
                            id = doc.id,
                            placeId = doc.getString("placeId") ?: "",
                            placeName = doc.getString("placeName") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            userAvatar = doc.getString("userAvatar"),
                            rating = doc.getLong("stars")?.toInt() ?: 5,
                            content = doc.getString("content"),
                            photos = photos,
                            createdAt = ts?.toDate()?.let {
                                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it)
                            } ?: "Mới đây",
                            createdAtMillis = ts?.toDate()?.time ?: System.currentTimeMillis(),
                            isCheckedIn = doc.getBoolean("hasCheckIn") ?: false,
                            isLocal = doc.getBoolean("isLocal") ?: false,
                            price = doc.getLong("price"),
                            helpfulCount = doc.getLong("helpfulCount")?.toInt() ?: 0,
                        )
                    }
            }

    override fun isPlaceSaved(placeId: String): Flow<Boolean> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(false)
        else userDoc(uid).collection(FirestorePaths.SAVED_PLACES).document(placeId).asFlow()
            .map { it.exists() }
    }

    override suspend fun toggleSavePlace(placeId: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = userDoc(uid).collection(FirestorePaths.SAVED_PLACES).document(placeId)
        val doc = ref.get().awaitTask()
        if (doc.exists()) {
            ref.delete().awaitTask()
        } else {
            val placeDoc = db.collection(FirestorePaths.PLACES).document(placeId).get().awaitTask()
            val name = placeDoc.getString("name") ?: "Quán"
            val photos = (placeDoc.get("photos") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            ref.set(
                mapOf(
                    "name" to name,
                    "dishName" to "Đặc sản",
                    "trustScore" to (placeDoc.getLong("trustScore")?.toInt() ?: 80),
                    "savedAt" to FieldValue.serverTimestamp(),
                    "imageUrl" to photos.firstOrNull(),
                )
            ).awaitTask()
        }
    }

    override suspend fun toggleSaveDish(dishId: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId)
        val doc = ref.get().awaitTask()
        if (doc.exists()) {
            ref.delete().awaitTask()
        } else {
            val dishDoc = db.collection(FirestorePaths.DISHES).document(dishId).get().awaitTask()
            val name = dishDoc.getString("name") ?: "Đặc sản"
            val regionId = dishDoc.getString("regionId") ?: ""
            ref.set(
                mapOf(
                    "dishName" to name,
                    "regionId" to regionId,
                    "eaten" to false,
                    "addedAt" to FieldValue.serverTimestamp(),
                )
            ).awaitTask()
        }
    }

    override suspend fun performCheckIn(
        placeId: String,
        userLat: Double,
        userLon: Double,
    ): Result<CheckInResult> = runCatching {
        val uid = auth.currentUser?.uid
            ?: error("Bạn cần đăng nhập để check-in")

        val placeDoc = db.collection(FirestorePaths.PLACES).document(placeId).get().awaitTask()
        if (!placeDoc.exists()) error("Quán không tồn tại")

        val geo = placeDoc.getGeoPoint("location")
            ?: error("Quán chưa có vị trí GPS")

        val regionId = placeDoc.getString("regionId") ?: ""

        val lastCheckInSnap = db.collection(FirestorePaths.CHECKINS)
            .whereEqualTo("userId", uid)
            .whereEqualTo("placeId", placeId)
            .get()
            .awaitTask()

        val lastCheckInMillis = lastCheckInSnap.documents
            .mapNotNull { it.getTimestamp("createdAt")?.toDate()?.time }
            .maxOrNull()

        val result = checkInValidator.validate(
            userLat = userLat,
            userLon = userLon,
            placeLat = geo.latitude,
            placeLon = geo.longitude,
            lastCheckInAtMillis = lastCheckInMillis,
        )

        if (result is CheckInResult.Valid) {
            val checkInRef = db.collection(FirestorePaths.CHECKINS).document()
            checkInRef.set(
                mapOf(
                    "userId" to uid,
                    "placeId" to placeId,
                    "regionId" to regionId,
                    "location" to GeoPoint(userLat, userLon),
                    "distanceM" to result.distanceMeters,
                    "valid" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).awaitTask()

            // Cập nhật checkInCount cho quán
            val currentCheckIns = (placeDoc.getLong("checkInCount") ?: 0L) + 1
            db.collection(FirestorePaths.PLACES).document(placeId)
                .update("checkInCount", currentCheckIns)
                .awaitTask()

            // Cập nhật stats của user an toàn
            userDoc(uid).set(
                mapOf("stats" to mapOf("checkInCount" to FieldValue.increment(1))),
                SetOptions.merge()
            ).awaitTask()

            // Tự động tích "Đã ăn" cho các món của quán có trong wishlist của user
            val dishIds = (placeDoc.get("dishIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            for (dishId in dishIds) {
                val wishRef = userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId)
                val wishDoc = wishRef.get().awaitTask()
                if (wishDoc.exists()) {
                    wishRef.update(
                        mapOf(
                            "eaten" to true,
                            "eatenAt" to FieldValue.serverTimestamp(),
                            "placeId" to placeId,
                            "placeName" to (placeDoc.getString("name") ?: ""),
                        )
                    ).awaitTask()
                }
            }
        }
        result
    }

    override suspend fun submitReview(
        placeId: String,
        rating: Int,
        content: String,
        price: Long?,
        photoUrls: List<String>,
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
            ?: error("Bạn cần đăng nhập để viết đánh giá")

        val placeDoc = db.collection(FirestorePaths.PLACES).document(placeId).get().awaitTask()
        if (!placeDoc.exists()) error("Quán không tồn tại")

        val userDocSnap = userDoc(uid).get().awaitTask()
        val userName = userDocSnap.getString("name") ?: auth.currentUser?.displayName ?: "Người dùng"
        val userAvatar = userDocSnap.getString("avatarUrl") ?: auth.currentUser?.photoUrl?.toString()
        val userBadge = userDocSnap.getString("localBadge") ?: ""

        val placeRegion = placeDoc.getString("regionId") ?: ""
        val isLocal = userBadge.contains(placeRegion, ignoreCase = true)

        // Lấy checkin gần nhất của user tại quán này (truy vấn đơn giản không cần index)
        val lastCheckInSnap = db.collection(FirestorePaths.CHECKINS)
            .whereEqualTo("userId", uid)
            .whereEqualTo("placeId", placeId)
            .get()
            .awaitTask()

        val lastCheckInMillis = lastCheckInSnap.documents
            .mapNotNull { it.getTimestamp("createdAt")?.toDate()?.time }
            .maxOrNull()

        val isCheckedIn = checkInValidator.reviewHasCheckIn(lastCheckInMillis, System.currentTimeMillis())

        val reviewDocId = "${placeId}_$uid"
        val reviewRef = db.collection(FirestorePaths.REVIEWS).document(reviewDocId)

        val reviewPayload = mutableMapOf<String, Any>(
            "placeId" to placeId,
            "placeName" to (placeDoc.getString("name") ?: ""),
            "userId" to uid,
            "userName" to userName,
            "stars" to rating,
            "hasCheckIn" to isCheckedIn,
            "isLocal" to isLocal,
            "helpfulCount" to 0,
            "status" to "visible",
            "createdAt" to FieldValue.serverTimestamp(),
        )

        if (content.isNotBlank()) {
            reviewPayload["content"] = content.trim()
        }
        if (price != null && price > 0) {
            reviewPayload["price"] = price
        }
        if (!userAvatar.isNullOrBlank()) {
            reviewPayload["userAvatar"] = userAvatar
        }
        if (photoUrls.isNotEmpty()) {
            reviewPayload["photos"] = photoUrls
        }

        reviewRef.set(reviewPayload).awaitTask()

        // Tính lại ratingAvg của quán
        val reviewsSnap = db.collection(FirestorePaths.REVIEWS)
            .whereEqualTo("placeId", placeId)
            .get()
            .awaitTask()

        val reviewsList = reviewsSnap.documents
            .filter { (it.getString("status") ?: "visible") == "visible" }
            .map { doc ->
                ReviewInput(
                    stars = doc.getLong("stars")?.toInt() ?: 5,
                    isLocal = doc.getBoolean("isLocal") ?: false,
                )
            }

        val weightedAvg = trustScoreCalculator.weightedAverageStars(reviewsList) ?: rating.toDouble()
        val roundedAvg = (weightedAvg * 10).toInt() / 10.0

        db.collection(FirestorePaths.PLACES).document(placeId).update(
            mapOf(
                "ratingAvg" to roundedAvg,
                "reviewCount" to reviewsList.size,
            )
        ).awaitTask()

        // Cập nhật reviewCount của user an toàn
        userDoc(uid).set(
            mapOf("stats" to mapOf("reviewCount" to FieldValue.increment(1))),
            SetOptions.merge()
        ).awaitTask()
    }
}
