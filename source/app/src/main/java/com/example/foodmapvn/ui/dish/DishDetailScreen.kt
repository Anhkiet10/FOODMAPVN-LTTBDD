package com.example.foodmapvn.ui.dish

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.DishDetail
import com.example.foodmapvn.data.model.DishPlace
import com.example.foodmapvn.data.model.FoodArtType
import com.example.foodmapvn.data.model.FoodCategory
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.PlaceSort
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.domain.GeoUtils
import com.example.foodmapvn.domain.formatVnd
import com.example.foodmapvn.ui.auth.LocalLoginGate
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.components.iconRes
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SuccessGreen

/** Nối DishDetailViewModel với màn hình (route "dish/{dishId}"). */
@Composable
fun DishDetailRoute(
    onBack: () -> Unit,
    onPlaceClick: (placeId: String) -> Unit,
    viewModel: DishDetailViewModel = viewModel(factory = DishDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loginGate = LocalLoginGate.current
    // Quay lại màn (vd. vừa cấp quyền vị trí) -> lấy lại vị trí
    LaunchedEffect(Unit) { viewModel.refreshLocation() }

    DishDetailScreen(
        state = state,
        onBack = onBack,
        onPlaceClick = onPlaceClick,
        onSortChange = viewModel::onSortChange,
        // Lưu món cần tài khoản: Khách được đưa sang Đăng nhập rồi quay lại đây
        onToggleSave = { loginGate.requireLogin { viewModel.toggleSave() } },
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun DishDetailScreen(
    state: DishDetailUiState,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onSortChange: (PlaceSort) -> Unit,
    onToggleSave: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.dish != null) SaveBar(isSaved = state.isSaved, isSaving = state.isSaving, onClick = onToggleSave)
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.notFound || state.dish == null -> NotFound(onBack)
            else -> DishContent(
                dish = state.dish,
                places = state.places,
                sort = state.sort,
                hasLocation = state.hasLocation,
                onBack = onBack,
                onPlaceClick = onPlaceClick,
                onSortChange = onSortChange,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun DishContent(
    dish: DishDetail,
    places: List<DishPlace>,
    sort: PlaceSort,
    hasLocation: Boolean,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onSortChange: (PlaceSort) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
    ) {
        // Ảnh + nút quay lại
        item {
            Box {
                FoodImage(
                    imageUrl = dish.imageUrl,
                    fallbackArt = dish.art,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    modifier = Modifier.statusBarsPadding().padding(8.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                }
            }
        }
        // Tên, loại, giá
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(dish.category.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dish.category.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(dish.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dish.priceRange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (dish.priceRange != "Chưa có giá") {
                        Text(" / ${dish.priceUnit}", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    }
                }
                Text("Mặt bằng giá do cộng đồng báo cáo", style = MaterialTheme.typography.bodySmall, color = InkTertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    dish.bestTime?.let { InfoTag(R.drawable.ic_star, it) }
                    InfoTag(R.drawable.ic_verified, "${dish.verifiedPlaceCount} quán đã xác thực", iconTint = SuccessGreen)
                }
            }
        }
        // Giới thiệu
        if (dish.description.isNotBlank()) {
            item {
                Section("Giới thiệu")
                Text(dish.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp))
            }
        }
        // Mẹo ăn
        if (dish.tips.isNotEmpty()) {
            item { Section("Mẹo ăn") }
            items(dish.tips) { tip ->
                Row(Modifier.padding(horizontal = 20.dp, vertical = 3.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        // Quán ngon gần bạn
        item {
            Section("Quán ngon gần bạn (${places.size})")
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceSort.entries.forEach { option ->
                    FilterChip(selected = sort == option, onClick = { onSortChange(option) }, label = { Text(option.label) })
                }
            }
            if (sort == PlaceSort.DISTANCE && !hasLocation) {
                Text(
                    "Chưa có vị trí của bạn — đang xếp theo uy tín.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (places.isEmpty()) {
            item {
                Text(
                    "Chưa có quán nào bán món này trên bản đồ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        items(places, key = { it.place.id }) { item ->
            PlaceRow(item, onClick = { onPlaceClick(item.place.id) })
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Màu viền uy tín, cùng quy ước với ghim bản đồ (≥ 80 xanh, 60–79 vàng, < 60 xám, chưa có đánh giá xám nhạt). */
private fun trustColor(place: PlaceSummary): Color = when {
    !place.hasReviews -> InkTertiary
    place.trustScore >= 80 -> SuccessGreen
    place.trustScore >= 60 -> Amber
    else -> InkSecondary
}

@Composable
private fun PlaceRow(item: DishPlace, onClick: () -> Unit) {
    val p = item.place
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ghim thu nhỏ: icon loại món trong vòng màu uy tín
        Box(
            Modifier.size(40.dp).clip(CircleShape).border(3.dp, trustColor(p), CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(p.category.iconRes), contentDescription = null, tint = Color(0xFF3A2A20), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (p.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(painterResource(R.drawable.ic_verified), contentDescription = "Đã xác thực", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                }
            }
            Text(p.address, style = MaterialTheme.typography.bodySmall, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (p.hasReviews) "Uy tín ${p.trustScore}" else "Chưa có đánh giá",
                    style = MaterialTheme.typography.labelMedium,
                    color = trustColor(p),
                    fontWeight = FontWeight.SemiBold,
                )
                item.distanceMeters?.let { Text(GeoUtils.formatDistance(it), style = MaterialTheme.typography.labelMedium, color = InkSecondary) }
                when (item.isOpen) {
                    true -> Text("Đang mở", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                    false -> Text("Đã đóng", style = MaterialTheme.typography.labelMedium, color = InkTertiary)
                    null -> Unit
                }
            }
            if (p.priceWarning) {
                Text("⚠ Cảnh báo giá cao hơn mặt bằng", style = MaterialTheme.typography.labelSmall, color = BrickRed)
            }
        }
        item.minPrice?.let {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("từ", style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                Text("${formatVnd(it)}đ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SaveBar(isSaved: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (isSaved) {
                OutlinedButton(onClick = onClick, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đã lưu · Bấm để bỏ lưu")
                }
            } else {
                Button(onClick = onClick, enabled = !isSaving, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Lưu vào Món muốn ăn")
                }
            }
        }
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Không tìm thấy món này", style = MaterialTheme.typography.titleMedium)
        Text("Món có thể đã bị ẩn hoặc xoá.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Quay lại") }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 900)
@Composable
private fun DishDetailPreview() {
    val dish = DishDetail(
        id = "bun-bo-hue", regionId = "hue", name = "Bún bò Huế",
        description = "Nước dùng hầm xương, thơm sả và mắm ruốc; ăn kèm giò heo, chả cua, bò tái và rau sống.",
        tips = listOf("Ăn buổi sáng 6–9h, nước dùng ngon nhất", "Trên 60.000đ/tô là cao hơn mặt bằng"),
        bestTime = "Ngon nhất buổi sáng", priceRange = "35.000–50.000đ", priceUnit = "tô",
        category = FoodCategory.NOODLE_SOUP, art = FoodArtType.BUN_BO, imageUrl = null, verifiedPlaceCount = 12,
    )
    fun place(id: String, name: String, trust: Int, rated: Boolean) = PlaceSummary(
        id = id, name = name, address = "Phường Phú Hội, Huế", location = LatLng(16.46, 107.59),
        openTime = "06:00", closeTime = "21:00", trustScore = trust, isVerified = trust >= 80,
        priceWarning = false, ratingAvg = 4.5, dishIds = listOf("bun-bo-hue"),
        category = FoodCategory.NOODLE_SOUP, hasReviews = rated,
    )
    FoodmapvnTheme {
        DishDetailScreen(
            state = DishDetailUiState(
                isLoading = false, dish = dish,
                places = listOf(
                    DishPlace(place("a", "Bún bò Mẫu A (Mẫu)", 89, true), 350.0, 45_000, true),
                    DishPlace(place("b", "Bún bò bà Nga", 0, false), 1200.0, null, null),
                ),
                hasLocation = true,
            ),
            onBack = {}, onPlaceClick = {}, onSortChange = {}, onToggleSave = {}, onMessageShown = {},
        )
    }
}
