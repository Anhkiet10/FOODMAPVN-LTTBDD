package com.example.foodmapvn.domain

import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class ProvinceLocatorTest {
    private val all = VietnamProvinces.all

    @Test fun matchesNewNameWithPrefix() {
        assertEquals("ho-chi-minh", ProvinceLocator.matchByName("Thành phố Hồ Chí Minh", all)?.id)
        assertEquals("ho-chi-minh", ProvinceLocator.matchByName("Ho Chi Minh City", all)?.id)
        assertEquals("hue", ProvinceLocator.matchByName("Thừa Thiên Huế", all)?.id)
    }

    @Test fun matchesFormerProvinceName() {
        assertEquals("ho-chi-minh", ProvinceLocator.matchByName("Bình Dương", all)?.id)
        assertEquals("da-nang", ProvinceLocator.matchByName("Tỉnh Quảng Nam", all)?.id)
    }

    @Test fun fallsBackToNearestCenter() {
        // Không có tên từ Geocoder: điểm ở Bình Thạnh -> TP. Hồ Chí Minh
        assertEquals("ho-chi-minh", ProvinceLocator.locate(LatLng(10.8325, 106.716), null, all)?.id)
        // Tên lạ -> vẫn lấy theo khoảng cách (gần Đại Nội Huế)
        assertEquals("hue", ProvinceLocator.locate(LatLng(16.4698, 107.5786), "Unknown", all)?.id)
    }

    @Test fun has34Provinces() {
        assertEquals(34, all.size)
    }
}
