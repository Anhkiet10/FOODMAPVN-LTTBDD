package com.example.foodmapvn.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Màn "Đánh giá của tôi" (route my_reviews): mở từ "Xem tất cả" ở Hồ sơ. */
class MyReviewsViewModel(userRepository: UserRepository) : ViewModel() {
    /** null = đang tải. */
    val reviews: StateFlow<List<ReviewSummary>?> = userRepository.getAllReviews()
        .map<List<ReviewSummary>, List<ReviewSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MyReviewsViewModel(appContainer().userRepository) }
        }
    }
}

@Composable
fun MyReviewsRoute(
    onBack: () -> Unit,
    onPlaceClick: (placeId: String) -> Unit,
    viewModel: MyReviewsViewModel = viewModel(factory = MyReviewsViewModel.Factory),
) {
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    MyReviewsScreen(reviews, onBack, onReviewClick = { if (it.placeId.isNotBlank()) onPlaceClick(it.placeId) })
}

@Composable
fun MyReviewsScreen(
    reviews: List<ReviewSummary>?,
    onBack: () -> Unit,
    onReviewClick: (ReviewSummary) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
            }
            Text("Đánh giá của tôi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        when {
            reviews == null -> Unit // đang tải: để trống, dữ liệu thường về rất nhanh
            reviews.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Bạn chưa viết đánh giá nào.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        "${reviews.size} đánh giá",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(review, onClick = { onReviewClick(review) })
                }
            }
        }
    }
}
