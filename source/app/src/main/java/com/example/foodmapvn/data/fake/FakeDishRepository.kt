package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
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
}
