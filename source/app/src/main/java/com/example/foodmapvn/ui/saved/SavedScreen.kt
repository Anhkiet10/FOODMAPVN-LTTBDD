package com.example.foodmapvn.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.fake.FakeData
import com.example.foodmapvn.data.model.RouteSuggestion
import com.example.foodmapvn.data.model.SavedDish
import com.example.foodmapvn.data.model.SavedPlace
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.components.ProgressBar
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

/** Nối SavedViewModel với SavedScreen. */
@Composable
fun SavedRoute(
    modifier: Modifier = Modifier,
    viewModel: SavedViewModel = viewModel(factory = SavedViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SavedScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onToggleEaten = viewModel::onToggleEaten,
        modifier = modifier,
    )
}

@Composable
fun SavedScreen(
    state: SavedUiState,
    modifier: Modifier = Modifier,
    onTabSelected: (SavedTab) -> Unit = {},
    onToggleEaten: (SavedDish) -> Unit = {},
    onDishClick: (SavedDish) -> Unit = {},
    onPlaceClick: (SavedPlace) -> Unit = {},
    onRouteClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Column {
                Text("Đã lưu", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                SegmentedTabs(
                    selected = state.selectedTab,
                    dishCount = state.dishes.size,
                    placeCount = state.places.size,
                    onSelect = onTabSelected,
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        when (state.selectedTab) {
            SavedTab.DISHES -> {
                item {
                    RegionProgress(
                        regionName = state.regionName,
                        eaten = state.eatenCount,
                        total = state.dishes.size,
                        progress = state.progress,
                    )
                }
                itemsIndexed(state.dishes, key = { _, d -> d.id }) { index, dish ->
                    Column {
                        SavedDishRow(
                            dish = dish,
                            onClick = { onDishClick(dish) },
                            onToggleEaten = { onToggleEaten(dish) },
                        )
                        if (index < state.dishes.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 72.dp),
                            )
                        }
                    }
                }
                val route = state.routeSuggestion
                if (route != null && state.remainingCount > 0) {
                    item {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            RouteSuggestionCard(route, state.remainingCount, onRouteClick)
                        }
                    }
                }
            }

            SavedTab.PLACES -> {
                if (state.places.isEmpty()) {
                    item {
                        Text(
                            "Bạn chưa lưu quán nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(state.places, key = { _, p -> p.id }) { index, place ->
                    Column {
                        SavedPlaceRow(place, onClick = { onPlaceClick(place) })
                        if (index < state.places.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 72.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(
    selected: SavedTab,
    dishCount: Int,
    placeCount: Int,
    onSelect: (SavedTab) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Sand)
            .padding(4.dp)
    ) {
        SavedTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val label = when (tab) {
                SavedTab.DISHES -> "Món muốn ăn ($dishCount)"
                SavedTab.PLACES -> "Quán đã lưu ($placeCount)"
            }
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RegionProgress(regionName: String, eaten: Int, total: Int, progress: Float) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(regionName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "$eaten / $total đã ăn",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SuccessGreen,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

@Composable
private fun SavedDishRow(dish: SavedDish, onClick: () -> Unit, onToggleEaten: () -> Unit) {
    val subtitle = when {
        dish.placeName == null -> "Chưa chọn quán"
        dish.distance == null -> dish.placeName
        else -> "${dish.placeName} · ${dish.distance}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodImage(
            dish.imageUrl,
            dish.art,
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = dish.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (dish.isEaten) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (dish.isEaten) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EatenCheck(checked = dish.isEaten, onClick = onToggleEaten)
    }
}

@Composable
private fun EatenCheck(checked: Boolean, onClick: () -> Unit) {
    val boxShape = RoundedCornerShape(6.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(boxShape)
                .background(if (checked) SuccessGreen else Color.White)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, SandDark, boxShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "Đã ăn",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
            color = if (checked) SuccessGreen else InkTertiary,
        )
    }
}

@Composable
private fun RouteSuggestionCard(route: RouteSuggestion, remaining: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Sand)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_place),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Gợi ý lộ trình ăn $remaining món còn lại",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${route.placeCount} quán · ${route.totalDistance} · ${route.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedPlaceRow(place: SavedPlace, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodImage(
            place.imageUrl,
            place.art,
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${place.dishName} · ${place.distance}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SuccessGreenSoft)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "${place.trustScore}",
                style = MaterialTheme.typography.labelLarge,
                color = SuccessGreen,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedScreenPreview() {
    FoodmapvnTheme {
        SavedScreen(
            state = SavedUiState(
                regionName = "Huế",
                dishes = FakeData.savedDishes,
                places = FakeData.savedPlaces,
                routeSuggestion = FakeData.routeSuggestion,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SavedPlacesPreview() {
    FoodmapvnTheme {
        SavedScreen(
            state = SavedUiState(
                selectedTab = SavedTab.PLACES,
                regionName = "Huế",
                dishes = FakeData.savedDishes,
                places = FakeData.savedPlaces,
            ),
        )
    }
}
