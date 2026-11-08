package com.example.foodmapvn.ui.navigation

/**
 * Tên các màn trong NavHost — thống nhất cả nhóm (tài liệu đặc tả mục 9.1).
 *
 * Quy ước:
 * - Tên viết thường, nối bằng gạch dưới; phần {…} là tham số (Document ID trong Firestore).
 * - Chỉ truyền ID, màn đích tự đọc dữ liệu từ Repository.
 * - Mở màn có tham số bằng hàm tạo đường dẫn bên dưới (vd. Routes.place(id)), không tự ghép chuỗi.
 * - File chia 3 vùng theo người; mỗi người chỉ sửa vùng của mình.
 */
object Routes {

    // ===== Khung chung (Người 1) =====
    const val SPLASH = "splash"
    const val MAIN = "main"            // 4 tab: Khám phá, Bản đồ, Đã lưu, Hồ sơ
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val LOGIN_PHONE = "login_phone"   // đăng nhập bằng số điện thoại (OTP)

    // ===== Người 1 =====
    const val ONBOARDING = "onboarding"     // 3 trang Giới thiệu + hỏi quyền vị trí (lần đầu mở app)
    const val REGION_SELECT = "region_select"
    const val ARG_ASK_LOCATION = "ask"
    /** Mẫu route có tham số tùy chọn: navigate(Routes.REGION_SELECT) vẫn khớp (ask mặc định = true). */
    const val REGION_SELECT_PATTERN = "region_select?ask={$ARG_ASK_LOCATION}"
    fun regionSelect(askLocation: Boolean) = "region_select?ask=$askLocation"
    const val SEARCH = "search"
    const val PROFILE_EDIT = "profile_edit"
    const val MY_REVIEWS = "my_reviews"     // "Xem tất cả" đánh giá ở Hồ sơ
    const val BADGES = "badges"             // "Xem huy hiệu" ở Hồ sơ: 34 vùng + quán đã check-in
    const val SETTINGS = "settings"         // nút bánh răng ở Hồ sơ
    const val BUSINESS_REGISTER = "business_register" // Đăng ký doanh nghiệp (chủ quán), cần trước khi đề xuất quán

    // ===== Người 2 =====
    const val ARG_DISH_ID = "dishId"
    const val ARG_PLACE_ID = "placeId"
    /** placeId (tùy chọn) = quán đang xem khi mở món từ "Món của quán" -> nút "Xem quán trên bản đồ" chọn đúng quán đó. */
    const val ARG_FROM_PLACE_ID = "fromPlace"
    const val DISH = "dish/{$ARG_DISH_ID}?fromPlace={$ARG_FROM_PLACE_ID}"
    const val PLACE = "place/{$ARG_PLACE_ID}"
    const val PLACE_REVIEWS = "place/{$ARG_PLACE_ID}/reviews"
    const val CHECKIN = "checkin/{$ARG_PLACE_ID}"
    const val REVIEW = "review/{$ARG_PLACE_ID}"

    fun dish(dishId: String, fromPlaceId: String? = null) =
        if (fromPlaceId == null) "dish/$dishId" else "dish/$dishId?fromPlace=$fromPlaceId"
    fun place(placeId: String) = "place/$placeId"
    fun placeReviews(placeId: String) = "place/$placeId/reviews"
    fun checkin(placeId: String) = "checkin/$placeId"
    fun review(placeId: String) = "review/$placeId"

    // ===== Người 3 =====
    const val ARG_TARGET_TYPE = "targetType" // PLACE | REVIEW
    const val ARG_TARGET_ID = "targetId"
    const val SUGGESTION_NEW = "suggestion_new"
    const val SUGGESTION_MINE = "suggestion_mine"
    const val REPORT = "report/{$ARG_TARGET_TYPE}/{$ARG_TARGET_ID}"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN = "admin"

    fun report(targetType: String, targetId: String) = "report/$targetType/$targetId"
}
