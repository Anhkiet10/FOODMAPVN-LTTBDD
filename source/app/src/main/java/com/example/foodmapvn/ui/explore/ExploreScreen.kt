package com.example.foodmapvn.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.fake.FakeData
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.components.PagerDots
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.SearchBarBg
import com.example.foodmapvn.ui.theme.SuccessGreen

/** Nối ExploreViewModel với ExploreScreen. */
@Composable
fun ExploreRoute(
    onRegionClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExploreScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onRegionClick = onRegionClick,
        onLocateClick = onRegionClick,
        modifier = modifier,
    )
}

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onRegionClick: () -> Unit = {},
    onLocateClick: () -> Unit = {},
    onOpenFilterSheet: () -> Unit = {},
    onFilterSelected: (String) -> Unit = {},
    onDishClick: (Dish) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SearchHeader(
                state = state,
                onSearchClick = onSearchClick,
                onRegionClick = onRegionClick,
                onLocateClick = onLocateClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            FilterRow(
                filters = state.filters,
                selected = state.selectedFilter,
                onOpenFilterSheet = onOpenFilterSheet,
                onFilterSelected = onFilterSelected,
            )
        }
        if (state.dishes.isEmpty() && state.regionName.isNotEmpty()) {
            item {
                Text(
                    "Chưa có đặc sản cho ${state.regionName}. Dữ liệu đang được bổ sung — bấm tên khu vực ở trên để xem tỉnh/thành khác.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        }
        items(state.dishes, key = { it.id }) { dish ->
            DishCard(
                dish = dish,
                onClick = { onDishClick(dish) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SearchHeader(
    state: ExploreUiState,
    onSearchClick: () -> Unit,
    onRegionClick: () -> Unit,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SearchBarBg)
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kính lúp: mở Tìm kiếm (Tuần 2)
        IconButton(onClick = onSearchClick) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Tìm kiếm",
                modifier = Modifier.size(24.dp),
            )
        }
        // Tên khu vực: bấm để đổi tỉnh/thành (màn Chọn vùng)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRegionClick)
                .padding(vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.regionName, style = MaterialTheme.typography.titleMedium)
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = "Đổi khu vực",
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onLocateClick) {
            Icon(
                painter = painterResource(R.drawable.ic_my_location),
                contentDescription = "Vị trí của tôi",
            )
        }
    }
}

@Composable
private fun FilterRow(
    filters: List<String>,
    selected: String,
    onOpenFilterSheet: () -> Unit,
    onFilterSelected: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterChipItem(label = "Lọc", selected = false, onClick = onOpenFilterSheet) }
        items(filters) { filter ->
            FilterChipItem(
                label = filter,
                selected = filter == selected,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val background = if (selected) colors.primary else colors.surface
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        modifier = Modifier
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.outline, shape))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = content)
        Spacer(Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.ic_expand_more),
            contentDescription = null,
            tint = content.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DishCard(dish: Dish, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
        ) {
            FoodImage(dish.imageUrl, dish.art, Modifier.fillMaxSize())
            PagerDots(
                count = dish.photoCount,
                selected = 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(dish.name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoTag(
                icon = R.drawable.ic_verified,
                text = "${dish.verifiedPlaceCount} quán đã xác thực",
                iconTint = SuccessGreen,
            )
            InfoTag(icon = R.drawable.ic_place_outline, text = "Gần nhất ${dish.nearestDistance}")
        }
        if (dish.marketPrice != null || dish.bestTime != null) {
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                dish.marketPrice?.let {
                    InfoTag(icon = R.drawable.ic_verified, text = "Mặt bằng $it", iconTint = SuccessGreen)
                }
                dish.bestTime?.let {
                    InfoTag(icon = R.drawable.ic_place_outline, text = it)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append(dish.priceRange)
                    }
                    withStyle(
                        SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        append(" / ${dish.priceUnit}")
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text("Xem quán", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ExploreScreenPreview() {
    FoodmapvnTheme {
        ExploreScreen(
            state = ExploreUiState(
                regionName = "Huế",
                dishCount = 7,
                placeCount = 12,
                isOpenNow = true,
                filters = listOf(ExploreUiState.ALL, "Bún bò", "Cơm hến"),
                dishes = FakeData.dishes,
            ),
        )
    }
}
