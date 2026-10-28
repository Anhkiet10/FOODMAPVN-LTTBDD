package com.example.foodmapvn.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhoneLoginUiState(
    val phone: String = "",
    val code: String = "",
    /** Đã gửi mã -> hiện ô nhập mã. */
    val verificationId: String? = null,
    val sentTo: String? = null,
    /** Số giây còn lại trước khi được bấm "Gửi lại mã". */
    val resendInSeconds: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDone: Boolean = false,
) {
    val isCodeStep: Boolean get() = verificationId != null
}

/** UC-02 Đăng nhập bằng số điện thoại (Firebase Phone Auth): nhập số -> nhận mã OTP -> nhập mã. */
class PhoneLoginViewModel(
    private val authRepository: AuthRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    private var resendToken: Any? = null
    private var countdown: Job? = null
    private var finishing = false

    init {
        // Máy tự đọc được SMS (onVerificationCompleted đến muộn) -> Firebase đăng nhập luôn: tạo hồ sơ rồi đóng màn
        viewModelScope.launch {
            authRepository.currentUser.drop(1).filterNotNull().first()
            finish()
        }
    }

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value.filter { c -> c.isDigit() || c == '+' || c == ' ' }, error = null) }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }

    /** Bước 1: gửi mã. [activity] cần cho bước kiểm tra chống spam (reCAPTCHA / Play Integrity). */
    fun sendCode(activity: Activity, resend: Boolean = false) {
        val e164 = PhoneNumbers.toE164(_uiState.value.phone)
            ?: return _uiState.update { it.copy(error = "Số điện thoại không hợp lệ (vd. 0912 345 678)") }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendPhoneCode(activity, e164, if (resend) resendToken else null)
                .onSuccess { sent ->
                    if (sent.autoSignedIn) return@onSuccess finish()
                    resendToken = sent.resendToken
                    _uiState.update { it.copy(isLoading = false, verificationId = sent.verificationId, sentTo = e164, code = "") }
                    startCountdown()
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    /** Bước 2: xác minh mã 6 số. */
    fun verifyCode() {
        val s = _uiState.value
        val id = s.verificationId ?: return
        if (s.code.length != 6) return _uiState.update { it.copy(error = "Mã xác minh gồm 6 chữ số") }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.verifyPhoneCode(id, s.code)
                .onSuccess { finish() }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    /** Quay lại bước nhập số (đổi số khác). */
    fun changeNumber() {
        countdown?.cancel()
        _uiState.update { it.copy(verificationId = null, code = "", resendInSeconds = 0, error = null) }
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            authRepository.ensureProfile()
            analytics.login("phone")
            _uiState.update { it.copy(isLoading = false, isDone = true) }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (s in 60 downTo 0) {
                _uiState.update { it.copy(resendInSeconds = s) }
                delay(1_000)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PhoneLoginViewModel(appContainer().authRepository, appContainer().analytics) }
        }
    }
}
