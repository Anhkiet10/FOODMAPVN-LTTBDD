package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.data.repository.SavedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update

/** Giữ danh sách trong bộ nhớ: tích "Đã ăn" có tác dụng cho tới khi tắt app. */
class FakeSavedRepository : SavedRepository {
    private val dishes = MutableStateFlow(FakeData.savedDishes)

    override fun getSavedDishes(): Flow<List<SavedDish>> = dishes.asStateFlow()
    override fun getSavedPlaces(): Flow<List<SavedPlace>> = flowOf(FakeData.savedPlaces)
    override fun getRouteSuggestion(): Flow<RouteSuggestion?> = flowOf(FakeData.routeSuggestion)

    override suspend fun toggleEaten(dishId: String) {
        dishes.update { list ->
            list.map { if (it.id == dishId) it.copy(isEaten = !it.isEaten) else it }
        }
    }
}
