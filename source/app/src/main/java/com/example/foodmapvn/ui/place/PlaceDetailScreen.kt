package com.example.foodmapvn.ui.place

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.foodmapvn.ui.auth.LocalLoginGate
import com.example.foodmapvn.domain.formatVnd
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.components.StarRating
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

@Composable
fun PlaceDetailRoute(
    onBack: () -> Unit,
    onCheckInClick: (String) -> Unit,
    onWriteReviewClick: (String) -> Unit,
    onAllReviewsClick: (String) -> Unit,
    onDirectionsClick: (PlaceDetail) -> Unit = {},
    modifier: Modifier = Modifier,
    /** Bấm một món trong "Món của quán" -> Chi tiết đặc sản. */
    onDishClick: (dishId: String) -> Unit = {},
    viewModel: PlaceDetailViewModel = viewModel(factory = PlaceDetailViewModel.Factory),
) {
    val place by viewModel.placeDetail.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val dishes by viewModel.dishes.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val loginGate = LocalLoginGate.current

    PlaceDetailScreen(
        place = place,
        reviews = reviews,
        onBack = onBack,
        // Lưu quán cần tài khoản: khách được đưa sang Đăng nhập
        onToggleSave = { loginGate.requireLogin { viewModel.toggleSavePlace() } },
        message = message,
        onMessageShown = viewModel::onMessageShown,
        onCheckInClick = { onCheckInClick(place?.id ?: "") },
        onWriteReviewClick = { onWriteReviewClick(place?.id ?: "") },
        onAllReviewsClick = { onAllReviewsClick(place?.id ?: "") },
        onDirectionsClick = { place?.let(onDirectionsClick) },
        dishes = dishes,
        onDishClick = onDishClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    place: PlaceDetail?,
    reviews: List<ReviewItem>,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onCheckInClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onAllReviewsClick: () -> Unit,
    onDirectionsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    dishes: List<PlaceDish> = emptyList(),
    onDishClick: (String) -> Unit = {},
    message: String? = null,
    onMessageShown: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(place?.name ?: "Chi tiết quán") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (place != null) {
                        IconButton(onClick = onToggleSave) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bookmark),
                                contentDescription = "Lưu quán",
                                tint = if (place.isSaved) BrickRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier,
    ) { innerPadding ->
        if (place == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Đang tải thông tin quán...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                FoodImage(
                    imageUrl = place.imageUrl,
                    fallbackArt = FoodArtType.BUN_BO,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        place.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreenSoft)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Uy tín: ${place.trustScore}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_place),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        place.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(rating = place.ratingAvg.toInt())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${place.ratingAvg} (${place.reviewCount} đánh giá)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(16.dp))
                    InfoTag(R.drawable.ic_check, "${place.checkInCount} check-in")
                }
                Spacer(Modifier.height(12.dp))

                if (place.hasPriceWarning) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(10.dp)
                    ) {
                        Text(
                            "⚠️ Cảnh báo giá: Quán có mức giá cao hơn đáng kể so với mặt bằng chung khu vực!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (!place.openTime.isNullOrBlank()) {
                    Text(
                        "Giờ mở cửa: ${place.openTime} - ${place.closeTime ?: "Khuya"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Hàng nút tương tác
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDirectionsClick,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_place), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "Chỉ đường",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Button(
                        onClick = onCheckInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "Check-in",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Button(
                        onClick = onWriteReviewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_star), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "Đánh giá",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Món đặc sản của quán: bấm để xem Chi tiết đặc sản
            if (dishes.isNotEmpty()) {
                item {
                    Text("Món của quán", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
                items(dishes, key = { "dish-" + it.dish.id }) { item ->
                    PlaceDishRow(item, onClick = { onDishClick(item.dish.id) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            item {
                // Bấm tiêu đề hoặc "Xem tất cả" -> trang Danh sách đánh giá (lọc, báo cáo đánh giá)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAllReviewsClick),
                ) {
                    Text(
                        "Đánh giá (${reviews.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("Xem tất cả", color = BrickRed, fontWeight = FontWeight.Bold)
                    Icon(
                        painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = BrickRed,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            if (reviews.isEmpty()) {
                item {
                    Text(
                        "Chưa có đánh giá nào cho quán này. Hãy là người đầu tiên viết đánh giá!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(reviews.take(3), key = { it.id }) { review ->
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAllReviewsClick)) {
                        ReviewCard(review = review)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (reviews.size > 3) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAllReviewsClick)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Xem tất cả ${reviews.size} đánh giá ->",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrickRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceDishRow(item: PlaceDish, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodImage(
                imageUrl = item.dish.imageUrl,
                fallbackArt = item.dish.art,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.dish.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Đặc sản · xem chi tiết", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                item.minPrice?.let { "${formatVnd(it)}đ" } ?: "Chưa có giá",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (item.minPrice != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun ReviewCard(review: ReviewItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Sand),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    review.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    review.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(rating = review.rating)
                if (review.isCheckedIn) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SuccessGreenSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "✓ Check-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (review.isLocal) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Người địa phương",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (!review.content.isNullOrBlank()) {
                Text(review.content, style = MaterialTheme.typography.bodyMedium)
            }
            // Ảnh đính kèm của đánh giá
            if (review.photos.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    review.photos.take(4).forEach { url ->
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = "Ảnh đánh giá",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
    }
}
