package com.example.foodmapvn.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.SavedRepository
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Trạng thái màn Hồ sơ: đang tải, Khách (chưa đăng nhập) hoặc có dữ liệu. */
sealed interface ProfileScreenState {
    data object Loading : ProfileScreenState
    data object Guest : ProfileScreenState
    data class Content(val data: ProfileUiState) : ProfileScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    userRepository: UserRepository,
    savedRepository: SavedRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileScreenState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(ProfileScreenState.Guest)
            else combine(
                userRepository.getUser(),
                userRepository.hasUnreadNotification(),
                userRepository.getRegionBadges(),
                userRepository.getRecentReviews(),
                savedRepository.getSavedDishes(),
            ) { profile, hasUnread, badges, reviews, savedDishes ->
                ProfileScreenState.Content(
                    ProfileUiState(
                        user = profile,
                        hasUnreadNotification = hasUnread,
                        regionBadges = badges,
                        recentReviews = reviews,
                        wishlistCount = savedDishes.size,
                        mySuggestionCount = profile.suggestedPlaceCount,
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileScreenState.Loading,
        )

    fun signOut() = authRepository.signOut()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ProfileViewModel(
                    userRepository = container.userRepository,
                    savedRepository = container.savedRepository,
                    authRepository = container.authRepository,
                )
            }
        }
    }
}
