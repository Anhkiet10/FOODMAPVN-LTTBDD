package com.example.foodmapvn.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MapUiState(
    val pins: List<PlacePin> = emptyList(),
    val myLocation: LatLng? = null,
    /** Tâm bản đồ mặc định: trung tâm Huế. */
    val center: LatLng = LatLng(16.4637, 107.5909),
)

class MapViewModel(
    dishRepository: DishRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val myLocation = MutableStateFlow<LatLng?>(null)

    val uiState: StateFlow<MapUiState> = combine(dishRepository.getPlacePins(), myLocation) { pins, location ->
        MapUiState(pins = pins, myLocation = location, center = location ?: MapUiState().center)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    /** Gọi sau khi người dùng cấp quyền vị trí. */
    fun refreshMyLocation() {
        viewModelScope.launch { myLocation.value = locationRepository.currentLocation() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                MapViewModel(container.dishRepository, container.locationRepository)
            }
        }
    }
}
