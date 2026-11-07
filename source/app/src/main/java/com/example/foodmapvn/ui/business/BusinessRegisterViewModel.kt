package com.example.foodmapvn.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.BusinessStatus
import com.example.foodmapvn.data.model.BusinessType
import com.example.foodmapvn.data.repository.BusinessRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessRegisterUiState(
    val isLoaded: Boolean = false,
    /** Hồ sơ đã gửi; null = chưa đăng ký. */
    val business: Business? = null,
    /** Đang hiện form (chưa đăng ký, bị từ chối, hoặc bấm "Sửa thông tin" khi đang chờ). */
    val isEditing: Boolean = false,
    val name: String = "",
    val type: BusinessType = BusinessType.HOUSEHOLD,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Vừa gửi thành công -> hiện thông báo. */
    val justSubmitted: Boolean = false,
)

/** Đăng ký doanh nghiệp (chủ quán) — Người 1. Chỉ doanh nghiệp đã duyệt mới được đề xuất quán (UC-14). */
class BusinessRegisterViewModel(
    private val businessRepository: BusinessRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessRegisterUiState())
    val uiState: StateFlow<BusinessRegisterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            businessRepository.getMyBusiness().collect { business ->
                _uiState.update { s ->
                    val firstLoad = !s.isLoaded
                    s.copy(
                        isLoaded = true,
                        business = business,
                        // Lần đầu: chưa đăng ký hoặc bị từ chối -> mở form, điền sẵn thông tin cũ
                        isEditing = if (firstLoad) business == null || business.status == BusinessStatus.REJECTED else s.isEditing,
                        name = if (firstLoad) business?.name.orEmpty() else s.name,
                        type = if (firstLoad) business?.type ?: BusinessType.HOUSEHOLD else s.type,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value.take(BusinessRepository.NAME_MAX), error = null) }

    fun onTypeChange(value: BusinessType) = _uiState.update { it.copy(type = value) }

    fun startEditing() = _uiState.update {
        it.copy(isEditing = true, name = it.business?.name.orEmpty(), type = it.business?.type ?: BusinessType.HOUSEHOLD, error = null)
    }

    fun cancelEditing() = _uiState.update { it.copy(isEditing = false, error = null) }

    fun submit() {
        val s = _uiState.value
        BusinessRepository.validateName(s.name)?.let { msg -> return _uiState.update { it.copy(error = msg) } }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            businessRepository.register(s.name, s.type)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, isEditing = false, justSubmitted = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, error = e.message ?: "Không gửi được, thử lại sau") } }
        }
    }

    fun onSubmittedShown() = _uiState.update { it.copy(justSubmitted = false) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BusinessRegisterViewModel(appContainer().businessRepository) }
        }
    }
}
