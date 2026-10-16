package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.model.AuthUser
import com.example.foodmapvn.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Đăng nhập giả để chạy app khi chưa nối Firebase.
 * Mặc định đã đăng nhập sẵn tài khoản demo; mật khẩu nào ≥ 6 ký tự cũng được chấp nhận.
 */
class FakeAuthRepository : AuthRepository {
    private val user = MutableStateFlow<AuthUser?>(
        AuthUser(uid = "demo", email = "demo@foodmap.vn", displayName = FakeData.user.name)
    )
    override val currentUser: Flow<AuthUser?> = user.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        delay(500)
        if (password.length < 6) return Result.failure(IllegalArgumentException("Sai email hoặc mật khẩu"))
        user.value = AuthUser(uid = "demo", email = email, displayName = FakeData.user.name)
        return Result.success(Unit)
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        delay(500)
        user.value = AuthUser(uid = "demo", email = email, displayName = name)
        return Result.success(Unit)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(500)
        return Result.success(Unit)
    }

    override fun signOut() {
        user.value = null
    }
}
