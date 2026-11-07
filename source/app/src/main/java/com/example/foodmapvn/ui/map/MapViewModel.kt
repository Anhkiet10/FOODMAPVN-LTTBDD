package com.example.foodmapvn.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.location.NavigationRouteManager
import com.example.foodmapvn.data.location.RouteDestination
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.GeoUtils
import com.example.foodmapvn.domain.ProvinceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

enum class LocationStatus { Idle, Locating, Found, Denied, Unavailable }

data class ActiveNavigationRoute(
    val startLocation: LatLng,
    val destination: RouteDestination,
    val distanceText: String,
    val estimatedTimeText: String,
    val routePoints: List<LatLng>,
    val mode: String = "driving",
)

/** Khung mà tâm bản đồ được phép kéo tới (độ): giữ bản đồ trong khu vực. */
data class MapBounds(val north: Double, val south: Double, val east: Double, val west: Double) {
    companion object {
        fun around(center: LatLng, halfSpanDeg: Double) = MapBounds(
            north = center.latitude + halfSpanDeg,
            south = center.latitude - halfSpanDeg,
            east = center.longitude + halfSpanDeg,
            west = center.longitude - halfSpanDeg,
        )

        /** Khung chứa cả 2 điểm (vị trí hiện tại & điểm đến). */
        fun forRoute(p1: LatLng, p2: LatLng): MapBounds {
            val minLat = minOf(p1.latitude, p2.latitude) - 0.05
            val maxLat = maxOf(p1.latitude, p2.latitude) + 0.05
            val minLng = minOf(p1.longitude, p2.longitude) - 0.05
            val maxLng = maxOf(p1.longitude, p2.longitude) + 0.05
            return MapBounds(north = maxLat, south = minLat, east = maxLng, west = minLng)
        }

        /** Khung của tỉnh, nới rộng nếu cần. */
        fun forProvince(province: Province, include: LatLng? = null): MapBounds {
            val c = province.center
            var half = MapUiState.PROVINCE_HALF_SPAN_DEG
            if (include != null) {
                half = max(half, abs(include.latitude - c.latitude) + 0.1)
                half = max(half, abs(include.longitude - c.longitude) + 0.1)
            }
            return around(c, half)
        }
    }
}

data class MapUiState(
    val area: Province = VietnamProvinces.DEFAULT,
    val status: LocationStatus = LocationStatus.Idle,
    val myLocation: LatLng? = null,
    val provinces: List<Province> = VietnamProvinces.all,
    val center: LatLng = VietnamProvinces.DEFAULT.center,
    val zoom: Double = PROVINCE_ZOOM,
    val bounds: MapBounds = MapBounds.forProvince(VietnamProvinces.DEFAULT),
    val cameraVersion: Int = 0,
    val activeRoute: ActiveNavigationRoute? = null,
) {
    companion object {
        const val PROVINCE_HALF_SPAN_DEG = 0.7
        const val LOCATION_ZOOM = 13.0
        const val ROUTE_ZOOM = 13.5
        const val PROVINCE_ZOOM = 11.5
    }
}

