package com.example.foodmapvn.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.CheckInResult
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CheckInUiState {
    data object Idle : CheckInUiState
    data object Checking : CheckInUiState
    data class Success(val distanceMeters: Double) : CheckInUiState
    data class TooFar(val distanceMeters: Double, val maxMeters: Double) : CheckInUiState
    data class TooSoon(val nextAllowedAtMillis: Long) : CheckInUiState
    data class Error(val message: String) : CheckInUiState
}

class CheckInViewModel(
    private val placeId: String,
    private val placeRepository: PlaceRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    val placeDetail: StateFlow<PlaceDetail?> = placeRepository.getPlaceDetail(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    fun checkInWithLocation(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            _uiState.value = CheckInUiState.Checking

            val place = placeDetail.value
            val userLocation = if (lat != null && lon != null) {
                LatLng(lat, lon)
            } else {
                locationRepository.currentLocation() ?: place?.location
            }

            if (userLocation == null) {
                _uiState.value = CheckInUiState.Error("Không thể lấy vị trí GPS hiện tại. Vui lòng bật định vị.")
                return@launch
            }

            val result = placeRepository.performCheckIn(placeId, userLocation.latitude, userLocation.longitude)
            result.onSuccess { res ->
                _uiState.value = when (res) {
                    is CheckInResult.Valid -> CheckInUiState.Success(res.distanceMeters)
                    is CheckInResult.TooFar -> CheckInUiState.TooFar(res.distanceMeters, res.maxMeters)
                    is CheckInResult.TooSoon -> CheckInUiState.TooSoon(res.nextAllowedAtMillis)
                }
            }.onFailure { err ->
                _uiState.value = CheckInUiState.Error(err.message ?: "Lỗi kiểm tra check-in")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val placeId = checkNotNull(savedStateHandle.get<String>(Routes.ARG_PLACE_ID))
                val container = appContainer()
                CheckInViewModel(
                    placeId = placeId,
                    placeRepository = container.placeRepository,
                    locationRepository = container.locationRepository,
                )
            }
        }
    }
}
