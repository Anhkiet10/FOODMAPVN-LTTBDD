package com.example.foodmapvn.ui.dish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.repository.PlaceRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DishDetailViewModel(
    private val dishId: String,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    val dishDetail: StateFlow<DishDetail?> = placeRepository.getDishDetail(dishId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun toggleSaveDish() {
        viewModelScope.launch {
            placeRepository.toggleSaveDish(dishId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val dishId = checkNotNull(savedStateHandle.get<String>(Routes.ARG_DISH_ID))
                val container = appContainer()
                DishDetailViewModel(dishId = dishId, placeRepository = container.placeRepository)
            }
        }
    }
}
