package com.example.foodmapvn.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.location.NavigationRouteManager
import com.example.foodmapvn.data.location.RoadRoutingService
import com.example.foodmapvn.data.location.RouteDestination
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.domain.ExploreFilter
import com.example.foodmapvn.domain.GeoUtils
import com.example.foodmapvn.domain.OpeningHours
import kotlinx.coroutines.flow.catch
import java.util.Calendar
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.ProvinceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

/** Thẻ tóm tắt quán khi bấm ghim. */
data class PlaceCard(
    val place: PlaceSummary,
    /** Khoảng cách từ vị trí của tôi, vd. "350 m"; null = chưa có vị trí. */
    val distance: String?,
    /** true/false = đang mở/đã đóng; null = chưa có giờ mở cửa. */
    val isOpen: Boolean?,
)

/** Mức uy tín để tô màu ghim: cao (≥ 80), trung bình (60–79), thấp (< 60). */
enum class TrustLevel(val min: Int) { HIGH(80), MEDIUM(60), LOW(0);
    companion object { fun of(score: Int) = entries.first { score >= it.min } }
}

enum class LocationStatus { Idle, Locating, Found, Denied, Unavailable }

/** Khung mà tâm bản đồ được phép kéo tới (độ): giữ bản đồ trong khu vực, không tải dữ liệu bản đồ nơi khác. */
data class MapBounds(val north: Double, val south: Double, val east: Double, val west: Double) {
    companion object {
        /** Khung chứa cả điểm xuất phát và điểm đến (khi chỉ đường). */
        fun forRoute(p1: LatLng, p2: LatLng): MapBounds = MapBounds(
            north = maxOf(p1.latitude, p2.latitude) + 0.05,
            south = minOf(p1.latitude, p2.latitude) - 0.05,
            east = maxOf(p1.longitude, p2.longitude) + 0.05,
            west = minOf(p1.longitude, p2.longitude) - 0.05,
        )

        fun around(center: LatLng, halfSpanDeg: Double) = MapBounds(
            north = center.latitude + halfSpanDeg,
            south = center.latitude - halfSpanDeg,
            east = center.longitude + halfSpanDeg,
            west = center.longitude - halfSpanDeg,
        )

        /** Khung của tỉnh, nới rộng nếu cần để chứa cả điểm [include] (vd. vị trí ở rìa tỉnh). */
        fun forProvince(province: Province, include: LatLng? = null): MapBounds {
            val c = province.center
            var half = MapUiState.PROVINCE_HALF_SPAN_DEG
            if (include != null) {
                half = max(half, kotlin.math.abs(include.latitude - c.latitude) + 0.1)
                half = max(half, kotlin.math.abs(include.longitude - c.longitude) + 0.1)
            }
            return around(c, half)
        }
    }
}

/** Tuyến đường đang chỉ dẫn (từ màn Chi tiết quán -> "Chỉ đường", Người 2). */
data class ActiveNavigationRoute(
    val startLocation: LatLng,
    val destination: RouteDestination,
    val distanceText: String,
    val estimatedTimeText: String,
    val routePoints: List<LatLng>,
    /** "driving" hoặc "walking". */
    val mode: String = "driving",
    /** false = không lấy được đường thực tế, đang vẽ đường thẳng ước lượng. */
    val isRoadRoute: Boolean = true,
)

