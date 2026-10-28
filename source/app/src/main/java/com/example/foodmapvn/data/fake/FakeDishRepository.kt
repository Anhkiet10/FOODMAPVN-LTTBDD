package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Dữ liệu giả chỉ có Huế; tỉnh khác trả về rỗng (vẫn hiện tên tỉnh). */
class FakeDishRepository(private val areaRepository: AreaRepository) : DishRepository {

    private val areaId = areaRepository.selectedArea.map { (it ?: VietnamProvinces.DEFAULT).id }

    override fun getRegionSummary(): Flow<RegionSummary> = areaRepository.selectedArea.map { p ->
        val province = p ?: VietnamProvinces.DEFAULT
        if (province.id == "hue") FakeData.regionSummary else RegionSummary.empty(province.name)
    }

    override fun getDishes(): Flow<List<Dish>> = areaId.map { if (it == "hue") FakeData.dishes else emptyList() }

    override fun getPlacePins(): Flow<List<PlacePin>> = areaId.map { if (it == "hue") FakeData.placePins else emptyList() }

    override fun getPlaces(): Flow<List<PlaceSummary>> = areaId.map { if (it == "hue") FAKE_PLACES else emptyList() }

    private companion object {
        val FAKE_PLACES = listOf(
            PlaceSummary("p1", "Bún bò O Hạnh", "Phú Hội, Huế", LatLng(16.4655, 107.5905), "06:00", "21:00", 89, true, false, 4.6, listOf("bun-bo-hue"), mapOf("bun-bo-hue" to 35_000L)),
            PlaceSummary("p2", "Chè Hẻm", "Hùng Vương, Huế", LatLng(16.4690, 107.5960), "09:00", "22:30", 86, true, false, 4.5, listOf("che-hue"), mapOf("che-hue" to 15_000L)),
            PlaceSummary("p3", "Cơm hến Bà Nở", "Cồn Hến, Huế", LatLng(16.4720, 107.5850), "06:30", "14:00", 84, true, false, 4.4, listOf("com-hen"), mapOf("com-hen" to 20_000L)),
            PlaceSummary("p4", "Bánh canh Nam Phổ", "Phú Hội, Huế", LatLng(16.4610, 107.5980), "15:00", "21:00", 80, false, false, 4.2, listOf("banh-canh-nam-pho"), mapOf("banh-canh-nam-pho" to 25_000L)),
            PlaceSummary("p5", "Hoàng Cung", "Lê Lợi, Huế", LatLng(16.4680, 107.5880), "10:00", "22:00", 58, false, true, 3.6, listOf("bun-bo-hue", "com-hen"), mapOf("bun-bo-hue" to 80_000L, "com-hen" to 60_000L)),
        )
    }
}
