package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.data.repository.SavedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    override fun isDishSaved(dishId: String): Flow<Boolean> = dishes.map { list -> list.any { it.id == dishId } }

    override suspend fun saveDish(dish: DishDetail, placeId: String?, placeName: String?, distanceHint: String?): Result<Unit> {
        dishes.update { list ->
            list.filterNot { it.id == dish.id } + SavedDish(
                id = dish.id, name = dish.name, placeName = placeName, distance = distanceHint,
                isEaten = false, art = dish.art, imageUrl = dish.imageUrl,
            )
        }
        return Result.success(Unit)
    }

    override suspend fun removeDish(dishId: String): Result<Unit> {
        dishes.update { list -> list.filterNot { it.id == dishId } }
        return Result.success(Unit)
    }
}
