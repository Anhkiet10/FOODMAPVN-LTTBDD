package com.example.foodmapvn.data.location

import com.example.foodmapvn.data.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RouteDestination(
    val placeId: String,
    val placeName: String,
    val address: String,
    val location: LatLng,
)

class NavigationRouteManager {
    private val _activeDestination = MutableStateFlow<RouteDestination?>(null)
    val activeDestination: StateFlow<RouteDestination?> = _activeDestination.asStateFlow()

    private val _requestedTabSwitch = MutableStateFlow(false)
    val requestedTabSwitch: StateFlow<Boolean> = _requestedTabSwitch.asStateFlow()

    fun startNavigation(placeId: String, placeName: String, address: String, location: LatLng) {
        _activeDestination.value = RouteDestination(placeId, placeName, address, location)
        _requestedTabSwitch.value = true
    }

    fun clearRoute() {
        _activeDestination.value = null
    }

    fun consumeTabSwitch() {
        _requestedTabSwitch.value = false
    }
}
