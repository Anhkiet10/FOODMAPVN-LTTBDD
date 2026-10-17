package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.toDish
import com.example.foodmapvn.data.firebase.mapper.toPlacePin
import com.example.foodmapvn.data.firebase.mapper.toRegionSummary
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.repository.DishRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreDishRepository(
    private val db: FirebaseFirestore,
    private val regionId: String,
) : DishRepository {

    override fun getRegionSummary(): Flow<RegionSummary> =
        db.collection(FirestorePaths.REGIONS).document(regionId).asFlow()
            .map { it.toRegionSummary() }

    override fun getDishes(): Flow<List<Dish>> =
        db.collection(FirestorePaths.DISHES)
            .whereEqualTo("regionId", regionId)
            .whereEqualTo("status", "active")
            .asFlow()
            .map { snapshot -> snapshot.documents.map { it.toDish() } }

    override fun getPlacePins(): Flow<List<PlacePin>> =
        db.collection(FirestorePaths.PLACES)
            .whereEqualTo("regionId", regionId)
            .whereEqualTo("status", "active")
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toPlacePin() } }
}
