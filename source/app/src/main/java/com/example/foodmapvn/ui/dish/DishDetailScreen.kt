package com.example.foodmapvn.ui.dish

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
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
    /** placeId = quán cần chọn sẵn trên bản đồ (quán đang xem, hoặc quán đứng đầu danh sách). */
    onShowOnMap: (placeId: String?) -> Unit = {},
    /** Mở từ "Món của quán" ở Chi tiết quán -> quán đó. */
    fromPlaceId: String? = null,
    viewModel: DishDetailViewModel = viewModel(factory = DishDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loginGate = LocalLoginGate.current
    val context = LocalContext.current
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
        onShowOnMap = { onShowOnMap(fromPlaceId ?: state.places.firstOrNull()?.place?.id) },
        // "Xem quán": mở Chi tiết quán. Đến từ "Món của quán" -> quay về đúng quán đó (không mở chồng thêm màn)
        onViewPlace = {
            val target = fromPlaceId ?: state.places.firstOrNull()?.place?.id
            when {
                target == null -> Unit
                target == fromPlaceId -> onBack()
                else -> onPlaceClick(target)
            }
        },
        onShare = { dish ->
            val text = "${dish.name} – ${dish.priceRange}. Xem quán ngon trên Food Map VN"
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(Intent.createChooser(intent, "Chia sẻ món"))
        },
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
    onShowOnMap: () -> Unit = {},
    onViewPlace: () -> Unit = {},
    onShare: (DishDetail) -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.dish != null) MapBar(
                hasPlace = state.places.isNotEmpty(),
                onViewPlace = onViewPlace,
                onShowOnMap = onShowOnMap,
            )
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
                isSaved = state.isSaved,
                isSaving = state.isSaving,
                onBack = onBack,
                onPlaceClick = onPlaceClick,
                onSortChange = onSortChange,
                onToggleSave = onToggleSave,
                onShowOnMap = onShowOnMap,
                onShare = { onShare(state.dish) },
                contentPadding = padding,
            )
        }
    }
}

/** "35.000–50.000đ" -> "35 – 50k" (chip "Giá phổ biến" trong Figma). */
private fun shortPrice(range: String): String {
    val numbers = Regex("[0-9][0-9.]*").findAll(range).map { it.value.replace(".", "").toLongOrNull() }.filterNotNull().toList()
    return when (numbers.size) {
        0 -> range
        1 -> "${numbers[0] / 1000}k"
        else -> "${numbers[0] / 1000} – ${numbers[1] / 1000}k"
    }
}

