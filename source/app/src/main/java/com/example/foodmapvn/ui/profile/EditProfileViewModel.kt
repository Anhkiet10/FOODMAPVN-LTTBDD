package com.example.foodmapvn.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.local.ImageCompressor
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.ImageUploader
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoaded: Boolean = false,
    val name: String = "",
    /** Ảnh đang có trên hồ sơ. */
    val currentAvatarUrl: String? = null,
    /** Ảnh vừa chọn trong máy (chưa tải lên) — hiện xem trước. */
    val pickedAvatar: Uri? = null,
    val isSaving: Boolean = false,
    /** Đang làm bước nào khi lưu, vd. "Đang tải ảnh lên…". */
    val progress: String? = null,
    val error: String? = null,
    val isDone: Boolean = false,
) {
    val avatarInitial: String
        get() = name.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "?"
}

/** UC-16 Sửa hồ sơ: đổi tên hiển thị và ảnh đại diện (ảnh lưu trên Cloudinary, Firestore chỉ lưu URL). */
class EditProfileViewModel(
    private val userRepository: UserRepository,
    private val imageUploader: ImageUploader,
    private val imageCompressor: ImageCompressor,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userRepository.getUser().first()
            _uiState.update { it.copy(isLoaded = true, name = user.name, currentAvatarUrl = user.avatarUrl) }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value.take(MAX_NAME), error = null) }

    fun onAvatarPicked(uri: Uri?) {
        if (uri != null) _uiState.update { it.copy(pickedAvatar = uri, error = null) }
    }

    fun save() {
        val s = _uiState.value
        val name = s.name.trim().replace(Regex("\\s+"), " ")
        validateName(name)?.let { msg -> return _uiState.update { it.copy(error = msg) } }

        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            // 1. Có ảnh mới: thu nhỏ -> tải lên Cloudinary -> lấy URL
            val avatarUrl: String? = s.pickedAvatar?.let { picked ->
                _uiState.update { it.copy(progress = "Đang tải ảnh lên…") }
                val uploaded = imageCompressor.compress(picked).mapCatching { small -> imageUploader.upload(small).getOrThrow() }
                uploaded.getOrElse { e ->
                    _uiState.update { it.copy(isSaving = false, progress = null, error = "Không tải được ảnh: ${e.message}") }
                    return@launch
                }
            }
            // 2. Lưu tên + URL ảnh vào users/{uid} (tạo hồ sơ trước nếu tài khoản cũ chưa có document)
            _uiState.update { it.copy(progress = "Đang lưu…") }
            authRepository.ensureProfile()
            userRepository.updateProfile(name, avatarUrl)
                .onSuccess { _uiState.update { it.copy(isSaving = false, progress = null, isDone = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, progress = null, error = e.message) } }
        }
    }

    companion object {
        const val MAX_NAME = 40

        /** Tên hiển thị: 2–40 ký tự, có ít nhất một chữ cái. Được trùng với người khác (tên thật hay trùng). */
        fun validateName(name: String): String? = when {
            name.length < 2 -> "Tên cần ít nhất 2 ký tự"
            name.length > MAX_NAME -> "Tên tối đa $MAX_NAME ký tự"
            name.none { it.isLetter() } -> "Tên cần có chữ cái"
            else -> null
        }

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                EditProfileViewModel(c.userRepository, c.imageUploader, c.imageCompressor, c.authRepository)
            }
        }
    }
}
