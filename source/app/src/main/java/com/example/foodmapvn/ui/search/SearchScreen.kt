package com.example.foodmapvn.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.SearchBarBg
import com.example.foodmapvn.ui.theme.SuccessGreen

/** Nối SearchViewModel với SearchScreen. */
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onDishClick: (Dish) -> Unit,
    onPlaceClick: (PlaceSummary) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onSubmit = viewModel::saveQuery,
        onRecentClick = viewModel::useRecent,
        onRecentRemove = viewModel::removeRecent,
        onClearRecent = viewModel::clearRecent,
        onDishClick = { viewModel.saveQuery(); onDishClick(it) },
        onPlaceClick = { viewModel.saveQuery(); onPlaceClick(it) },
        onProvinceClick = { viewModel.selectProvince(it); onBack() },
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onRecentClick: (String) -> Unit = {},
    onRecentRemove: (String) -> Unit = {},
    onClearRecent: () -> Unit = {},
    onDishClick: (Dish) -> Unit = {},
    onPlaceClick: (PlaceSummary) -> Unit = {},
    onProvinceClick: (Province) -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Ô tìm kiếm
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
            }
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Món, quán ở ${state.areaName} hoặc tỉnh/thành…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        TextButton(onClick = { onQueryChange("") }) { Text("Xoá") }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SearchBarBg,
                    unfocusedContainerColor = SearchBarBg,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!state.isTyping) {
                recentSection(state.recent, onRecentClick, onRecentRemove, onClearRecent)
            } else if (!state.hasResults) {
                item {
                    Text(
                        "Không tìm thấy \"${state.query}\" ở ${state.areaName}. Thử tên khác, gõ không dấu, hoặc tìm tên tỉnh/thành.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            } else {
                if (state.dishes.isNotEmpty()) {
                    item { SectionTitle("Đặc sản ở ${state.areaName}") }
                    items(state.dishes, key = { "d-" + it.id }) { dish -> DishRow(dish, onClick = { onDishClick(dish) }) }
                }
                if (state.places.isNotEmpty()) {
                    item { SectionTitle("Quán ở ${state.areaName}") }
                    items(state.places, key = { "p-" + it.place.id }) { r -> PlaceRow(r, onClick = { onPlaceClick(r.place) }) }
                }
                if (state.provinces.isNotEmpty()) {
                    item { SectionTitle("Tỉnh, thành phố") }
                    items(state.provinces, key = { "r-" + it.id }) { p -> ProvinceRow(p, onClick = { onProvinceClick(p) }) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recentSection(
    recent: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (recent.isEmpty()) {
        item {
            Text(
                "Gõ tên món (vd. bun bo), tên quán, hoặc tên tỉnh/thành để đổi khu vực.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
        return
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
            Text("Tìm gần đây", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Xoá tất cả") }
        }
    }
    items(recent, key = { "h-$it" }) { q ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(q) }
                .padding(start = 20.dp, end = 4.dp),
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(14.dp))
            Text(q, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(q) }) {
                Icon(painterResource(R.drawable.ic_remove), contentDescription = "Xoá khỏi lịch sử", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResultRow(icon: Int, title: String, onClick: () -> Unit, details: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            details()
        }
    }
}

@Composable
private fun DishRow(dish: Dish, onClick: () -> Unit) = ResultRow(R.drawable.ic_star_border, dish.name, onClick) {
    Text(
        "${dish.priceRange}/${dish.priceUnit} · ${dish.verifiedPlaceCount} quán đã xác minh",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlaceRow(r: PlaceResult, onClick: () -> Unit) = ResultRow(R.drawable.ic_place_outline, r.place.name, onClick) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoTag(icon = R.drawable.ic_shield_check, text = "Uy tín ${r.place.trustScore}", iconTint = SuccessGreen)
        r.distance?.let { InfoTag(icon = R.drawable.ic_my_location, text = it) }
        when (r.isOpen) {
            true -> Text("Đang mở", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
            false -> Text("Đã đóng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            null -> Unit
        }
        if (r.place.priceWarning) Text("Cảnh báo giá", style = MaterialTheme.typography.bodySmall, color = BrickRed)
    }
}

@Composable
private fun ProvinceRow(p: Province, onClick: () -> Unit) = ResultRow(R.drawable.ic_place, p.name, onClick) {
    Text(
        "Chuyển khu vực sang " + p.name + if (p.formerNames.isEmpty()) "" else " (gồm cả ${p.formerNames.joinToString(", ")})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchPreview() {
    FoodmapvnTheme {
        SearchScreen(
            SearchUiState(
                query = "bun bo",
                areaName = "Huế",
                dishes = listOf(Dish("bun-bo-hue", "Bún bò Huế", 12, "350 m", priceRange = "35.000–50.000đ", priceUnit = "tô", art = FoodArtType.BUN_BO)),
                places = listOf(
                    PlaceResult(
                        PlaceSummary("p1", "Bún bò O Hạnh", "Phú Hội", LatLng(16.46, 107.59), "06:00", "21:00", 89, true, false, 4.6, listOf("bun-bo-hue")),
                        "350 m", true,
                    )
                ),
            )
        )
    }
}
