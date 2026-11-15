package com.example.foodmapvn.ui.admin

import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
import com.example.foodmapvn.data.model.AdminReport
import com.example.foodmapvn.data.model.AdminReportStatus
import com.example.foodmapvn.data.model.AdminStats
import com.example.foodmapvn.data.model.AdminSuggestion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.NearbyPlace
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.data.repository.AdminRepository
import com.example.foodmapvn.data.repository.BusinessRepository
import com.example.foodmapvn.data.repository.ReportRepository
import com.example.foodmapvn.data.repository.SuggestionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái + thao tác cho khu vực Admin (Người 3 – Nhật).
 * Được tạo bằng remember ở FoodMapApp (route ADMIN); gọi [clear] khi rời màn Admin.
 * [suggestionRepository] và [reportRepository] giữ lại để khớp lời gọi ở FoodMapApp.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val adminRepository: AdminRepository,
    @Suppress("unused") private val suggestionRepository: SuggestionRepository,
    @Suppress("unused") private val reportRepository: ReportRepository,
    private val businessRepository: BusinessRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val keep = SharingStarted.WhileSubscribed(5_000)

    // ----- Tổng quan -----
    val stats: StateFlow<AdminStats> = adminRepository.getStats().stateIn(scope, keep, AdminStats())

    val adminName: StateFlow<String> = adminRepository.getAdminName().stateIn(scope, keep, "")

    // ----- Duyệt quán -----
    val suggestionTab = MutableStateFlow(SuggestionStatus.PENDING)
    val suggestions: StateFlow<List<AdminSuggestion>> = suggestionTab
        .flatMapLatest { adminRepository.getSuggestions(it) }
        .stateIn(scope, keep, emptyList())
    val pendingBusinesses: StateFlow<List<Business>> =
        businessRepository.getPendingBusinesses().stateIn(scope, keep, emptyList())

    // ----- Báo cáo -----
    val reportTab = MutableStateFlow(AdminReportStatus.PENDING)
    val reports: StateFlow<List<AdminReport>> = reportTab
        .flatMapLatest { adminRepository.getReports(it) }
        .stateIn(scope, keep, emptyList())

    // ----- Đặc sản & vùng -----
    val regions: StateFlow<List<AdminRegion>> = adminRepository.getRegions().stateIn(scope, keep, emptyList())
    val selectedRegionId = MutableStateFlow<String?>(null)
    val dishes: StateFlow<List<AdminDish>> = selectedRegionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else adminRepository.getDishes(id) }
        .stateIn(scope, keep, emptyList())

    // ----- Người dùng -----
    val users: StateFlow<List<AdminUser>> = adminRepository.getUsers().stateIn(scope, keep, emptyList())

    /** Thông báo ngắn (thành công / lỗi) hiển thị bằng Snackbar. */
    val message = MutableStateFlow<String?>(null)
    fun clearMessage() { message.value = null }

    fun selectSuggestionTab(s: SuggestionStatus) { suggestionTab.value = s }
    fun selectReportTab(s: AdminReportStatus) { reportTab.value = s }
    fun selectRegion(id: String?) { selectedRegionId.value = id }

    private fun act(ok: String, block: suspend () -> Result<Unit>) {
        scope.launch {
            block().onSuccess { message.value = ok }
                .onFailure { message.value = it.message ?: "Đã có lỗi xảy ra" }
        }
    }

    fun approve(item: AdminSuggestion) = act("Đã duyệt \"${item.suggestion.name}\"") { adminRepository.approveSuggestion(item) }
    fun reject(item: AdminSuggestion, reason: String) = act("Đã từ chối đề xuất") { adminRepository.rejectSuggestion(item, reason) }

    fun reviewBusiness(ownerId: String, approve: Boolean, reason: String? = null) =
        act(if (approve) "Đã duyệt doanh nghiệp" else "Đã từ chối doanh nghiệp") {
            businessRepository.review(ownerId, approve, reason)
        }

    fun dismiss(r: AdminReport) = act("Đã bỏ qua báo cáo") { adminRepository.dismissReport(r) }
    fun hideReview(r: AdminReport) = act("Đã ẩn đánh giá") { adminRepository.hideReportedReview(r) }
    fun markClosed(r: AdminReport) = act("Đã đánh dấu quán đóng cửa") { adminRepository.markPlaceClosed(r) }
    fun resolve(r: AdminReport) = act("Đã xử lý báo cáo") { adminRepository.resolveReport(r) }

    fun setDishVisible(d: AdminDish, visible: Boolean) =
        act(if (visible) "Đã hiện món" else "Đã ẩn món") { adminRepository.setDishVisible(d.id, visible) }

    fun addDish(name: String, unit: String) {
        val region = selectedRegionId.value ?: return
        act("Đã thêm món") { adminRepository.addDish(region, name, unit) }
    }

    fun setUserLocked(u: AdminUser, locked: Boolean) =
        act(if (locked) "Đã khóa tài khoản" else "Đã mở khóa tài khoản") { adminRepository.setUserLocked(u.uid, locked) }

    // ----- Chi tiết đề xuất: quán trùng gần đó + Sửa -----
    val nearby = MutableStateFlow<List<NearbyPlace>?>(null)   // null = đang kiểm tra
    fun loadNearby(item: AdminSuggestion) {
        nearby.value = null
        scope.launch { nearby.value = adminRepository.findNearbyPlaces(item.suggestion.regionId, item.suggestion.location) }
    }
    fun editSuggestion(item: AdminSuggestion, name: String, address: String, price: Long?, openHours: String?) =
        act("Đã cập nhật đề xuất") { adminRepository.updateSuggestion(item, name, address, price, openHours) }

    // ----- Đặc sản & vùng: sửa món, thêm/sửa vùng -----
    fun editDish(d: AdminDish, name: String, unit: String) = act("Đã cập nhật món") { adminRepository.updateDish(d.id, name, unit) }
    fun addRegion(name: String, lat: Double, lng: Double) = act("Đã thêm vùng") { adminRepository.addRegion(name, lat, lng) }
    fun editRegion(r: AdminRegion, name: String, visible: Boolean) = act("Đã cập nhật vùng") { adminRepository.updateRegion(r.id, name, visible) }

    fun clear() { scope.cancel() }
}