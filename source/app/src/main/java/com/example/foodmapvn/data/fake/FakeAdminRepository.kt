package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
import com.example.foodmapvn.data.model.AdminReport
import com.example.foodmapvn.data.model.AdminReportStatus
import com.example.foodmapvn.data.model.AdminStats
import com.example.foodmapvn.data.model.AdminSuggestion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.NearbyPlace
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.data.repository.AdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Dữ liệu giả cho màn Admin (chạy khi chưa có google-services.json). Mẫu theo Figma. */
class FakeAdminRepository : AdminRepository {

    private fun sug(id: String, name: String, dish: String, by: String, price: Long, hours: String, region: String = "Huế") =
        AdminSuggestion(
            suggestion = PlaceSuggestion(
                id = id, userId = "u_$id", regionId = "hue", dishId = "d_$id", name = name,
                address = "12 Lê Lợi, TP Huế", location = LatLng(16.4637, 107.5909),
                price = price, openHours = hours, photoUrl = null,
                status = SuggestionStatus.PENDING, rejectReason = null, createdAt = "13/11/2026",
            ),
            dishName = dish, regionName = region, proposerName = by,
        )

    private val suggestions = MutableStateFlow(
        listOf(
            sug("s1", "Bún bò Mẹ Kéo", "Bún bò Huế", "Minh Thư", 38000, "06:00 - 13:00"),
            sug("s2", "Chè Hẻm Cô Ba", "Chè Huế", "Quốc Huy", 15000, "14:00 - 21:00"),
            sug("s3", "Bún bò Mụ Rớt", "Bún bò Huế", "Kiệt Nguyễn", 40000, "05:30 - 11:00"),
            sug("s4", "Bún riêu Cô Sáu", "Bún riêu", "Lan Anh", 35000, "06:00 - 12:00", "Cần Thơ"),
            sug("s5", "Cơm hến Bà Nở", "Cơm hến", "Minh Khoa", 20000, "06:00 - 17:00"),
        )
    )
    private val handled = MutableStateFlow(listOf<AdminSuggestion>())

    private fun rep(id: String, type: String, target: String, title: String, quote: String, reason: String, by: String) =
        AdminReport(id, type, target, title, quote, reason, by, AdminReportStatus.PENDING, "Hôm nay")

    private val reports = MutableStateFlow(
        listOf(
            rep("r1", "REVIEW", "x1", "Hoàng Cung · Bún bò Huế", "Ngon nhất Huế, ai cũng nên đến, 5 sao!", "OTHER", "Quốc Huy"),
            rep("r2", "PLACE", "p2", "Chè Hẻm Cô Ba · Huế", "Quán đã nghỉ bán từ tháng trước.", "NOT_EXIST", "Lan Anh"),
            rep("r3", "PLACE", "p3", "Bánh khoái Lạc Thiện · Huế", "Giá trên app 30k, thực tế 45k.", "WRONG_PRICE", "Minh Thư"),
        )
    )

    private val regions = MutableStateFlow(listOf(AdminRegion("hue", "Huế", 5, 12), AdminRegion("ct", "Cần Thơ", 4, 9)))
    private val dishes = MutableStateFlow(
        listOf(
            "hue" to AdminDish("d1", "Bún bò Huế", "35 – 50k", 5, true),
            "hue" to AdminDish("d2", "Chè Huế", "10 – 20k", 3, true),
            "hue" to AdminDish("d3", "Cơm hến", "15 – 25k", 2, true),
            "hue" to AdminDish("d4", "Bánh canh Nam Phổ", "20 – 30k", 1, false),
            "ct" to AdminDish("d5", "Bún riêu", "30 – 40k", 4, true),
        )
    )

    private val users = MutableStateFlow(
        listOf(
            AdminUser("u1", "Minh Thư", "minhthu@gmail.com", false, false),
            AdminUser("u2", "Quốc Huy", "quochuy@gmail.com", false, true),
            AdminUser("u3", "Huy Hoàng", "huyhoang@gmail.com", false, false, 3),
            AdminUser("u4", "Minh Thuận", "minhthuan@gmail.com", false, false),
            AdminUser("u5", "Phong Quý", "phongquy@gmail.com", false, false),
        )
    )

    override fun getStats(): Flow<AdminStats> =
        combine(suggestions, reports, dishes, users) { s, r, d, u ->
            AdminStats(
                pendingSuggestions = s.size,
                newReports = r.count { it.status == AdminReportStatus.PENDING },
                warnedPlaces = 3,
                reviewsThisWeek = 50,
                regionCount = regions.value.size,
                pendingToday = 2, warnedRegions = 1, reviewsLastWeek = 40,
                dishCount = d.size,
                userCount = u.size,
            )
        }

