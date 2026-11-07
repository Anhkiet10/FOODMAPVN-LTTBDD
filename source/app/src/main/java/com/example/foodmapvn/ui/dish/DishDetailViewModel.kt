package com.example.foodmapvn.ui.dish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSort
import com.example.foodmapvn.data.repository.DishDetailRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.data.repository.SavedRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.DishPlaceSorter
import com.example.foodmapvn.domain.GeoUtils
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * UC-08 Chi tiết đặc sản + UC-10 Lưu vào Món muốn ăn — Người 2.
 * Nhận dishId từ route "dish/{dishId}" (SavedStateHandle).
 */
class DishDetailViewModel(
    savedStateHandle: SavedStateHandle,
    dishDetailRepository: DishDetailRepository,
    private val savedRepository: SavedRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val dishId: String = checkNotNull(savedStateHandle[Routes.ARG_DISH_ID]) { "Thiếu dishId" }

    private val sort = MutableStateFlow(PlaceSort.TRUST)
    private val myLocation = MutableStateFlow<LatLng?>(null)
    private val saving = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    /** Dữ liệu từ Repository: món + quán bán món + đã lưu chưa. */
    private val data = combine(
        dishDetailRepository.getDish(dishId),
        dishDetailRepository.getPlacesServing(dishId).catch { emit(emptyList()) },
        savedRepository.isDishSaved(dishId).catch { emit(false) },
    ) { dish, places, saved -> Triple(dish, places, saved) }

    val uiState: StateFlow<DishDetailUiState> = combine(
        data, sort, myLocation, saving, message,
    ) { (dish, places, saved), sortBy, me, isSaving, msg ->
        DishDetailUiState(
            isLoading = false,
            notFound = dish == null,
            dish = dish,
            places = DishPlaceSorter.sort(places, dishId, me, nowMinutes(), sortBy),
            sort = sortBy,
            hasLocation = me != null,
            isSaved = saved,
            isSaving = isSaving,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DishDetailUiState(),
    )

    init {
        refreshLocation()
    }

    /** Lấy vị trí thật (nếu đã cấp quyền) để tính khoảng cách. */
    fun refreshLocation() {
        viewModelScope.launch { locationRepository.currentLocation()?.let { myLocation.value = it } }
    }

    fun onSortChange(value: PlaceSort) {
        sort.value = value
        if (value == PlaceSort.DISTANCE && myLocation.value == null) {
            refreshLocation()
            if (!locationRepository.hasPermission()) message.value = "Bật quyền vị trí để xếp theo khoảng cách"
        }
    }

    /** Lưu / bỏ lưu món (màn gọi qua LoginGate nên lúc này đã đăng nhập). */
    fun toggleSave() {
        val state = uiState.value
        val dish = state.dish ?: return
        if (saving.value) return
        saving.value = true
        viewModelScope.launch {
            val result = if (state.isSaved) {
                savedRepository.removeDish(dish.id)
            } else {
                // Gợi ý quán đứng đầu danh sách hiện tại cho tab Đã lưu
                val top = state.places.firstOrNull()
                savedRepository.saveDish(
                    dish = dish,
                    placeId = top?.place?.id,
                    placeName = top?.place?.name,
                    distanceHint = top?.distanceMeters?.let { GeoUtils.formatDistance(it) },
                )
            }
            message.value = result.fold(
                onSuccess = { if (state.isSaved) "Đã bỏ khỏi Món muốn ăn" else "Đã lưu vào Món muốn ăn" },
                onFailure = { "Không lưu được: ${it.message}" },
            )
            saving.value = false
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun nowMinutes(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                DishDetailViewModel(createSavedStateHandle(), c.dishDetailRepository, c.savedRepository, c.locationRepository)
            }
        }
    }
}
