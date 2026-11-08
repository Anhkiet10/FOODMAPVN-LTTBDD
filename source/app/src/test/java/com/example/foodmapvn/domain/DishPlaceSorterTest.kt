package com.example.foodmapvn.domain

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSort
import com.example.foodmapvn.data.model.PlaceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DishPlaceSorterTest {

    private fun place(id: String, trust: Int, lat: Double, rated: Boolean = true, verified: Boolean = false) = PlaceSummary(
        id = id, name = id, address = "", location = LatLng(lat, 107.59),
        openTime = "06:00", closeTime = "21:00", trustScore = trust, isVerified = verified,
        priceWarning = false, ratingAvg = 0.0, dishIds = listOf("bun-bo-hue"),
        minPriceByDish = mapOf("bun-bo-hue" to 35_000L), hasReviews = rated,
    )

    private val me = LatLng(16.4600, 107.59)
    private val near = place("gan", trust = 70, lat = 16.4610)          // ~110 m
    private val far = place("xa", trust = 90, lat = 16.4800)            // ~2,2 km
    private val unrated = place("moi", trust = 0, lat = 16.4601, rated = false)

    @Test fun trust_order_puts_rated_high_score_first_and_unrated_last() {
        val ids = DishPlaceSorter.sort(listOf(near, unrated, far), "bun-bo-hue", me, 600, PlaceSort.TRUST).map { it.place.id }
        assertEquals(listOf("xa", "gan", "moi"), ids)
    }

    @Test fun distance_order_uses_location() {
        val ids = DishPlaceSorter.sort(listOf(far, near, unrated), "bun-bo-hue", me, 600, PlaceSort.DISTANCE).map { it.place.id }
        assertEquals(listOf("moi", "gan", "xa"), ids)
    }

    @Test fun distance_without_location_falls_back_to_trust() {
        val result = DishPlaceSorter.sort(listOf(near, far), "bun-bo-hue", null, 600, PlaceSort.DISTANCE)
        assertEquals(listOf("xa", "gan"), result.map { it.place.id })
        assertNull(result.first().distanceMeters)
    }

    @Test fun fills_price_and_opening_state() {
        val r = DishPlaceSorter.sort(listOf(near), "bun-bo-hue", me, 22 * 60, PlaceSort.TRUST).single()
        assertEquals(35_000L, r.minPrice)
        assertEquals(false, r.isOpen)   // 22:00 > giờ đóng 21:00
    }

    @Test fun price_order_cheapest_first_and_no_price_last() {
        val cheap = near.copy(id = "re", minPriceByDish = mapOf("bun-bo-hue" to 25_000L))
        val noPrice = far.copy(id = "chua-gia", minPriceByDish = emptyMap())
        val ids = DishPlaceSorter.sort(listOf(noPrice, near, cheap), "bun-bo-hue", me, 600, PlaceSort.PRICE).map { it.place.id }
        assertEquals(listOf("re", "gan", "chua-gia"), ids)
    }

    @Test fun open_now_order_puts_open_places_first() {
        val night = far.copy(id = "dem", openTime = "18:00", closeTime = "23:00")
        val ids = DishPlaceSorter.sort(listOf(night, near), "bun-bo-hue", me, 20 * 60, PlaceSort.OPEN_NOW).map { it.place.id }
        assertEquals(listOf("dem", "gan"), ids)   // 20:00: quán đêm đang mở, quán 06–21h cũng mở -> xét uy tín (90 > 70)
        val morning = DishPlaceSorter.sort(listOf(night, near), "bun-bo-hue", me, 7 * 60, PlaceSort.OPEN_NOW).map { it.place.id }
        assertEquals(listOf("gan", "dem"), morning) // 07:00: chỉ quán 06–21h mở
    }
}
