package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.FoodCategory
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.repository.DishDetailRepository
import com.example.foodmapvn.data.repository.DishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Người 2 — bản giả: lấy món từ FakeData và quán từ DishRepository giả (chỉ có Huế). */
class FakeDishDetailRepository(private val dishRepository: DishRepository) : DishDetailRepository {

    override fun getDish(dishId: String): Flow<DishDetail?> = dishRepository.getDishes().map { dishes ->
        (dishes + FakeData.dishes).firstOrNull { it.id == dishId }?.let { d ->
            DishDetail(
                id = d.id,
                regionId = "hue",
                name = d.name,
                description = TIPS[d.id]?.first ?: "Món đặc sản nổi tiếng của vùng.",
                tips = TIPS[d.id]?.second ?: emptyList(),
                bestTime = d.bestTime,
                priceRange = d.priceRange,
                priceUnit = d.priceUnit,
                category = if (d.category == FoodCategory.OTHER) FoodCategory.NOODLE_SOUP else d.category,
                art = d.art,
                imageUrl = d.imageUrl,
                verifiedPlaceCount = d.verifiedPlaceCount,
            )
        }
    }

    override fun getPlacesServing(dishId: String): Flow<List<PlaceSummary>> =
        dishRepository.getPlaces().map { places -> places.filter { dishId in it.dishIds } }

    private companion object {
        val TIPS = mapOf(
            "bun-bo-hue" to ("Nước dùng hầm xương, thơm sả và mắm ruốc; ăn kèm giò heo, chả cua, bò tái và rau sống." to
                listOf("Ăn buổi sáng 6–9h, nước dùng ngon nhất", "Trên 60.000đ/tô là cao hơn mặt bằng")),
            "che-hue" to ("Nhiều loại chè: chè bột lọc heo quay, chè đậu ván, chè khoai môn." to
                listOf("Gọi một cốc thập cẩm để thử nhiều vị")),
            "com-hen" to ("Cơm nguội trộn hến xào, ruốc, tóp mỡ, rau thơm, chan nước hến." to
                listOf("Thêm ớt và ruốc theo khẩu vị", "Ngon nhất vào buổi sáng")),
        )
    }
}
