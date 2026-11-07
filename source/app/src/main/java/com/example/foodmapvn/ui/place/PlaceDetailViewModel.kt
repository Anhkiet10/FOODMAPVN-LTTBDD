package com.example.foodmapvn.ui.place

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaceDetailViewModel(
    private val placeId: String,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    val placeDetail: StateFlow<PlaceDetail?> = placeRepository.getPlaceDetail(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val reviews: StateFlow<List<ReviewItem>> = placeRepository.getPlaceReviews(placeId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggleSavePlace() {
        viewModelScope.launch {
            placeRepository.toggleSavePlace(placeId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val placeId = checkNotNull(savedStateHandle.get<String>(Routes.ARG_PLACE_ID))
                val container = appContainer()
                PlaceDetailViewModel(placeId = placeId, placeRepository = container.placeRepository)
            }
        }
    }
}
