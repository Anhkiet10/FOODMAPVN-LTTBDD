package com.example.foodmapvn.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.local.VietnamProvinces
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
) {
    companion object {
        /** Nửa cạnh khung khi xem một tỉnh/thành: 0,7° ≈ 78 km (tỉnh sau sáp nhập khá rộng). */
        const val PROVINCE_HALF_SPAN_DEG = 0.7
        const val LOCATION_ZOOM = 13.0
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Vị trí cần đưa camera tới khi khu vực đổi do bấm "Vị trí của tôi". */
    private var pendingFocus: LatLng? = null

    init {
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
                MapViewModel(container.areaRepository, container.dishRepository, container.locationRepository)
            }
        }
    }
}
