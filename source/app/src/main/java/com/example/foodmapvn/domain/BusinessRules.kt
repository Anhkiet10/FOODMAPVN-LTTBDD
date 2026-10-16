package com.example.foodmapvn.domain

/**
 * Các ngưỡng nghiệp vụ (mục 5 tài liệu đặc tả). Gom một chỗ để dễ chỉnh;
 * sau này có thể đọc từ document config/rules trên Firestore.
 */
data class BusinessRules(
    /** BR-01: khoảng cách tối đa (m) để check-in hợp lệ. */
    val checkInRadiusMeters: Double = 100.0,
    /** BR-02: thời gian chờ (giờ) giữa hai lần check-in cùng một quán. */
    val checkInCooldownHours: Long = 12,
    /** BR-03: đánh giá được gắn nhãn "Check-in" nếu có check-in trong số ngày này. */
    val reviewCheckInWindowDays: Long = 7,
    /** BR-05: số check-in và số người tối thiểu để quán "đã xác thực". */
    val verifiedMinCheckIns: Int = 5,
    val verifiedMinUsers: Int = 3,
    /** BR-07: vượt cận trên mặt bằng giá bao nhiêu (%) thì cảnh báo. */
    val priceWarningThreshold: Double = 0.20,
    /** BR-08: số check-in để thành phần "check-in" của điểm uy tín đạt tối đa. */
    val trustCheckInSaturation: Int = 20,
    /** BR-08: điểm trừ cho mỗi vi phạm đã xác nhận. */
    val trustPenaltyPerViolation: Int = 5,
    /** BR-09: điều kiện huy hiệu Người địa phương. */
    val localBadgeCheckIns: Int = 5,
    val localBadgeMinPlaces: Int = 3,
    /** BR-10: trọng số đánh giá của người địa phương. */
    val localReviewWeight: Double = 1.5,
    /** BR-11: bán kính (m) cảnh báo trùng khi đề xuất quán. */
    val duplicatePlaceRadiusMeters: Double = 15.0,
    /** BR-13: số báo cáo từ người khác nhau để tạm ẩn nội dung. */
    val reportHideThreshold: Int = 3,
)
