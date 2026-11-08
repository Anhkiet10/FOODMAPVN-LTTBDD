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

    /** Chỉ chuyển sang tab Bản đồ (không vẽ đường), vd. nút "Xem N quán trên bản đồ" ở Chi tiết đặc sản. */
    fun requestMapTab() {
        _requestedTabSwitch.value = true
    }

    private val _focusPlaceId = MutableStateFlow<String?>(null)
    /** Quán cần chọn sẵn khi mở tab Bản đồ (nút "Xem quán trên bản đồ"); null = không có. */
    val focusPlaceId: StateFlow<String?> = _focusPlaceId.asStateFlow()

    /** Chuyển sang tab Bản đồ và mở thẻ của quán [placeId] (bỏ đường chỉ dẫn đang có). */
    fun showPlaceOnMap(placeId: String) {
        _activeDestination.value = null
        _focusPlaceId.value = placeId
        _requestedTabSwitch.value = true
    }

    fun consumeFocusPlace() {
        _focusPlaceId.value = null
    }

    fun consumeTabSwitch() {
        _requestedTabSwitch.value = false
    }
}