@Composable
private fun DishContent(
    dish: DishDetail,
    places: List<DishPlace>,
    sort: PlaceSort,
    hasLocation: Boolean,
    isSaved: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onSortChange: (PlaceSort) -> Unit,
    onToggleSave: () -> Unit,
    onShowOnMap: () -> Unit,
    onShare: () -> Unit,
    contentPadding: PaddingValues,
) {
    val regionName = remember(dish.regionId) { VietnamProvinces.all.firstOrNull { it.id == dish.regionId }?.name.orEmpty() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
    ) {
        // Ảnh lớn + nút tròn trắng: quay lại / chia sẻ / lưu (Figma)
        item {
            Box {
                FoodImage(
                    imageUrl = dish.imageUrl,
                    fallbackArt = dish.art,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleButton(R.drawable.ic_arrow_back, "Quay lại", onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    CircleButton(R.drawable.ic_share, "Chia sẻ", onClick = onShare)
                    Spacer(Modifier.width(10.dp))
                    // Lưu vào "Món muốn ăn" (UC-10): nút bookmark, đổ màu khi đã lưu
                    CircleButton(
                        if (isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border,
                        if (isSaved) "Bỏ lưu" else "Lưu vào Món muốn ăn",
                        tint = if (isSaved) BrickRed else Ink,
                        enabled = !isSaving,
                        onClick = onToggleSave,
                    )
                }
            }
        }
        // Tấm trắng bo góc đè lên ảnh
        item {
            Column(
                Modifier
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(dish.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_place), contentDescription = null, tint = BrickRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        listOfNotNull(
                            regionName.takeIf { it.isNotEmpty() }?.let { "Đặc sản $it" },
                            dish.bestTime ?: dish.category.label,
                        ).joinToString(" · "),
                        fontSize = 15.sp,
                        color = InkSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val verified = dish.verifiedPlaceCount > 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (verified) SuccessGreenSoft else SandDark)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        if (verified) {
                            Icon(painterResource(R.drawable.ic_verified), contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (verified) "${dish.verifiedPlaceCount} quán đã xác thực" else "Chưa có quán xác thực",
                            color = if (verified) SuccessGreen else InkSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    Text(
                        if (dish.priceRange == "Chưa có giá") "Chưa có giá" else "Giá phổ biến ${shortPrice(dish.priceRange)}",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SandDark)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                if (dish.description.isNotBlank()) {
                    Text(dish.description, fontSize = 15.sp, lineHeight = 22.sp, color = Ink)
                }
                if (dish.tips.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Sand)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            if (regionName.isNotEmpty()) "Mẹo ăn như người $regionName" else "Mẹo ăn",
                            color = BrickRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        dish.tips.forEach { tip ->
                            Row {
                                Text("•", fontSize = 15.sp, color = Ink)
                                Spacer(Modifier.width(10.dp))
                                Text(tip, fontSize = 15.sp, color = Ink)
                            }
                        }
                    }
                }
                Text(
                    "${dish.priceRange}${if (dish.priceRange != "Chưa có giá") " / ${dish.priceUnit}" else ""} · mặt bằng giá do cộng đồng báo cáo",
                    fontSize = 12.sp,
                    color = InkTertiary,
                )
            }
        }
        // Quán ngon gần bạn
        item {
            Column(Modifier.offset(y = (-12).dp)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quán ngon gần bạn", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(
                        "Xem bản đồ",
                        color = BrickRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onShowOnMap).padding(4.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlaceSort.entries) { option ->
                        val selected = sort == option
                        Text(
                            option.label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) BrickRed else Color.White)
                                .then(if (selected) Modifier else Modifier.border(1.dp, FieldBorder, RoundedCornerShape(50)))
                                .clickable { onSortChange(option) }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                }
                if (sort == PlaceSort.DISTANCE && !hasLocation) {
                    Text(
                        "Chưa có vị trí của bạn — đang xếp theo uy tín.",
                        fontSize = 12.sp,
                        color = InkSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (places.isEmpty()) {
            item {
                Text(
                    "Chưa có quán nào bán món này trên bản đồ.",
                    fontSize = 14.sp,
                    color = InkSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(places, key = { it.place.id }) { item ->
            PlaceRow(item, dish, onClick = { onPlaceClick(item.place.id) })
        }
    }
}

@Composable
private fun CircleButton(icon: Int, description: String, onClick: () -> Unit, tint: Color = Ink, enabled: Boolean = true) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Màu điểm uy tín, cùng quy ước với ghim bản đồ (≥ 80 xanh, 60–79 vàng, < 60 xám, chưa có đánh giá xám nhạt). */
/** Có điểm uy tín thật chưa (đã có đánh giá và điểm > 0); chưa có thì hiện "Mới". */
private fun isRated(place: PlaceSummary): Boolean = place.hasReviews && place.trustScore > 0

private fun trustColor(place: PlaceSummary): Color = when {
    !isRated(place) -> InkTertiary
    place.trustScore >= 80 -> SuccessGreen
    place.trustScore >= 60 -> Amber
    else -> InkSecondary
}

@Composable
private fun PlaceRow(item: DishPlace, dish: DishDetail, onClick: () -> Unit) {
    val p = item.place
    val warn = p.priceWarning
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ảnh món thu nhỏ, viền màu uy tín (giữ quy ước vòng uy tín của ghim bản đồ)
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, trustColor(p), RoundedCornerShape(12.dp))
        ) {
            FoodImage(dish.imageUrl, dish.art, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp, bottom = 8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val parts = buildList {
                            if (warn) add("Giá cao hơn mặt bằng")
                            item.distanceMeters?.let { add(GeoUtils.formatDistance(it)) }
                            when (item.isOpen) {
                                true -> add("Đang mở")
                                false -> add("Đã đóng")
                                null -> Unit
                            }
                            if (!warn && isEmpty()) add(p.address)
                        }
                        when {
                            warn -> Text("⚠", color = Amber, fontSize = 13.sp)
                            p.isVerified -> Icon(painterResource(R.drawable.ic_verified), contentDescription = "Đã xác thực", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                        if (warn || p.isVerified) Spacer(Modifier.width(6.dp))
                        Text(
                            parts.joinToString(" · "),
                            fontSize = 13.sp,
                            color = if (warn) Amber else InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val color = trustColor(p)
                    Text(
                        if (isRated(p)) "${p.trustScore}" else "Mới",
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    item.minPrice?.let {
                        Text("từ ${it / 1000}k", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (warn) Amber else Ink)
                    }
                }
            }
            HorizontalDivider(color = DividerColor)
        }
    }
}

/** Nút dưới cùng (Figma): "Xem N quán trên bản đồ". Nút Lưu chuyển lên biểu tượng bookmark ở ảnh. */
@Composable
private fun MapBar(hasPlace: Boolean, onViewPlace: () -> Unit, onShowOnMap: () -> Unit) {
    // 2 nút: "Xem quán" (Chi tiết quán) + "Xem trên bản đồ" (tab Bản đồ, chọn sẵn quán)
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onViewPlace,
                enabled = hasPlace,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (hasPlace) BrickRed else FieldBorder),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text("Xem quán", color = if (hasPlace) BrickRed else InkTertiary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Button(
                onClick = onShowOnMap,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                modifier = Modifier.weight(1.4f).height(52.dp),
            ) {
                Icon(painterResource(R.drawable.ic_place), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xem trên bản đồ", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
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
