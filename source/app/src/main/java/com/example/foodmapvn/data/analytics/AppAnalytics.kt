package com.example.foodmapvn.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Gửi thống kê sử dụng lên Google Analytics for Firebase (Firebase Console → Analytics).
 * Không gửi email, tên hay toạ độ người dùng — chỉ tên màn, từ khoá tìm kiếm, mã tỉnh.
 *
 * Dùng chung cả nhóm: lấy qua appContainer().analytics rồi gọi event(...) hoặc các hàm có sẵn.
 * Chưa nối Firebase (không có google-services.json) -> mọi hàm không làm gì.
 */
class AppAnalytics(context: Context, enabled: Boolean) {

    private val firebase: FirebaseAnalytics? =
        if (enabled) FirebaseAnalytics.getInstance(context.applicationContext) else null

    /** Mở một màn (route trong NavHost, hoặc tên tab). */
    fun screen(name: String) = log(FirebaseAnalytics.Event.SCREEN_VIEW) {
        putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
        putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
    }

    fun login(method: String = "email") = log(FirebaseAnalytics.Event.LOGIN) {
        putString(FirebaseAnalytics.Param.METHOD, method)
    }

    fun signUp(method: String = "email") = log(FirebaseAnalytics.Event.SIGN_UP) {
        putString(FirebaseAnalytics.Param.METHOD, method)
    }

    fun search(term: String) = log(FirebaseAnalytics.Event.SEARCH) {
        putString(FirebaseAnalytics.Param.SEARCH_TERM, term.take(100))
    }

    /** Chọn khu vực; source = "location" (tự nhận) | "manual" (chọn tay). */
    fun selectRegion(regionId: String, source: String) = event("select_region") {
        putString("region_id", regionId)
        putString("source", source)
    }

    /**
     * Sự kiện tuỳ ý cho các chức năng khác, vd. Người 2: event("check_in") { putString("place_id", id) }.
     * Tên sự kiện: chữ thường, gạch dưới, tối đa 40 ký tự.
     */
    fun event(name: String, params: Bundle.() -> Unit = {}) = log(name, params)

    private fun log(name: String, params: Bundle.() -> Unit) {
        firebase?.logEvent(name, Bundle().apply(params))
    }
}
