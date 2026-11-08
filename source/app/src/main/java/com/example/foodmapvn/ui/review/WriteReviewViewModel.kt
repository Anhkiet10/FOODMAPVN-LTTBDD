package com.example.foodmapvn.ui.review

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.local.ImageCompressor
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.repository.ImageUploader
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ReviewUiState {
    data object Idle : ReviewUiState
    data class Submitting(val progressText: String) : ReviewUiState
    data object Success : ReviewUiState
    data class Error(val message: String) : ReviewUiState
}

class WriteReviewViewModel(
    private val placeId: String,
    private val placeRepository: PlaceRepository,
    private val imageUploader: ImageUploader,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {

    val placeDetail: StateFlow<PlaceDetail?> = placeRepository.getPlaceDetail(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val rating = MutableStateFlow(5)
    val content = MutableStateFlow("")
    val priceText = MutableStateFlow("")
    val selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun setRating(stars: Int) {
        rating.value = stars.coerceIn(1, 5)
    }

    fun addImageUri(uri: Uri) {
        if (selectedImageUris.value.size < 5) {
            selectedImageUris.value = selectedImageUris.value + uri
        }
    }

    fun removeImageUri(uri: Uri) {
        selectedImageUris.value = selectedImageUris.value - uri
    }

    fun submitReview(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Submitting("Đang tải ảnh và gửi đánh giá...")

            val uploadedUrls = mutableListOf<String>()
            val uris = selectedImageUris.value

            for (i in uris.indices) {
                _uiState.value = ReviewUiState.Submitting("Đang tải ảnh ${i + 1}/${uris.size}...")
                val uri = uris[i]
                val compressedUri = imageCompressor.compress(uri).getOrDefault(uri)
                val uploadResult = imageUploader.upload(compressedUri)
                val url = uploadResult.getOrNull()
                if (url == null) {
                    // Trước đây ảnh lỗi bị bỏ qua âm thầm -> đánh giá lưu mà không có ảnh.
                    // Giờ dừng lại và báo rõ lý do để sửa (cấu hình Cloudinary / mạng) hoặc bỏ ảnh rồi gửi lại.
                    val reason = uploadResult.exceptionOrNull()?.message ?: "không rõ lỗi"
                    android.util.Log.e("FoodMap", "Tải ảnh đánh giá thất bại: $reason")
                    _uiState.value = ReviewUiState.Error(
                        "Không tải được ảnh ${i + 1}: $reason\nKiểm tra mạng và cấu hình Cloudinary, hoặc xoá ảnh rồi gửi lại."
                    )
                    return@launch
                }
                uploadedUrls.add(url)
            }
            _uiState.value = ReviewUiState.Submitting("Đang gửi đánh giá...")

            val price = priceText.value.toLongOrNull()
            val result = placeRepository.submitReview(
                placeId = placeId,
                rating = rating.value,
                content = content.value,
                price = price,
                photoUrls = uploadedUrls,
            )

            result.onSuccess {
                _uiState.value = ReviewUiState.Success
                onSuccess()
            }.onFailure { err ->
                _uiState.value = ReviewUiState.Error(err.message ?: "Gửi đánh giá thất bại")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val placeId = checkNotNull(savedStateHandle.get<String>(Routes.ARG_PLACE_ID))
                val container = appContainer()
                WriteReviewViewModel(
                    placeId = placeId,
                    placeRepository = container.placeRepository,
                    imageUploader = container.imageUploader,
                    imageCompressor = container.imageCompressor,
                )
            }
        }
    }
}
