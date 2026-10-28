package com.example.foodmapvn.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.analytics.AppAnalytics
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Thông báo thành công (ví dụ đã gửi email đặt lại mật khẩu). */
    val message: String? = null,
    /** true khi đăng nhập/đăng ký xong → màn hình tự đóng. */
    val isDone: Boolean = false,
)

/** Dùng chung cho Đăng nhập, Đăng ký và Quên mật khẩu. */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val analytics: AppAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun signIn() {
        val s = _uiState.value
        validate(email = s.email, password = s.password)?.let { return showError(it) }
        submit(onSuccess = { analytics.login() }) { authRepository.signIn(s.email, s.password) }
    }

    fun register() {
        val s = _uiState.value
        if (s.name.isBlank()) return showError("Vui lòng nhập họ tên")
        validate(email = s.email, password = s.password)?.let { return showError(it) }
        submit(onSuccess = { analytics.signUp() }) { authRepository.register(s.name, s.email, s.password) }
    }

    fun sendPasswordReset() {
        val s = _uiState.value
        validate(email = s.email, password = null)?.let { return showError(it) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            authRepository.sendPasswordReset(s.email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, message = "Đã gửi email đặt lại mật khẩu tới ${s.email}") }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    // ---------- Bên thứ ba: màn hình lấy token (Google / Facebook) rồi gọi các hàm này ----------

    fun onSocialStart() = _uiState.update { it.copy(isLoading = true, error = null, message = null) }

    fun onSocialError(message: String) = _uiState.update { it.copy(isLoading = false, error = message) }

    fun signInWithGoogleToken(idToken: String) =
        submit(onSuccess = { analytics.login("google") }) { authRepository.signInWithGoogle(idToken) }

    fun signInWithFacebookToken(accessToken: String) =
        submit(onSuccess = { analytics.login("facebook") }) { authRepository.signInWithFacebook(accessToken) }

    private fun submit(onSuccess: () -> Unit = {}, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            action()
                .onSuccess {
                    onSuccess()
                    _uiState.update { it.copy(isLoading = false, isDone = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun showError(message: String) = _uiState.update { it.copy(error = message) }

    private fun validate(email: String, password: String?): String? = when {
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Email không hợp lệ"
        password != null && password.length < 6 -> "Mật khẩu cần ít nhất 6 ký tự"
        else -> null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(appContainer().authRepository, appContainer().analytics) }
        }
    }
}
