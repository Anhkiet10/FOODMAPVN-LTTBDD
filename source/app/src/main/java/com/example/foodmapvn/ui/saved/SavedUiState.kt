package com.example.foodmapvn.ui.saved

import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace

enum class SavedTab { DISHES, PLACES }

data class SavedUiState(
    val selectedTab: SavedTab = SavedTab.DISHES,
    val regionName: String = "",
    val dishes: List<SavedDish> = emptyList(),
    val places: List<SavedPlace> = emptyList(),
    val routeSuggestion: RouteSuggestion? = null,
) {
    val eatenCount: Int get() = dishes.count { it.isEaten }
    val remainingCount: Int get() = dishes.size - eatenCount
    val progress: Float get() = if (dishes.isEmpty()) 0f else eatenCount.toFloat() / dishes.size
}
