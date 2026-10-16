package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import kotlinx.coroutines.flow.Flow

/** Món muốn ăn và quán đã lưu của người dùng. */
interface SavedRepository {
    fun getSavedDishes(): Flow<List<SavedDish>>
    fun getSavedPlaces(): Flow<List<SavedPlace>>
    fun getRouteSuggestion(): Flow<RouteSuggestion?>
    suspend fun toggleEaten(dishId: String)
}
