package com.example.foodmapvn.ui.dish

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.components.StarRating
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

@Composable
fun DishDetailRoute(
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DishDetailViewModel = viewModel(factory = DishDetailViewModel.Factory),
) {
    val detail by viewModel.dishDetail.collectAsStateWithLifecycle()
    DishDetailScreen(
        detail = detail,
        onBack = onBack,
        onToggleSave = viewModel::toggleSaveDish,
        onPlaceClick = onPlaceClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    detail: DishDetail?,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.dish?.name ?: "Chi tiết đặc sản") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = onToggleSave) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bookmark),
                                contentDescription = "Lưu món",
                                tint = if (detail.isSaved) BrickRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (detail == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Đang tải thông tin đặc sản...")
            }
            return@Scaffold
        }

        val dish = detail.dish
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                FoodImage(
                    imageUrl = dish.imageUrl,
                    fallbackArt = dish.art,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(16.dp))

                Text(dish.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dish.priceRange,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrickRed,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(" / ${dish.priceUnit}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))

                Row {
                    InfoTag(R.drawable.ic_verified, "${dish.verifiedPlaceCount} quán đã xác thực")
                    Spacer(Modifier.width(16.dp))
                    InfoTag(R.drawable.ic_place, dish.nearestDistance)
                }
                Spacer(Modifier.height(16.dp))

                if (!detail.description.isNullOrBlank()) {
                    Text("Giới thiệu", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        detail.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (!detail.bestTime.isNullOrBlank()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Sand)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(R.drawable.ic_place),
                                contentDescription = null,
                                tint = BrickRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                detail.bestTime,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Text(
                    "Quán ngon gần bạn (${detail.nearbyPlaces.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
            }

            if (detail.nearbyPlaces.isEmpty()) {
                item {
                    Text(
                        "Chưa có quán nào bán món này trong khu vực.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(detail.nearbyPlaces, key = { it.id }) { place ->
                    PlaceSummaryCard(place = place, onClick = { onPlaceClick(place.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaceSummaryCard(place: PlaceSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (place.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painterResource(R.drawable.ic_verified),
                            contentDescription = "Đã xác thực",
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    place.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(rating = place.ratingAvg.toInt())
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${place.ratingAvg}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SuccessGreenSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${place.trustScore}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
