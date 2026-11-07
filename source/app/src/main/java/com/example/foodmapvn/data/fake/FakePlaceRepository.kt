package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.domain.CheckInResult
import com.example.foodmapvn.domain.CheckInValidator
import com.example.foodmapvn.domain.TrustScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakePlaceRepository(
    private val checkInValidator: CheckInValidator = CheckInValidator(),
    private val trustScoreCalculator: TrustScoreCalculator = TrustScoreCalculator(),
) : PlaceRepository {

    private val savedPlaceIds = MutableStateFlow(setOf("p1", "p2"))
    private val savedDishIds = MutableStateFlow(setOf("s1", "s2", "s3"))

    private val placesMap = MutableStateFlow(
        mapOf(
            "p1" to PlaceDetail(
                id = "p1",
                name = "Bún bò Mẫu A (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4608, 107.5925),
                openTime = "06:00",
                closeTime = "21:00",
                trustScore = 89,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.6,
                reviewCount = 28,
                checkInCount = 142,
                dishIds = listOf("bun-bo-hue"),
                dishes = listOf(FakeData.dishes[0]),
                minPriceByDish = mapOf("bun-bo-hue" to 35000L),
                regionId = "hue",
            ),
            "p2" to PlaceDetail(
                id = "p2",
                name = "Chè Mẫu D (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4658, 107.5948),
                openTime = "09:00",
                closeTime = "22:30",
                trustScore = 86,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.5,
                reviewCount = 19,
                checkInCount = 98,
                dishIds = listOf("che-hue"),
                dishes = listOf(FakeData.dishes[1]),
                minPriceByDish = mapOf("che-hue" to 15000L),
                regionId = "hue",
            ),
            "p3" to PlaceDetail(
                id = "p3",
                name = "Cơm hến Mẫu E (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4785, 107.5990),
                openTime = "06:30",
                closeTime = "14:00",
                trustScore = 84,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.4,
                reviewCount = 15,
                checkInCount = 76,
                dishIds = listOf("com-hen"),
                dishes = listOf(FakeData.dishes[2]),
                minPriceByDish = mapOf("com-hen" to 20000L),
                regionId = "hue",
            ),
            "p4" to PlaceDetail(
                id = "p4",
                name = "Bánh canh Mẫu F (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4612, 107.5941),
                openTime = "15:00",
                closeTime = "21:00",
                trustScore = 80,
                isVerified = false,
                hasPriceWarning = false,
                ratingAvg = 4.2,
                reviewCount = 10,
                checkInCount = 45,
                dishIds = listOf("banh-canh-nam-pho"),
                dishes = listOf(FakeData.dishes[3]),
                minPriceByDish = mapOf("banh-canh-nam-pho" to 25000L),
                regionId = "hue",
            ),
            "p5" to PlaceDetail(
                id = "p5",
                name = "Nhà hàng Mẫu C (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4678, 107.5882),
                openTime = "10:00",
                closeTime = "22:00",
                trustScore = 58,
                isVerified = false,
                hasPriceWarning = true,
                ratingAvg = 3.6,
                reviewCount = 8,
                checkInCount = 12,
                dishIds = listOf("bun-bo-hue", "com-hen"),
                dishes = listOf(FakeData.dishes[0], FakeData.dishes[2]),
                minPriceByDish = mapOf("bun-bo-hue" to 80000L, "com-hen" to 60000L),
                regionId = "hue",
            ),
            "mau-bun-bo-o-hanh" to PlaceDetail(
                id = "mau-bun-bo-o-hanh",
                name = "Bún bò Mẫu A (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4608, 107.5925),
                openTime = "06:00",
                closeTime = "21:00",
                trustScore = 89,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.6,
                reviewCount = 32,
                checkInCount = 46,
                dishIds = listOf("bun-bo-hue"),
                dishes = listOf(FakeData.dishes[0]),
                minPriceByDish = mapOf("bun-bo-hue" to 35000L),
                regionId = "hue",
            ),
            "mau-bun-bo-me-keo" to PlaceDetail(
                id = "mau-bun-bo-me-keo",
                name = "Bún bò Mẫu B (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4741, 107.5921),
                openTime = "06:00",
                closeTime = "13:00",
                trustScore = 82,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.4,
                reviewCount = 21,
                checkInCount = 38,
                dishIds = listOf("bun-bo-hue"),
                dishes = listOf(FakeData.dishes[0]),
                minPriceByDish = mapOf("bun-bo-hue" to 40000L),
                regionId = "hue",
            ),
            "mau-hoang-cung" to PlaceDetail(
                id = "mau-hoang-cung",
                name = "Nhà hàng Mẫu C (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4678, 107.5882),
                openTime = "07:00",
                closeTime = "22:00",
                trustScore = 58,
                isVerified = true,
                hasPriceWarning = true,
                ratingAvg = 3.6,
                reviewCount = 14,
                checkInCount = 19,
                dishIds = listOf("bun-bo-hue", "nem-lui"),
                dishes = listOf(FakeData.dishes[0]),
                minPriceByDish = mapOf("bun-bo-hue" to 70000L),
                regionId = "hue",
            ),
            "mau-che-hem" to PlaceDetail(
                id = "mau-che-hem",
                name = "Chè Mẫu D (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4658, 107.5948),
                openTime = "10:00",
                closeTime = "22:00",
                trustScore = 86,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.5,
                reviewCount = 27,
                checkInCount = 41,
                dishIds = listOf("che-hue"),
                dishes = listOf(FakeData.dishes[1]),
                minPriceByDish = mapOf("che-hue" to 15000L),
                regionId = "hue",
            ),
            "mau-com-hen-ba-no" to PlaceDetail(
                id = "mau-com-hen-ba-no",
                name = "Cơm hến Mẫu E (Mẫu)",
                address = "Quán tưởng tượng để demo – TP. Huế",
                location = LatLng(16.4785, 107.5990),
                openTime = "06:30",
                closeTime = "14:00",
                trustScore = 84,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.5,
                reviewCount = 18,
                checkInCount = 29,
                dishIds = listOf("com-hen"),
                dishes = listOf(FakeData.dishes[2]),
                minPriceByDish = mapOf("com-hen" to 20000L),
                regionId = "hue",
            ),
            "com-tam-ba-cuong" to PlaceDetail(
                id = "com-tam-ba-cuong",
                name = "Cơm tấm Mẫu G (Mẫu)",
                address = "Quán tưởng tượng để demo – TP Hồ Chí Minh",
                location = LatLng(10.7645, 106.6880),
                openTime = "07:00",
                closeTime = "22:00",
                trustScore = 91,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.7,
                reviewCount = 54,
                checkInCount = 88,
                dishIds = listOf("com-tam-sai-gon"),
                dishes = emptyList(),
                minPriceByDish = mapOf("com-tam-sai-gon" to 55000L),
                regionId = "ho-chi-minh",
            ),
            "banh-mi-huynh-hoa" to PlaceDetail(
                id = "banh-mi-huynh-hoa",
                name = "Bánh mì Mẫu H (Mẫu)",
                address = "Quán tưởng tượng để demo – TP Hồ Chí Minh",
                location = LatLng(10.7712, 106.6918),
                openTime = "11:00",
                closeTime = "21:00",
                trustScore = 94,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.8,
                reviewCount = 120,
                checkInCount = 210,
                dishIds = listOf("banh-mi-sai-gon"),
                dishes = emptyList(),
                minPriceByDish = mapOf("banh-mi-sai-gon" to 60000L),
                regionId = "ho-chi-minh",
            ),
            "pho-thin-lo-duc" to PlaceDetail(
                id = "pho-thin-lo-duc",
                name = "Phở Mẫu I (Mẫu)",
                address = "Quán tưởng tượng để demo – Hà Nội",
                location = LatLng(21.0180, 105.8560),
                openTime = "06:00",
                closeTime = "20:30",
                trustScore = 92,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.7,
                reviewCount = 85,
                checkInCount = 140,
                dishIds = listOf("pho-bo-ha-noi"),
                dishes = emptyList(),
                minPriceByDish = mapOf("pho-bo-ha-noi" to 65000L),
                regionId = "ha-noi",
            ),
            "bun-cha-dac-kim" to PlaceDetail(
                id = "bun-cha-dac-kim",
                name = "Bún chả Mẫu K (Mẫu)",
                address = "Quán tưởng tượng để demo – Hà Nội",
                location = LatLng(21.0315, 105.8485),
                openTime = "08:30",
                closeTime = "21:00",
                trustScore = 89,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.5,
                reviewCount = 62,
                checkInCount = 95,
                dishIds = listOf("bun-cha-ha-noi"),
                dishes = emptyList(),
                minPriceByDish = mapOf("bun-cha-ha-noi" to 60000L),
                regionId = "ha-noi",
            ),
            "banh-da-cua-co-yen" to PlaceDetail(
                id = "banh-da-cua-co-yen",
                name = "Bánh đa cua Mẫu L (Mẫu)",
                address = "Quán tưởng tượng để demo – TP Hải Phòng",
                location = LatLng(20.8580, 106.6850),
                openTime = "06:00",
                closeTime = "21:00",
                trustScore = 88,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.6,
                reviewCount = 25,
                checkInCount = 39,
                dishIds = listOf("banh-da-cua"),
                dishes = emptyList(),
                minPriceByDish = mapOf("banh-da-cua" to 40000L),
                regionId = "hai-phong",
            ),
            "bun-ca-cay-le-loi" to PlaceDetail(
                id = "bun-ca-cay-le-loi",
                name = "Bún cá cay Mẫu M (Mẫu)",
                address = "Quán tưởng tượng để demo – TP Hải Phòng",
                location = LatLng(20.8640, 106.6890),
                openTime = "06:30",
                closeTime = "20:30",
                trustScore = 85,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.4,
                reviewCount = 19,
                checkInCount = 28,
                dishIds = listOf("bun-ca-cay"),
                dishes = emptyList(),
                minPriceByDish = mapOf("bun-ca-cay" to 35000L),
                regionId = "hai-phong",
            ),
            "bun-ca-long-xuyen-be-hai" to PlaceDetail(
                id = "bun-ca-long-xuyen-be-hai",
                name = "Bún cá Mẫu N (Mẫu)",
                address = "Quán tưởng tượng để demo – An Giang",
                location = LatLng(10.3840, 105.4350),
                openTime = "06:00",
                closeTime = "18:00",
                trustScore = 86,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.5,
                reviewCount = 16,
                checkInCount = 24,
                dishIds = listOf("bun-ca-long-xuyen"),
                dishes = emptyList(),
                minPriceByDish = mapOf("bun-ca-long-xuyen" to 30000L),
                regionId = "an-giang",
            ),
            "bun-ken-chau-doc-co-mai" to PlaceDetail(
                id = "bun-ken-chau-doc-co-mai",
                name = "Bún kèn Mẫu O (Mẫu)",
                address = "Quán tưởng tượng để demo – An Giang",
                location = LatLng(10.7080, 105.1180),
                openTime = "06:30",
                closeTime = "12:00",
                trustScore = 83,
                isVerified = true,
                hasPriceWarning = false,
                ratingAvg = 4.4,
                reviewCount = 12,
                checkInCount = 18,
                dishIds = listOf("bun-ken-chau-doc"),
                dishes = emptyList(),
                minPriceByDish = mapOf("bun-ken-chau-doc" to 30000L),
                regionId = "an-giang",
            )
        )
    )

    private val reviewsMap = MutableStateFlow(
        mapOf(
            "p1" to listOf(
                ReviewItem(
                    id = "r1",
                    placeId = "p1",
                    placeName = "Bún bò Mẫu A (Mẫu)",
                    userId = "u1",
                    userName = "Lê Hoàng Nam",
                    rating = 5,
                    content = "Nước dùng đậm đà hương vị Huế chuẩn gốc, mắm ruốc thơm lừng. Quán phục vụ nhanh nhẹn dù rất đông khách.",
                    photos = emptyList(),
                    createdAt = "2 ngày trước",
                    isCheckedIn = true,
                    isLocal = true,
                    price = 45000L,
                    helpfulCount = 8,
                ),
                ReviewItem(
                    id = "r2",
                    placeId = "p1",
                    placeName = "Bún bò Mẫu A (Mẫu)",
                    userId = "u2",
                    userName = "Trần Thị Mai",
                    rating = 4,
                    content = "Bún bò ngon, giò heo mềm ngậy. Giá hợp lý so với chất lượng.",
                    photos = emptyList(),
                    createdAt = "5 ngày trước",
                    isCheckedIn = false,
                    isLocal = false,
                    price = 35000L,
                    helpfulCount = 3,
                )
            ),
            "p2" to listOf(
                ReviewItem(
                    id = "r3",
                    placeId = "p2",
                    placeName = "Chè Mẫu D (Mẫu)",
                    userId = "u3",
                    userName = "Nguyễn Thu Hà",
                    rating = 5,
                    content = "Chè hạt sen bọc nhãn lồng cực mát và thanh ngọt. Không gian hẻm cổ đặc trưng.",
                    photos = emptyList(),
                    createdAt = "1 tuần trước",
                    isCheckedIn = true,
                    isLocal = true,
                    price = 15000L,
                    helpfulCount = 5,
                )
            )
        )
    )

    private val lastCheckInMillisMap = mutableMapOf<String, Long>()

    override fun getPlaceDetail(placeId: String): Flow<PlaceDetail?> =
        placesMap.asStateFlow().map { map ->
            val place = map[placeId] ?: map.values.firstOrNull()
            place?.copy(isSaved = savedPlaceIds.value.contains(placeId))
        }

    override fun getPlaceReviews(placeId: String): Flow<List<ReviewItem>> =
        reviewsMap.asStateFlow().map { it[placeId] ?: emptyList() }

    override fun isPlaceSaved(placeId: String): Flow<Boolean> =
        savedPlaceIds.asStateFlow().map { it.contains(placeId) }

    override suspend fun toggleSavePlace(placeId: String) {
        savedPlaceIds.update { set ->
            if (set.contains(placeId)) set - placeId else set + placeId
        }
    }

    override suspend fun performCheckIn(
        placeId: String,
        userLat: Double,
        userLon: Double,
    ): Result<CheckInResult> {
        val place = placesMap.value[placeId]
            ?: return Result.failure(IllegalArgumentException("Quán không tồn tại"))

        val lastCheckIn = lastCheckInMillisMap[placeId]
        val result = checkInValidator.validate(
            userLat = userLat,
            userLon = userLon,
            placeLat = place.location.latitude,
            placeLon = place.location.longitude,
            lastCheckInAtMillis = lastCheckIn,
        )

        if (result is CheckInResult.Valid) {
            lastCheckInMillisMap[placeId] = System.currentTimeMillis()
            placesMap.update { map ->
                val current = map[placeId] ?: return@update map
                val updatedCheckInCount = current.checkInCount + 1
                map + (placeId to current.copy(
                    checkInCount = updatedCheckInCount,
                    trustScore = minOf(100, current.trustScore + 1),
                ))
            }
        }
        return Result.success(result)
    }

    override suspend fun submitReview(
        placeId: String,
        rating: Int,
        content: String,
        price: Long?,
        photoUrls: List<String>,
    ): Result<Unit> {
        val lastCheckIn = lastCheckInMillisMap[placeId]
        val isCheckedIn = checkInValidator.reviewHasCheckIn(lastCheckIn, System.currentTimeMillis())

        val newReview = ReviewItem(
            id = "rev_${System.currentTimeMillis()}",
            placeId = placeId,
            placeName = placesMap.value[placeId]?.name ?: "Quán",
            userId = "user_me",
            userName = FakeData.user.name,
            userAvatar = FakeData.user.avatarUrl,
            rating = rating,
            content = content.ifBlank { null },
            photos = photoUrls,
            createdAt = "Vừa xong",
            isCheckedIn = isCheckedIn,
            isLocal = true,
            price = price,
            helpfulCount = 0,
        )

        reviewsMap.update { map ->
            val list = map[placeId] ?: emptyList()
            map + (placeId to (listOf(newReview) + list))
        }

        // Tính lại ratingAvg
        placesMap.update { map ->
            val p = map[placeId] ?: return@update map
            val allReviews = reviewsMap.value[placeId] ?: emptyList()
            val avg = if (allReviews.isEmpty()) p.ratingAvg else allReviews.map { it.rating }.average()
            val roundedAvg = (avg * 10).toInt() / 10.0
            map + (placeId to p.copy(
                ratingAvg = roundedAvg,
                reviewCount = allReviews.size,
            ))
        }

        return Result.success(Unit)
    }
}
