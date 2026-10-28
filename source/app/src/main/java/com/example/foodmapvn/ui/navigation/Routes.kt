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
    const val REGION_SELECT = "region_select"
    const val SEARCH = "search"
    const val PROFILE_EDIT = "profile_edit"

    // ===== Người 2 =====
    const val ARG_DISH_ID = "dishId"
    const val ARG_PLACE_ID = "placeId"
    const val DISH = "dish/{$ARG_DISH_ID}"
    const val PLACE = "place/{$ARG_PLACE_ID}"
    const val PLACE_REVIEWS = "place/{$ARG_PLACE_ID}/reviews"
    const val CHECKIN = "checkin/{$ARG_PLACE_ID}"
    const val REVIEW = "review/{$ARG_PLACE_ID}"

    fun dish(dishId: String) = "dish/$dishId"
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
