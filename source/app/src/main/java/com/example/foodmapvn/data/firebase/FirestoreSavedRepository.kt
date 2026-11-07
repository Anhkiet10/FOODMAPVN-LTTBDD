package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.bool
import com.example.foodmapvn.data.firebase.mapper.toSavedDish
import com.example.foodmapvn.data.firebase.mapper.toSavedPlace
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.data.repository.SavedRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Món muốn ăn và quán đã lưu nằm dưới users/{uid}. Khách (chưa đăng nhập) thấy danh sách rỗng. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreSavedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : SavedRepository {

    private fun userDoc(uid: String) = db.collection(FirestorePaths.USERS).document(uid)

    override fun getSavedDishes(): Flow<List<SavedDish>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else userDoc(uid).collection(FirestorePaths.WISHLIST).asFlow()
            .map { snapshot -> snapshot.documents.map { it.toSavedDish() } }
    }

    override fun getSavedPlaces(): Flow<List<SavedPlace>> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else userDoc(uid).collection(FirestorePaths.SAVED_PLACES).asFlow()
            .map { snapshot -> snapshot.documents.map { it.toSavedPlace() } }
    }

    /**
     * Gợi ý đơn giản: các món chưa ăn đã chọn quán.
     * TODO: sắp xếp theo quãng đường thật khi có vị trí người dùng (LocationRepository).
     */
    override fun getRouteSuggestion(): Flow<RouteSuggestion?> = getSavedDishes().map { dishes ->
        val pending = dishes.filter { !it.isEaten && it.placeName != null }
        if (pending.isEmpty()) null
        else RouteSuggestion(placeCount = pending.size, totalDistance = "—", note = "theo thứ tự gần nhất")
    }

    override suspend fun toggleEaten(dishId: String) {
        val uid = auth.currentUser?.uid ?: return
        val ref = userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId)
        val eaten = ref.get().awaitTask().bool("eaten") ?: false
        ref.update(
            mapOf(
                "eaten" to !eaten,
                "eatenAt" to if (!eaten) FieldValue.serverTimestamp() else null,
            )
        ).awaitTask()
    }

    override fun isDishSaved(dishId: String): Flow<Boolean> = auth.uidFlow().flatMapLatest { uid ->
        if (uid == null) flowOf(false)
        else userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId).asFlow().map { it.exists() }
    }

    override suspend fun saveDish(
        dish: DishDetail,
        placeId: String?,
        placeName: String?,
        distanceHint: String?,
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Hãy đăng nhập để lưu món")
        // Trường theo isValidWishlist() trong firestore.rules; null thì bỏ hẳn trường
        val data = buildMap<String, Any> {
            put("dishName", dish.name)
            put("regionId", dish.regionId)
            put("eaten", false)
            put("addedAt", FieldValue.serverTimestamp())
            put("art", dish.art.name)
            dish.imageUrl?.let { put("imageUrl", it) }
            placeId?.let { put("placeId", it) }
            placeName?.let { put("placeName", it) }
            distanceHint?.let { put("distanceHint", it) }
        }
        userDoc(uid).collection(FirestorePaths.WISHLIST).document(dish.id).set(data).awaitTask()
        Unit
    }

    override suspend fun removeDish(dishId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Hãy đăng nhập")
        userDoc(uid).collection(FirestorePaths.WISHLIST).document(dishId).delete().awaitTask()
        Unit
    }
}