    override fun getSuggestions(status: SuggestionStatus): Flow<List<AdminSuggestion>> =
        if (status == SuggestionStatus.PENDING) suggestions
        else handled.map { list -> list.filter { it.suggestion.status == status } }

    private fun moveSuggestion(item: AdminSuggestion, to: SuggestionStatus, reason: String?) {
        suggestions.update { list -> list.filterNot { it.suggestion.id == item.suggestion.id } }
        handled.update { it + item.copy(suggestion = item.suggestion.copy(status = to, rejectReason = reason)) }
    }

    override suspend fun approveSuggestion(item: AdminSuggestion): Result<Unit> {
        moveSuggestion(item, SuggestionStatus.APPROVED, null); return Result.success(Unit)
    }

    override suspend fun rejectSuggestion(item: AdminSuggestion, reason: String): Result<Unit> {
        if (reason.isBlank()) return Result.failure(Exception("Cần nhập lý do từ chối"))
        moveSuggestion(item, SuggestionStatus.REJECTED, reason); return Result.success(Unit)
    }

    override fun getReports(status: AdminReportStatus): Flow<List<AdminReport>> =
        reports.map { list -> list.filter { it.status == status } }

    private fun setReport(report: AdminReport, to: AdminReportStatus): Result<Unit> {
        reports.update { list -> list.map { if (it.id == report.id) it.copy(status = to) else it } }
        return Result.success(Unit)
    }

    override suspend fun dismissReport(report: AdminReport) = setReport(report, AdminReportStatus.DISMISSED)
    override suspend fun hideReportedReview(report: AdminReport) = setReport(report, AdminReportStatus.RESOLVED)
    override suspend fun markPlaceClosed(report: AdminReport) = setReport(report, AdminReportStatus.RESOLVED)
    override suspend fun resolveReport(report: AdminReport) = setReport(report, AdminReportStatus.RESOLVED)

    override fun getRegions(): Flow<List<AdminRegion>> = regions

    override fun getDishes(regionId: String): Flow<List<AdminDish>> =
        dishes.map { list -> list.filter { it.first == regionId }.map { it.second } }

    override suspend fun setDishVisible(dishId: String, visible: Boolean): Result<Unit> {
        dishes.update { list -> list.map { (r, d) -> if (d.id == dishId) r to d.copy(visible = visible) else r to d } }
        return Result.success(Unit)
    }

    override suspend fun addDish(regionId: String, name: String, unit: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Cần nhập tên món"))
        dishes.update { it + (regionId to AdminDish("d_${it.size + 1}", name.trim(), "—", 0, true)) }
        return Result.success(Unit)
    }

    override fun getUsers(): Flow<List<AdminUser>> = users

    override suspend fun setUserLocked(uid: String, locked: Boolean): Result<Unit> {
        users.update { list -> list.map { if (it.uid == uid) it.copy(locked = locked) else it } }
        return Result.success(Unit)
    }

    override fun getAdminName(): Flow<String> = flowOf("Nhật")

    override suspend fun findNearbyPlaces(regionId: String, location: LatLng, radiusM: Double): List<NearbyPlace> =
        listOf(NearbyPlace("Bún bò Huế O Hà", 9))

    override suspend fun updateSuggestion(item: AdminSuggestion, name: String, address: String, price: Long?, openHours: String?): Result<Unit> {
        if (name.isBlank() || address.isBlank()) return Result.failure(Exception("Tên và địa chỉ không được để trống"))
        suggestions.update { list ->
            list.map {
                if (it.suggestion.id == item.suggestion.id)
                    it.copy(suggestion = it.suggestion.copy(name = name.trim(), address = address.trim(), price = price ?: it.suggestion.price, openHours = openHours ?: it.suggestion.openHours))
                else it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun updateDish(dishId: String, name: String, unit: String): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Cần nhập tên món"))
        dishes.update { list -> list.map { (r, d) -> if (d.id == dishId) r to d.copy(name = name.trim(), unit = unit) else r to d } }
        return Result.success(Unit)
    }

    override suspend fun addRegion(name: String, lat: Double, lng: Double): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Cần nhập tên vùng"))
        regions.update { it + AdminRegion("r_${it.size + 1}", name.trim(), 0, 0, true) }
        return Result.success(Unit)
    }

    override suspend fun updateRegion(regionId: String, name: String, visible: Boolean): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Cần nhập tên vùng"))
        regions.update { list -> list.map { if (it.id == regionId) it.copy(name = name.trim(), visible = visible) else it } }
        return Result.success(Unit)
    }
}