data class MapUiState(
    /** Tỉnh/thành đang chọn (dùng chung cả app). */
    val area: Province = VietnamProvinces.DEFAULT,
    val status: LocationStatus = LocationStatus.Idle,
    val myLocation: LatLng? = null,
    val provinces: List<Province> = VietnamProvinces.all,
    val center: LatLng = VietnamProvinces.DEFAULT.center,
    val zoom: Double = PROVINCE_ZOOM,
    val bounds: MapBounds = MapBounds.forProvince(VietnamProvinces.DEFAULT),
    /** Tăng mỗi lần cần đưa camera về [center] (kể cả chọn lại đúng tỉnh đang xem). */
    val cameraVersion: Int = 0,
    /** Quán của khu vực đang chọn (ghim trên bản đồ, UC-07). */
    val places: List<PlaceSummary> = emptyList(),
    /** Quán đang được chọn (bấm vào ghim) -> hiện thẻ tóm tắt. */
    val selectedPlace: PlaceCard? = null,
    /** Đang chỉ đường tới một quán; null = không. */
    val activeRoute: ActiveNavigationRoute? = null,
    /** Thông báo ngắn cho người dùng (vd. cần bật vị trí để chỉ đường). */
    val message: String? = null,
) {
    companion object {
        /** Nửa cạnh khung khi xem một tỉnh/thành: 0,7° ≈ 78 km (tỉnh sau sáp nhập khá rộng). */
        const val PROVINCE_HALF_SPAN_DEG = 0.7
        const val LOCATION_ZOOM = 13.0
        const val ROUTE_ZOOM = 13.5
        const val PROVINCE_ZOOM = 11.5
    }
}

/**
 * Bản đồ của khu vực đang chọn (AreaRepository, dùng chung cả app):
 * - Đổi tỉnh ở bảng chọn → đổi khu vực cho CẢ APP, bản đồ chuyển sang tỉnh đó.
 * - "Vị trí của tôi" → suy ra tỉnh đang đứng, đặt làm khu vực, đưa bản đồ về vị trí.
 * - Ghim quán của khu vực, màu theo uy tín, viền đỏ nếu bị cảnh báo giá; bấm ghim -> thẻ tóm tắt -> Chi tiết quán.
 */