class MapViewModel(
    private val areaRepository: AreaRepository,
    private val locationRepository: LocationRepository,
    private val navigationRouteManager: NavigationRouteManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var pendingFocus: LatLng? = null

    init {
        viewModelScope.launch {
            areaRepository.selectedArea.collect { selected ->
                val province = selected ?: VietnamProvinces.DEFAULT
                val focus = pendingFocus
                pendingFocus = null
                _uiState.update {
                    it.copy(
                        area = province,
                        center = focus ?: province.center,
                        zoom = if (focus != null) MapUiState.LOCATION_ZOOM else MapUiState.PROVINCE_ZOOM,
                        bounds = MapBounds.forProvince(province, include = focus),
                        cameraVersion = it.cameraVersion + 1,
                    )
                }
            }
        }

        viewModelScope.launch {
            navigationRouteManager.activeDestination.collect { dest ->
                if (dest != null) {
                    computeRouteToDestination(dest)
                } else {
                    _uiState.update { it.copy(activeRoute = null) }
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean = locationRepository.hasPermission()

    fun showMyLocationDot() {
        viewModelScope.launch {
            locationRepository.currentLocation()?.let { me -> _uiState.update { it.copy(myLocation = me) } }
        }
    }

    private suspend fun computeRouteToDestination(dest: RouteDestination, mode: String = "driving") {
        _uiState.update { it.copy(status = LocationStatus.Locating) }
        var userLoc = locationRepository.currentLocation()

        // Tính khoảng cách giữa vị trí xuất phát và quán
        val distToDest = userLoc?.let { GeoUtils.distanceMeters(it.latitude, it.longitude, dest.location.latitude, dest.location.longitude) } ?: Double.MAX_VALUE

        // Nếu chưa có GPS hoặc GPS ở tỉnh khác quá xa (> 25 km):
        // Đặt điểm xuất phát loanh quanh trong cùng Thành phố / Khu vực của quán (cách quán ~1.2 km)
        if (userLoc == null || distToDest > 25_000.0) {
            userLoc = LatLng(
                dest.location.latitude - 0.008,
                dest.location.longitude - 0.008
            )
        }

        val roadResult = com.example.foodmapvn.data.location.RoadRoutingService.fetchRoute(userLoc, dest.location, mode)
        val meters = roadResult?.distanceMeters ?: GeoUtils.distanceMeters(userLoc.latitude, userLoc.longitude, dest.location.latitude, dest.location.longitude)
        val distText = GeoUtils.formatDistance(meters)

        val durationSecs = roadResult?.durationSeconds ?: (if (mode == "walking") meters / 1.3 else meters / 8.3)
        val durationMins = max(1, (durationSecs / 60.0).toInt())
        val timeText = "$durationMins phút ($distText)"

        val routePoints = roadResult?.points ?: generateRoutePoints(userLoc, dest.location)
        val centerLat = (userLoc.latitude + dest.location.latitude) / 2.0
        val centerLng = (userLoc.longitude + dest.location.longitude) / 2.0
        val routeBounds = MapBounds.forRoute(userLoc, dest.location)

        val route = ActiveNavigationRoute(
            startLocation = userLoc,
            destination = dest,
            distanceText = distText,
            estimatedTimeText = timeText,
            routePoints = routePoints,
            mode = mode,
        )

        _uiState.update {
            it.copy(
                status = LocationStatus.Found,
                myLocation = userLoc,
                activeRoute = route,
                center = LatLng(centerLat, centerLng),
                zoom = MapUiState.ROUTE_ZOOM,
                bounds = routeBounds,
                cameraVersion = it.cameraVersion + 1,
            )
        }
    }

    fun setTravelMode(mode: String) {
        val currentDest = navigationRouteManager.activeDestination.value ?: return
        viewModelScope.launch {
            computeRouteToDestination(currentDest, mode)
        }
    }

    fun clearActiveRoute() {
        navigationRouteManager.clearRoute()
    }

    private fun generateRoutePoints(start: LatLng, end: LatLng): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(start)

        // Tạo tuyến đường ngã rẽ vuông góc theo phố (Street-grid fallback)
        val corner = LatLng(start.latitude, end.longitude)

        for (i in 1..4) {
            val t = i / 4.0
            points.add(LatLng(start.latitude, start.longitude + t * (corner.longitude - start.longitude)))
        }
        for (i in 1..4) {
            val t = i / 4.0
            points.add(LatLng(corner.latitude + t * (end.latitude - corner.latitude), end.longitude))
        }

        return points
    }

    fun goToMyLocation() {
        _uiState.update { it.copy(status = LocationStatus.Locating) }
        viewModelScope.launch {
            val location = locationRepository.currentLocation()
            if (location == null) {
                _uiState.update { it.copy(status = LocationStatus.Unavailable) }
                return@launch
            }
            val adminArea = locationRepository.areaName(location)
            val province = ProvinceLocator.locate(location, adminArea, VietnamProvinces.all) ?: return@launch
            _uiState.update { it.copy(status = LocationStatus.Found, myLocation = location) }
            if (province == areaRepository.selectedArea.value) {
                _uiState.update {
                    it.copy(
                        center = location,
                        zoom = MapUiState.LOCATION_ZOOM,
                        bounds = MapBounds.forProvince(province, include = location),
                        cameraVersion = it.cameraVersion + 1,
                    )
                }
            } else {
                pendingFocus = location
                areaRepository.select(province)
            }
        }
    }

    fun selectProvince(province: Province) {
        if (province == areaRepository.selectedArea.value) {
            _uiState.update { it.copy(center = province.center, zoom = MapUiState.PROVINCE_ZOOM, cameraVersion = it.cameraVersion + 1) }
        } else {
            areaRepository.select(province)
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = LocationStatus.Denied) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                MapViewModel(container.areaRepository, container.locationRepository, container.navigationRouteManager)
            }
        }
    }
}
