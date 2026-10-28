package com.example.foodmapvn.data.repository

import android.app.Activity
import com.example.foodmapvn.data.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** Kết quả bước 1 của đăng nhập bằng số điện thoại. */
data class PhoneCodeSent(
    /** Mã phiên để xác minh mã OTP; null khi đã tự đăng nhập (autoSignedIn). */
    val verificationId: String?,
    /** true = Firebase tự xác minh được (máy nhận SMS tự đọc / số thử nghiệm), đã đăng nhập luôn. */
    val autoSignedIn: Boolean,
    /** Dùng khi bấm "Gửi lại mã" (giá trị nội bộ của Firebase). */
    val resendToken: Any?,
)

interface AuthRepository {
    /** null = chưa đăng nhập (Khách). */
    val currentUser: Flow<AuthUser?>

    /** true khi tài khoản đang đăng nhập có role = "admin" (users/{uid}). Người 3 dùng cho màn Admin. */
    val isAdmin: Flow<Boolean>

    // --- Email / mật khẩu ---
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>

    // --- Bên thứ ba (Firebase Auth) ---
    /** [idToken] lấy từ Credential Manager (nút "Tiếp tục với Google"). */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /** [accessToken] lấy từ Facebook Login SDK. */
    suspend fun signInWithFacebook(accessToken: String): Result<Unit>

    /** Gửi mã OTP tới [phoneNumber] dạng quốc tế, vd. "+84912345678". Cần Activity cho reCAPTCHA/Play Integrity. */
    suspend fun sendPhoneCode(activity: Activity, phoneNumber: String, resendToken: Any? = null): Result<PhoneCodeSent>

    suspend fun verifyPhoneCode(verificationId: String, code: String): Result<Unit>

    /** Tạo users/{uid} nếu chưa có (lần đầu đăng nhập bằng Google/Facebook/số điện thoại). */
    suspend fun ensureProfile(): Result<Unit>

    fun signOut()
}
