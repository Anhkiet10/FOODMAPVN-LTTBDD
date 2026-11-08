package com.example.foodmapvn.ui.explore

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
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
    onSearchClick: () -> Unit,
    onDishClick: (Dish) -> Unit,
    modifier: Modifier = Modifier,
    /** "Xem quán": mở Chi tiết quán của quán tốt nhất bán món đó. */
    onPlaceClick: (placeId: String) -> Unit = {},
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    // Quay lại màn (vd. vừa cấp quyền vị trí) -> lấy lại vị trí cho "gần nhất"
    LaunchedEffect(Unit) { viewModel.refreshLocation() }

    ExploreScreen(
        state = state,
        onSearchClick = onSearchClick,
        onDishClick = onDishClick,
        // Chưa có quán nào bán món -> mở Chi tiết đặc sản như cũ
        onViewPlaces = { dish -> viewModel.topPlaceFor(dish.id)?.let(onPlaceClick) ?: onDishClick(dish) },
        onOpenFilterSheet = { showFilterSheet = true },
        onFilterSelected = viewModel::onFilterSelected,
        onRegionClick = onRegionClick,
        onLocateClick = onRegionClick,
        modifier = modifier,
    )

    if (showFilterSheet) {
        FilterSheet(
            initial = state.advancedFilters,
            hasLocation = state.hasLocation,
            countPlaces = viewModel::countPlaces,
            onApply = {
                viewModel.onAdvancedFiltersChange(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false },
        )
    }
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
    onViewPlaces: (Dish) -> Unit = onDishClick,
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
                activeAdvancedCount = state.advancedFilters.activeCount,
                filters = state.filters,
                selected = state.selectedFilter,
                onOpenFilterSheet = onOpenFilterSheet,
                onFilterSelected = onFilterSelected,
            )
        }
        if (state.dishes.isEmpty() && state.regionName.isNotEmpty() && state.advancedFilters.activeCount > 0) {
            item {
                Text(
                    "Không có món nào có quán thỏa bộ lọc. Bấm \"Lọc\" để nới điều kiện.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        } else if (state.dishes.isEmpty() && state.regionName.isNotEmpty()) {
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
                onViewPlaces = { onViewPlaces(dish) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/**
 * Thanh trên cùng chia đôi:
 * - Trái: ô "Tìm món ăn…" – bấm là mở màn Tìm kiếm (bàn phím bật sẵn).
 * - Phải: khu vực đang chọn + số đặc sản/quán – bấm để đổi tỉnh/thành (màn Chọn vùng).
 */
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
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Bên trái: tìm món
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(SearchBarBg)
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Tìm kiếm món ăn",
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Tìm món ăn…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Bên phải: vị trí / khu vực
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(14.dp))
                .background(SearchBarBg)
                .clickable(onClick = onLocateClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_my_location),
                contentDescription = "Đổi khu vực",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.regionName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
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
        }
    }
}

@Composable
private fun FilterRow(
    activeAdvancedCount: Int,
    filters: List<String>,
    selected: String,
    onOpenFilterSheet: () -> Unit,
    onFilterSelected: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChipItem(
                label = if (activeAdvancedCount > 0) "Lọc ($activeAdvancedCount)" else "Lọc",
                selected = activeAdvancedCount > 0,
                onClick = onOpenFilterSheet,
            )
        }
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
            .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
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
private fun DishCard(dish: Dish, onClick: () -> Unit, onViewPlaces: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
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
                onClick = onViewPlaces,
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
