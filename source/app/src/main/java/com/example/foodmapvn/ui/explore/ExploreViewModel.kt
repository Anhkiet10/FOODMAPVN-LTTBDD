package com.example.foodmapvn.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.data.repository.DishRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ExploreViewModel(
    dishRepository: DishRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(ExploreUiState.ALL)

    val uiState: StateFlow<ExploreUiState> = combine(
        dishRepository.getRegionSummary(),
        dishRepository.getDishes(),
        selectedFilter,
    ) { region, dishes, selected ->
        // Đổi sang tỉnh khác mà bộ lọc cũ không còn -> về "Tất cả"
        val filter = if (selected in region.quickFilters) selected else ExploreUiState.ALL
        ExploreUiState(
            regionName = region.name,
            dishCount = region.dishCount,
            placeCount = region.placeCount,
            isOpenNow = region.isOpenNow,
            filters = listOf(ExploreUiState.ALL) + region.quickFilters,
            selectedFilter = filter,
            dishes = if (filter == ExploreUiState.ALL) dishes
            else dishes.filter { it.name.contains(filter, ignoreCase = true) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(),
    )

    fun onFilterSelected(filter: String) {
        selectedFilter.value = filter
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { ExploreViewModel(appContainer().dishRepository) }
        }
    }
}
