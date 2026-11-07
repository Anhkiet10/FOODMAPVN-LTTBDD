package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.NewSuggestion
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.data.repository.SuggestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FakeSuggestionRepository : SuggestionRepository {

    private val items = MutableStateFlow(
        listOf(
            PlaceSuggestion(
                id = "fake-1", userId = "demo", regionId = "hue", dishId = "bun-bo-hue",
                name = "Bún bò Cô Lan", address = "Dữ liệu mẫu – Phường Phú Hội, TP Huế",
                location = LatLng(16.4650, 107.5920), price = 40_000, openHours = "06:00-13:00",
                photoUrl = null, status = SuggestionStatus.PENDING, rejectReason = null,
                createdAt = "25/10/2026",
            ),
            PlaceSuggestion(
                id = "fake-2", userId = "demo", regionId = "hue", dishId = "che-hue",
                name = "Chè Cung Đình", address = "Dữ liệu mẫu – Phường Thuận Hoà, TP Huế",
                location = LatLng(16.4690, 107.5890), price = 15_000, openHours = "10:00-22:00",
                photoUrl = null, status = SuggestionStatus.APPROVED, rejectReason = null,
                createdAt = "20/10/2026",
            ),
            PlaceSuggestion(
                id = "fake-3", userId = "demo", regionId = "hue", dishId = "com-hen",
                name = "Cơm hến Hẻm 5", address = "Dữ liệu mẫu – Phường Vỹ Dạ, TP Huế",
                location = LatLng(16.4721, 107.5851), price = 20_000, openHours = null,
                photoUrl = null, status = SuggestionStatus.REJECTED,
                rejectReason = "Quán đã có trong hệ thống (trùng với Cơm hến Bà Nở)",
                createdAt = "18/10/2026",
            ),
        ),
    )

    override fun getMySuggestions(): Flow<List<PlaceSuggestion>> = items.asStateFlow()

    override suspend fun submit(input: NewSuggestion): Result<Unit> {
        delay(500)
        val created = PlaceSuggestion(
            id = "fake-${UUID.randomUUID()}", userId = "demo",
            regionId = input.regionId, dishId = input.dishId,
            name = input.name, address = input.address, location = input.location,
            price = input.price, openHours = input.openHours, photoUrl = input.photoUrl,
            status = SuggestionStatus.PENDING, rejectReason = null,
            createdAt = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()),
        )
        items.update { listOf(created) + it }
        return Result.success(Unit)
    }
}