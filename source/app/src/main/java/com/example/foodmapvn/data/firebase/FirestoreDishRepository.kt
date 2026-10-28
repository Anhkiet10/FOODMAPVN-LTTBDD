package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.toDish
import com.example.foodmapvn.data.firebase.mapper.toPlacePin
import com.example.foodmapvn.data.firebase.mapper.toPlaceSummary
import com.example.foodmapvn.data.firebase.mapper.toRegionSummary
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Dữ liệu của khu vực ĐANG CHỌN (AreaRepository): đổi tỉnh/thành là mọi màn đọc lại theo vùng mới.
 * Mã tỉnh trùng Document ID của collection regions (hue, dak-lak, can-tho…).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreDishRepository(
    private val db: FirebaseFirestore,
    areaRepository: AreaRepository,
) : DishRepository {

    private val area: Flow<Province> = areaRepository.selectedArea
        .map { it ?: VietnamProvinces.DEFAULT }
        .distinctUntilChanged()

    override fun getRegionSummary(): Flow<RegionSummary> = area.flatMapLatest { province ->
        db.collection(FirestorePaths.REGIONS).document(province.id).asFlow()
            .map { doc ->
                // Tỉnh chưa có dữ liệu trên Firestore: vẫn hiện tên tỉnh, 0 đặc sản
                if (doc.exists()) doc.toRegionSummary() else RegionSummary.empty(province.name)
            }
    }

    override fun getDishes(): Flow<List<Dish>> = area.flatMapLatest { province ->
        db.collection(FirestorePaths.DISHES)
            .whereEqualTo("regionId", province.id)
            .whereEqualTo("status", "active")
            .asFlow()
            .map { snapshot -> snapshot.documents.map { it.toDish() } }
    }

    override fun getPlacePins(): Flow<List<PlacePin>> = area.flatMapLatest { province ->
        db.collection(FirestorePaths.PLACES)
            .whereEqualTo("regionId", province.id)
            .whereEqualTo("status", "active")
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toPlacePin() } }
    }

    // Cùng truy vấn với getPlacePins; Firestore dùng chung một listener cho truy vấn giống nhau
    override fun getPlaces(): Flow<List<PlaceSummary>> = area.flatMapLatest { province ->
        db.collection(FirestorePaths.PLACES)
            .whereEqualTo("regionId", province.id)
            .whereEqualTo("status", "active")
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toPlaceSummary() } }
    }
}
