package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreFilterTest {
    private fun dish(id: String, low: Long?) = Dish(id, id, 1, "—", priceRange = "", priceUnit = "tô", art = FoodArtType.BUN_BO, priceLow = low)
    private fun place(id: String, lat: Double, dishes: Map<String, Long>, trust: Int = 85, warn: Boolean = false, open: String = "06:00", close: String = "21:00") =
        PlaceSummary(id, id, "", LatLng(lat, 107.59), open, close, trust, true, warn, 4.5, dishes.keys.toList(), dishes)

    private val me = LatLng(16.4600, 107.59)
    private val noon = 12 * 60
    private val bunBo = dish("bun-bo", 35_000)
    private val che = dish("che", 15_000)
    private val near = place("near", 16.4610, mapOf("bun-bo" to 40_000L))               // ~110 m
    private val far = place("far", 16.4900, mapOf("bun-bo" to 30_000L, "che" to 15_000L)) // ~3,3 km
    private val bad = place("bad", 16.4605, mapOf("che" to 90_000L), trust = 50, warn = true)

    @Test fun noFiltersKeepsAllAndUsesRealNearest() {
        val r = ExploreFilter.filterDishes(listOf(bunBo, che), listOf(near, far, bad), ExploreFilters(), me, noon)
        assertEquals(2, r.size)
        assertTrue(r[0].nearestMeters!! < 200)   // bún bò: quán "near"
        assertTrue(r[1].nearestMeters!! < 100)   // chè: quán "bad" (không lọc)
    }

    @Test fun hidePriceWarningAndTrustMovesNearestToGoodPlace() {
        val f = ExploreFilters(hidePriceWarning = true, minTrust = 80)
        val che = ExploreFilter.filterDishes(listOf(che), listOf(near, far, bad), f, me, noon).single()
        assertTrue(che.nearestMeters!! > 3000)    // phải đi tới quán "far"
    }

    @Test fun priceAndDistance() {
        val f = ExploreFilters(maxPrice = 35_000, maxDistanceKm = 1.0)
        // bún bò: "near" 40k (quá giá), "far" 30k nhưng xa 3,3 km -> loại
        assertTrue(ExploreFilter.filterDishes(listOf(bunBo), listOf(near, far), f, me, noon).isEmpty())
        // Chưa biết vị trí: bỏ qua khoảng cách -> "far" 30k đạt
        assertEquals(1, ExploreFilter.filterDishes(listOf(bunBo), listOf(near, far), f, null, noon).size)
    }

    @Test fun openingHoursIncludingOvernight() {
        assertEquals(true, OpeningHours.isOpen("06:00", "21:00", noon))
        assertEquals(false, OpeningHours.isOpen("06:00", "21:00", 22 * 60))
        assertEquals(true, OpeningHours.isOpen("18:00", "02:00", 60))
        assertNull(OpeningHours.isOpen(null, "21:00", noon))
        val night = 22 * 60
        assertTrue(ExploreFilter.filterDishes(listOf(bunBo), listOf(near), ExploreFilters(openNow = true), me, night).isEmpty())
    }

    @Test fun textSearchIgnoresAccents() {
        assertTrue(TextSearch.matches("bun bo", "Bún bò Huế"))
        assertTrue(TextSearch.matches("BÚN hue", "Bún bò Huế"))
        assertFalse(TextSearch.matches("com hen", "Bún bò Huế"))
    }
}
