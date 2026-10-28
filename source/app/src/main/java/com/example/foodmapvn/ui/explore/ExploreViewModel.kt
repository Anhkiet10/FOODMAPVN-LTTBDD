package com.example.foodmapvn.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.ExploreFilter
import com.example.foodmapvn.domain.ExploreFilters
import com.example.foodmapvn.domain.GeoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Khám phá: đặc sản của khu vực đang chọn.
 * - Lọc nhanh theo tên món (chip) + Bộ lọc nâng cao (UC-05): giá, khoảng cách, uy tín, đang mở cửa, ẩn quán cảnh báo giá.
 * - "Gần nhất" tính từ vị trí thật tới quán thỏa bộ lọc gần nhất; chưa có vị trí thì dùng nearestHint.
 */
class ExploreViewModel(
    dishRepository: DishRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(ExploreUiState.ALL)
    private val filters = MutableStateFlow(ExploreFilters())
    private val myLocation = MutableStateFlow<LatLng?>(null)

    private val data = combine(
        dishRepository.getRegionSummary(),
        dishRepository.getDishes(),
        dishRepository.getPlaces().catch { emit(emptyList()) },
    ) { region, dishes, places -> Triple(region, dishes, places) }

    val uiState: StateFlow<ExploreUiState> = combine(
        data, selectedFilter, filters, myLocation,
    ) { (region, dishes, places), selected, advanced, me ->
        // Đổi sang tỉnh khác mà bộ lọc cũ không còn -> về "Tất cả"
        val quick = if (selected in region.quickFilters) selected else ExploreUiState.ALL
        val byName = if (quick == ExploreUiState.ALL) dishes else dishes.filter { it.name.contains(quick, ignoreCase = true) }
        val matches = ExploreFilter.filterDishes(byName, places, advanced, me, nowMinutes())
        ExploreUiState(
            regionName = region.name,
            dishCount = region.dishCount,
            placeCount = region.placeCount,
            isOpenNow = region.isOpenNow,
            filters = listOf(ExploreUiState.ALL) + region.quickFilters,
            selectedFilter = quick,
            advancedFilters = advanced,
            hasLocation = me != null,
            dishes = matches.map { m ->
                m.nearestMeters?.let { m.dish.copy(nearestDistance = GeoUtils.formatDistance(it)) } ?: m.dish
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(),
    )

    init {
        refreshLocation()
    }

    /** Lấy vị trí thật (nếu đã cấp quyền) để tính "gần nhất" và lọc khoảng cách. */
    fun refreshLocation() {
        viewModelScope.launch { locationRepository.currentLocation()?.let { myLocation.value = it } }
    }

    fun onFilterSelected(filter: String) {
        selectedFilter.value = filter
    }

    fun onAdvancedFiltersChange(value: ExploreFilters) {
        filters.value = value
    }

    private fun nowMinutes(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ExploreViewModel(c.dishRepository, c.locationRepository)
            }
        }
    }
}
