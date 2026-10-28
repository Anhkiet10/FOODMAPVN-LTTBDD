package com.example.foodmapvn.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.data.repository.SearchHistoryRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.ExploreFilter
import com.example.foodmapvn.domain.GeoUtils
import com.example.foodmapvn.domain.OpeningHours
import com.example.foodmapvn.domain.TextSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** Một quán trong kết quả tìm kiếm, đã tính sẵn chữ hiển thị. */
data class PlaceResult(
    val place: PlaceSummary,
    val distance: String?,
    val isOpen: Boolean?,
)

data class SearchUiState(
    val query: String = "",
    val areaName: String = "",
    val recent: List<String> = emptyList(),
    val dishes: List<Dish> = emptyList(),
    val places: List<PlaceResult> = emptyList(),
    val provinces: List<Province> = emptyList(),
) {
    val isTyping: Boolean get() = query.isNotBlank()
    val hasResults: Boolean get() = dishes.isNotEmpty() || places.isNotEmpty() || provinces.isNotEmpty()
}

/**
 * UC-06 Tìm kiếm món / quán / vùng.
 * Món và quán tìm trong khu vực đang chọn (lọc ngay trong app, không tốn thêm lượt đọc Firestore);
 * vùng tìm trong 34 tỉnh/thành — chọn vùng là đổi khu vực của cả app.
 */
class SearchViewModel(
    dishRepository: DishRepository,
    private val areaRepository: AreaRepository,
    private val historyRepository: SearchHistoryRepository,
    private val locationRepository: LocationRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val myLocation = MutableStateFlow<LatLng?>(null)

    private val data = combine(
        dishRepository.getDishes().catch { emit(emptyList()) },
        dishRepository.getPlaces().catch { emit(emptyList()) },
        myLocation,
    ) { dishes, places, me -> Triple(dishes, places, me) }

    val uiState: StateFlow<SearchUiState> = combine(
        query, data, historyRepository.recent, areaRepository.selectedArea,
    ) { q, (dishes, places, me), recent, area ->
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        val placeMatches = if (q.isBlank()) emptyList() else
            ExploreFilter.sortPlaces(places.filter { TextSearch.matches(q, it.name, it.address) }, me)
        SearchUiState(
            query = q,
            areaName = (area ?: VietnamProvinces.DEFAULT).name,
            recent = recent,
            dishes = if (q.isBlank()) emptyList() else dishes.filter { TextSearch.matches(q, it.name) },
            places = placeMatches.map { p ->
                PlaceResult(
                    place = p,
                    distance = ExploreFilter.distanceMeters(p, me)?.let(GeoUtils::formatDistance),
                    isOpen = OpeningHours.isOpen(p.openTime, p.closeTime, now),
                )
            },
            provinces = if (q.isBlank()) emptyList() else
                VietnamProvinces.all.filter { p -> TextSearch.matches(q, p.name, *p.formerNames.toTypedArray()) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    init {
        // Có quyền vị trí thì dùng để tính khoảng cách tới quán
        viewModelScope.launch { myLocation.value = locationRepository.currentLocation() }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Bấm tìm trên bàn phím, hoặc mở một kết quả: lưu vào lịch sử. */
    fun saveQuery() {
        if (query.value.isBlank()) return
        historyRepository.add(query.value)
        analytics.search(query.value)
    }

    fun useRecent(value: String) {
        query.value = value
        historyRepository.add(value)
    }

    fun removeRecent(value: String) = historyRepository.remove(value)
    fun clearRecent() = historyRepository.clear()

    fun selectProvince(province: Province) {
        saveQuery()
        areaRepository.select(province)
        analytics.selectRegion(province.id, source = "search")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SearchViewModel(c.dishRepository, c.areaRepository, c.searchHistoryRepository, c.locationRepository, c.analytics)
            }
        }
    }
}
