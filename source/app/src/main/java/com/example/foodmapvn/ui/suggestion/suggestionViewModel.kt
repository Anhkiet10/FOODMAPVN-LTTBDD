package com.example.foodmapvn.ui.suggestion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.NewSuggestion
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.ImageUploader
import com.example.foodmapvn.data.repository.SuggestionRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.BusinessRules
import com.example.foodmapvn.domain.GeoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SuggestionStep { MAP, FORM }

data class SuggestionUiState(
    val step: SuggestionStep = SuggestionStep.MAP,
    val lat: Double = 16.4637,
    val lng: Double = 107.5909,
    val nearbyDuplicates: List<PlacePin> = emptyList(),
    val checkingNearby: Boolean = false,
    val dishes: List<Dish> = emptyList(),
    val region: Province? = null,
    val name: String = "",
    val address: String = "",
    val selectedDishId: String? = null,
    val priceText: String = "",
    val openHours: String = "",
    val photoUri: Uri? = null,
    val uploading: Boolean = false,
    val submitting: Boolean = false,
    val message: String? = null, // lỗi hoặc thành công
)

class SuggestionViewModel(
    private val suggestions: SuggestionRepository,
    private val dishes: DishRepository,
    private val area: AreaRepository,
    private val uploader: ImageUploader,
    private val rules: BusinessRules,
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestionUiState())
    val state: StateFlow<SuggestionUiState> = _state.asStateFlow()
    private var nearbyJob: Job? = null

    init {
        viewModelScope.launch {
            area.selectedArea.collect { p ->
                if (p == null) return@collect
                _state.update {
                    it.copy(region = p, lat = p.center.latitude, lng = p.center.longitude)
                }
                checkNearby(p.center.latitude, p.center.longitude)
            }
        }
        viewModelScope.launch {
            dishes.getDishes().collect { list -> _state.update { it.copy(dishes = list) } }
        }
    }

    fun onPinMoved(lat: Double, lng: Double) {
        _state.update { it.copy(lat = lat, lng = lng, message = null) }
        checkNearby(lat, lng)
    }

    private fun checkNearby(lat: Double, lng: Double) {
        nearbyJob?.cancel()
        nearbyJob = viewModelScope.launch {
            _state.update { it.copy(checkingNearby = true) }
            delay(300)
            val r = rules.duplicatePlaceRadiusMeters
            val near = runCatching {
                dishes.getPlacePins().first().filter {
                    GeoUtils.distanceMeters(lat, lng, it.location.latitude, it.location.longitude) <= r
                }
            }.getOrDefault(emptyList())
            _state.update { it.copy(nearbyDuplicates = near, checkingNearby = false) }
        }
    }

    fun goToForm() = _state.update { it.copy(step = SuggestionStep.FORM, message = null) }
    fun backToMap() = _state.update { it.copy(step = SuggestionStep.MAP, message = null) }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setAddress(v: String) = _state.update { it.copy(address = v) }
    fun setDishId(id: String) = _state.update { it.copy(selectedDishId = id) }
    fun setPriceText(v: String) = _state.update { it.copy(priceText = v.filter(Char::isDigit)) }
    fun setOpenHours(v: String) = _state.update { it.copy(openHours = v) }
    fun setPhoto(uri: Uri?) = _state.update { it.copy(photoUri = uri) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    fun submit() {
        val s = _state.value
        val err = when {
            s.region?.id.isNullOrBlank() -> "Chưa chọn vùng"
            s.name.isBlank() -> "Nhập tên quán"
            s.address.isBlank() -> "Nhập địa chỉ"
            s.selectedDishId.isNullOrBlank() -> "Chọn món chính"
            else -> null
        }
        if (err != null) {
            _state.update { it.copy(message = err) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, message = null) }
            var photoUrl: String? = null
            s.photoUri?.let { uri ->
                _state.update { it.copy(uploading = true) }
                val up = uploader.upload(uri)
                _state.update { it.copy(uploading = false) }
                up.onFailure { e ->
                    _state.update { it.copy(submitting = false, message = "Lỗi ảnh: ${e.message}") }
                    return@launch
                }
                photoUrl = up.getOrNull()
            }

            val input = NewSuggestion(
                regionId = s.region!!.id,
                dishId = s.selectedDishId!!,
                name = s.name.trim(),
                address = s.address.trim(),
                location = LatLng(s.lat, s.lng),
                price = s.priceText.toLongOrNull(),
                openHours = s.openHours.ifBlank { null },
                photoUrl = photoUrl,
            )
            val result = suggestions.submit(input)
            _state.update {
                it.copy(
                    submitting = false,
                    message = result.fold(
                        onSuccess = { "Đã gửi đề xuất (chờ duyệt)" },
                        onFailure = { e -> e.message ?: "Gửi thất bại" },
                    ),
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SuggestionViewModel(
                    c.suggestionRepository, c.dishRepository, c.areaRepository,
                    c.imageUploader, c.businessRules,
                )
            }
        }
    }
}