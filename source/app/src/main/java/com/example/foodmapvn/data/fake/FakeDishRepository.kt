package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.data.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeDishRepository : DishRepository {
    override fun getRegionSummary(): Flow<RegionSummary> = flowOf(FakeData.regionSummary)
    override fun getDishes(): Flow<List<Dish>> = flowOf(FakeData.dishes)
    override fun getPlacePins(): Flow<List<PlacePin>> = flowOf(FakeData.placePins)
}
