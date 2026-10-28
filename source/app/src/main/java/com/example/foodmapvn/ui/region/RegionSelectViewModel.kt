package com.example.foodmapvn.ui.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.LocationRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.ProvinceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocateStatus { Idle, Locating, Denied, Failed }

data class RegionSelectUiState(
    /** true = lần đầu mở app (chưa có khu vực): bắt buộc xin quyền vị trí, không có nút quay lại. */
    val isOnboarding: Boolean = true,
    val status: LocateStatus = LocateStatus.Idle,
    val selectedId: String? = null,
    val provinces: List<Province> = VietnamProvinces.all,
    /** true = đã chọn xong khu vực -> màn rời đi. */
    val finished: Boolean = false,
)

/**
 * UC-04 Chọn vùng.
 * Lần đầu: xin quyền vị trí -> suy ra tỉnh/thành -> lưu làm khu vực chung; từ chối -> tự chọn trong 34 tỉnh.
 * Mở lại từ Khám phá: đổi khu vực bằng vị trí hoặc chọn tay.
 */
class RegionSelectViewModel(
    private val areaRepository: AreaRepository,
    private val locationRepository: LocationRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RegionSelectUiState(
            isOnboarding = areaRepository.selectedArea.value == null,
            selectedId = areaRepository.selectedArea.value?.id,
        )
    )
    val uiState: StateFlow<RegionSelectUiState> = _uiState.asStateFlow()

    fun hasLocationPermission(): Boolean = locationRepository.hasPermission()

    /** Lấy vị trí -> tỉnh/thành -> lưu. Gọi sau khi đã có quyền. */
    fun useMyLocation() {
        _uiState.update { it.copy(status = LocateStatus.Locating) }
        viewModelScope.launch {
            val location = locationRepository.currentLocation()
            val province = location?.let {
                ProvinceLocator.locate(it, locationRepository.areaName(it), VietnamProvinces.all)
            }
            if (province == null) {
                _uiState.update { it.copy(status = LocateStatus.Failed) }
            } else {
                select(province, source = "location")
            }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(status = LocateStatus.Denied) }
    }

    fun select(province: Province, source: String = "manual") {
        areaRepository.select(province)
        analytics.selectRegion(province.id, source)
        _uiState.update { it.copy(selectedId = province.id, status = LocateStatus.Idle, finished = true) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                RegionSelectViewModel(container.areaRepository, container.locationRepository, container.analytics)
            }
        }
    }
}
