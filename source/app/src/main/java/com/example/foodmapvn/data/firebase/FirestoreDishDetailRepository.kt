package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.firebase.mapper.str
import com.example.foodmapvn.data.firebase.mapper.toDishDetail
import com.example.foodmapvn.data.firebase.mapper.toPlaceSummary
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.repository.DishDetailRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Người 2: đọc dishes/{dishId} và các quán có bán món (theo thời gian thực). */
class FirestoreDishDetailRepository(
    private val db: FirebaseFirestore,
) : DishDetailRepository {

    override fun getDish(dishId: String): Flow<DishDetail?> =
        db.collection(FirestorePaths.DISHES).document(dishId).asFlow()
            .map { doc -> if (doc.exists() && doc.str("status") != "hidden") doc.toDishDetail() else null }

    // Chỉ dùng array-contains (index đơn tự có); lọc status trong app để khỏi phải tạo composite index
    override fun getPlacesServing(dishId: String): Flow<List<PlaceSummary>> =
        db.collection(FirestorePaths.PLACES)
            .whereArrayContains("dishIds", dishId)
            .asFlow()
            .map { snapshot ->
                snapshot.documents
                    .filter { it.str("status") == "active" }
                    .mapNotNull { it.toPlaceSummary() }
            }
}
