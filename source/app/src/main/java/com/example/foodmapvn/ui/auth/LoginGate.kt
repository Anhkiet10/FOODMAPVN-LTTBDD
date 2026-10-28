package com.example.foodmapvn.ui.auth

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * "Cổng đăng nhập" dùng chung cả nhóm: việc nào cần tài khoản (Lưu, Check-in, Đánh giá, Đề xuất, Báo cáo…)
 * thì bọc trong requireLogin { … }. Khách sẽ được đưa sang màn Đăng nhập, đăng nhập xong quay lại đúng màn cũ.
 *
 * Cách dùng trong màn của bạn:
 *     val loginGate = LocalLoginGate.current
 *     Button(onClick = { loginGate.requireLogin { viewModel.save() } }) { … }
 *
 * FoodMapApp cung cấp giá trị thật; trong @Preview là bản luôn cho qua.
 */
class LoginGate(
    private val isLoggedIn: () -> Boolean,
    private val openLogin: () -> Unit,
) {
    val loggedIn: Boolean get() = isLoggedIn()

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn()) action() else openLogin()
    }
}

val LocalLoginGate = staticCompositionLocalOf { LoginGate(isLoggedIn = { true }, openLogin = {}) }