class MapViewModel(
    private val areaRepository: AreaRepository,
    private val dishRepository: DishRepository,
    private val locationRepository: LocationRepository,
    private val navigationRouteManager: NavigationRouteManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Vị trí cần đưa camera tới khi khu vực đổi do bấm "Vị trí của tôi". */
    private var pendingFocus: LatLng? = null

    init {
        startRouteListener()
        // "Xem quán trên bản đồ": đợi danh sách quán của vùng có quán đó rồi mở thẻ quán
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                navigationRouteManager.focusPlaceId,
                _uiState.map { it.places }.distinctUntilChanged(),
            ) { id, places -> id to places }.collect { (id, places) ->
                if (id != null && places.any { it.id == id }) {
                    selectPlace(id)
                    navigationRouteManager.consumeFocusPlace()
                }
            }
        }
        // Quán của khu vực đang chọn (DishRepository tự đổi theo tỉnh)
        viewModelScope.launch {
            dishRepository.getPlaces().catch { emit(emptyList()) }.collect { places ->
                _uiState.update { s ->
                    // Quán đang chọn không còn trong vùng mới -> đóng thẻ
                    val keep = s.selectedPlace?.takeIf { sel -> places.any { it.id == sel.place.id } }
                    s.copy(places = places, selectedPlace = keep)
                }
            }
        }
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
    }

    fun hasLocationPermission(): Boolean = locationRepository.hasPermission()

    // ===== Chỉ đường (Người 2) =====

    private fun startRouteListener() {
        viewModelScope.launch {
            navigationRouteManager.activeDestination.collect { dest ->
                if (dest != null) computeRoute(dest, _uiState.value.activeRoute?.mode ?: "driving")
                else _uiState.update { it.copy(activeRoute = null) }
            }
        }
    }

    /** Tính đường từ vị trí THẬT của người dùng tới quán; chưa có vị trí thì báo, không tự đặt điểm xuất phát. */
    private suspend fun computeRoute(dest: RouteDestination, mode: String) {
        _uiState.update { it.copy(status = LocationStatus.Locating, selectedPlace = null) }
        val me = locationRepository.currentLocation()
        if (me == null) {
            navigationRouteManager.clearRoute()
            _uiState.update {
                it.copy(
                    status = if (locationRepository.hasPermission()) LocationStatus.Unavailable else LocationStatus.Denied,
                    message = "Bật định vị để chỉ đường tới ${dest.placeName}",
                )
            }
            return
        }
        val road = RoadRoutingService.fetchRoute(me, dest.location, mode)
        val meters = road?.distanceMeters
            ?: GeoUtils.distanceMeters(me.latitude, me.longitude, dest.location.latitude, dest.location.longitude)
        val seconds = road?.durationSeconds ?: (if (mode == "walking") meters / 1.3 else meters / 8.3)
        val minutes = maxOf(1, (seconds / 60.0).toInt())
        val distText = GeoUtils.formatDistance(meters)
        val route = ActiveNavigationRoute(
            startLocation = me,
            destination = dest,
            distanceText = distText,
            estimatedTimeText = "$minutes phút ($distText)",
            // Không lấy được đường thực tế (mất mạng…) -> vẽ đường thẳng và ghi rõ là ước lượng
            routePoints = road?.points ?: listOf(me, dest.location),
            mode = mode,
            isRoadRoute = road != null,
        )
        _uiState.update {
            it.copy(
                status = LocationStatus.Found,
                myLocation = me,
                activeRoute = route,
                center = LatLng((me.latitude + dest.location.latitude) / 2, (me.longitude + dest.location.longitude) / 2),
                zoom = MapUiState.ROUTE_ZOOM,
                bounds = MapBounds.forRoute(me, dest.location),
                cameraVersion = it.cameraVersion + 1,
            )
        }
    }

    fun setTravelMode(mode: String) {
        val dest = navigationRouteManager.activeDestination.value ?: return
        viewModelScope.launch { computeRoute(dest, mode) }
    }

    fun clearActiveRoute() {
        navigationRouteManager.clearRoute()
        // Trả bản đồ về khung của tỉnh đang chọn
        _uiState.update { it.copy(bounds = MapBounds.forProvince(it.area, include = it.myLocation), cameraVersion = it.cameraVersion + 1) }
    }

    /** Nút "Chỉ đường" trên thẻ quán (Figma): vẽ đường tới quán ngay trên bản đồ. */
    fun startDirections(place: com.example.foodmapvn.data.model.PlaceSummary) {
        navigationRouteManager.startNavigation(place.id, place.name, place.address, place.location)
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** Mở bản đồ: chỉ lấy vị trí để vẽ chấm xanh, KHÔNG đổi khu vực đang chọn. */
    fun showMyLocationDot() {
        viewModelScope.launch {
            locationRepository.currentLocation()?.let { me -> _uiState.update { it.copy(myLocation = me) } }
        }
    }

    /** Bấm "Vị trí của tôi": suy ra tỉnh đang đứng, đặt làm khu vực của cả app, đưa camera về vị trí. */
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
                // Cùng tỉnh: chỉ đưa camera về vị trí
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

    /** Chọn tỉnh ở bảng chọn: đổi khu vực cho cả app. */
    fun selectProvince(province: Province) {
        if (province == areaRepository.selectedArea.value) {
            _uiState.update { it.copy(center = province.center, zoom = MapUiState.PROVINCE_ZOOM, cameraVersion = it.cameraVersion + 1) }
        } else {
            areaRepository.select(province)
        }
    }

    /** Bấm vào ghim quán ([placeId]) hoặc vào chỗ trống trên bản đồ (null) để đóng thẻ. */
    fun selectPlace(placeId: String?) {
        val s = _uiState.value
        val place = placeId?.let { id -> s.places.firstOrNull { it.id == id } }
        _uiState.update {
            it.copy(
                selectedPlace = place?.let { p ->
                    PlaceCard(
                        place = p,
                        distance = ExploreFilter.distanceMeters(p, s.myLocation)?.let(GeoUtils::formatDistance),
                        isOpen = OpeningHours.isOpen(p.openTime, p.closeTime, nowMinutes()),
                    )
                }
            )
        }
    }

    private fun nowMinutes(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = LocationStatus.Denied) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                MapViewModel(container.areaRepository, container.dishRepository, container.locationRepository, container.navigationRouteManager)
            }
        }
    }
}
