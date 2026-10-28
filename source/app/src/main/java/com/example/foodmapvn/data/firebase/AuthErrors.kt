package com.example.foodmapvn.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

/** Đổi lỗi của Firebase Auth sang câu tiếng Việt dễ hiểu. */
internal fun authErrorMessage(e: Throwable): String {
    val raw = e.message.orEmpty()
    if ("BILLING_NOT_ENABLED" in raw) {
        return "Gửi SMS thật cần gói Blaze. Khi demo trên gói miễn phí, hãy dùng số điện thoại thử nghiệm (xem FIREBASE_SETUP.md)."
    }
    if ("region enabled" in raw) {
        return "Firebase đang chặn gửi SMS tới vùng này: Authentication → Settings → SMS region policy → cho phép Vietnam."
    }
    return when (e) {
        is FirebaseNetworkException -> "Không có kết nối mạng"
        is FirebaseTooManyRequestsException -> "Thử quá nhiều lần, vui lòng đợi một lúc rồi thử lại"
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Email không hợp lệ"
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Thông tin đăng nhập không đúng hoặc đã hết hạn"
            "ERROR_USER_NOT_FOUND" -> "Không có tài khoản nào với email này"
            "ERROR_USER_DISABLED" -> "Tài khoản đã bị khoá"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email này đã được dùng cho tài khoản khác"
            "ERROR_WEAK_PASSWORD" -> "Mật khẩu cần ít nhất 6 ký tự"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Email này đã đăng ký bằng cách khác (email/mật khẩu hoặc Google). Hãy đăng nhập bằng cách đó."
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> "Tài khoản này đã gắn với người dùng khác"
            "ERROR_INVALID_PHONE_NUMBER" -> "Số điện thoại không hợp lệ"
            "ERROR_INVALID_VERIFICATION_CODE" -> "Mã xác minh không đúng"
            "ERROR_SESSION_EXPIRED" -> "Mã đã hết hạn, hãy bấm gửi lại mã"
            "ERROR_QUOTA_EXCEEDED" -> "Đã hết lượt gửi SMS trong hôm nay"
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Cách đăng nhập này chưa được bật (Firebase Console → Authentication → Sign-in method)"
            "ERROR_APP_NOT_AUTHORIZED", "ERROR_MISSING_CLIENT_IDENTIFIER" ->
                "App chưa được cấp phép: thêm mã SHA-1 của máy vào Firebase Console (Project settings → Your apps)"
            else -> raw.ifBlank { "Đăng nhập thất bại" }
        }
        else -> raw.ifBlank { "Đã có lỗi xảy ra" }
    }
}
