package com.example.foodmapvn.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.data.repository.ReportRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bộ lọc danh sách đánh giá (Figma "Danh sách đánh giá"). */
enum class ReviewFilter(val label: String) {
    ALL("Tất cả"),
    LOCAL("Người địa phương"),
    CHECKED_IN("Đã check-in"),
    WITH_PHOTOS("Có ảnh");

    fun matches(r: ReviewItem): Boolean = when (this) {
        ALL -> true
        LOCAL -> r.isLocal
        CHECKED_IN -> r.isCheckedIn
        WITH_PHOTOS -> r.photos.isNotEmpty()
    }
}

class PlaceReviewsViewModel(
    private val placeId: String,
    placeRepository: PlaceRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    val placeDetail: StateFlow<PlaceDetail?> = placeRepository.getPlaceDetail(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val reviews: StateFlow<List<ReviewItem>> = placeRepository.getPlaceReviews(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _filter = MutableStateFlow(ReviewFilter.ALL)
    val filter: StateFlow<ReviewFilter> = _filter.asStateFlow()

    /** Thông báo ngắn (Snackbar), vd. "Đã gửi báo cáo". */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isReporting = MutableStateFlow(false)
    val isReporting: StateFlow<Boolean> = _isReporting.asStateFlow()

    fun setFilter(value: ReviewFilter) {
        _filter.value = value
    }

    /** Gửi báo cáo một đánh giá; xong gọi [onDone] để đóng bảng báo cáo. */
    fun reportReview(review: ReviewItem, reason: ReportReason, note: String, onDone: () -> Unit) {
        if (_isReporting.value) return
        viewModelScope.launch {
            _isReporting.value = true
            reportRepository.report("REVIEW", review.id, reason, note)
                .onSuccess {
                    _message.value = "Đã gửi báo cáo. Admin sẽ xem xét đánh giá này."
                    onDone()
                }
                .onFailure { _message.value = it.message ?: "Gửi báo cáo thất bại" }
            _isReporting.value = false
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
                PlaceReviewsViewModel(
                    placeId = placeId,
                    placeRepository = container.placeRepository,
                    reportRepository = container.reportRepository,
                )
            }
        }
    }
}
