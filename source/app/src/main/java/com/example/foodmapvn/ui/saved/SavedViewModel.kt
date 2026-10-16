package com.example.foodmapvn.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.SavedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(
    private val savedRepository: SavedRepository,
    dishRepository: DishRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(SavedTab.DISHES)

    val uiState: StateFlow<SavedUiState> = combine(
        savedRepository.getSavedDishes(),
        savedRepository.getSavedPlaces(),
        savedRepository.getRouteSuggestion(),
        dishRepository.getRegionSummary(),
        selectedTab,
    ) { dishes, places, route, region, tab ->
        SavedUiState(
            selectedTab = tab,
            regionName = region.name,
            dishes = dishes,
            places = places,
            routeSuggestion = route,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState(),
    )

    fun onTabSelected(tab: SavedTab) {
        selectedTab.value = tab
    }

    fun onToggleEaten(dish: SavedDish) {
        viewModelScope.launch { savedRepository.toggleEaten(dish.id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SavedViewModel(
                    savedRepository = container.savedRepository,
                    dishRepository = container.dishRepository,
                )
            }
        }
    }
}
