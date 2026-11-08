package com.example.foodmapvn.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.repository.DishRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Một món đặc sản quán có bán, kèm giá thấp nhất trong menu quán (đồng; null = chưa có giá). */
data class PlaceDish(val dish: Dish, val minPrice: Long?)

class PlaceDetailViewModel(
    private val placeId: String,
    private val placeRepository: PlaceRepository,
    dishRepository: DishRepository? = null,
) : ViewModel() {

    val placeDetail: StateFlow<PlaceDetail?> = placeRepository.getPlaceDetail(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /**
     * "Món của quán": các món đặc sản quán bán (dishIds của quán), tên/ảnh lấy từ danh sách món của vùng.
     * Món không có trong vùng đang chọn thì dùng dữ liệu món đi kèm quán (nếu có).
     */
    val dishes: StateFlow<List<PlaceDish>> = combine(
        placeDetail,
        dishRepository?.getDishes()?.catch { emit(emptyList()) } ?: flowOf(emptyList()),
    ) { place, regionDishes ->
        if (place == null) return@combine emptyList()
        val ids = place.dishIds.ifEmpty { place.dishes.map { it.id } }
        ids.distinct().mapNotNull { id ->
            val dish = regionDishes.firstOrNull { it.id == id } ?: place.dishes.firstOrNull { it.id == id }
            dish?.let { PlaceDish(it, place.minPriceByDish[id]) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviews: StateFlow<List<ReviewItem>> = placeRepository.getPlaceReviews(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Thông báo ngắn sau khi Lưu / Bỏ lưu quán (giống Lưu món muốn ăn). */
    private val _message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun toggleSavePlace() {
        val wasSaved = placeDetail.value?.isSaved == true
        viewModelScope.launch {
            runCatching { placeRepository.toggleSavePlace(placeId) }
                .onSuccess {
                    _message.value = if (wasSaved) "Đã bỏ lưu quán" else "Đã lưu quán vào mục Đã lưu"
                }
                .onFailure { _message.value = it.message ?: "Không lưu được quán, thử lại sau" }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val placeId = checkNotNull(savedStateHandle.get<String>(Routes.ARG_PLACE_ID))
                val container = appContainer()
                PlaceDetailViewModel(
                    placeId = placeId,
                    placeRepository = container.placeRepository,
                    dishRepository = container.dishRepository,
                )
            }
        }
    }
}
