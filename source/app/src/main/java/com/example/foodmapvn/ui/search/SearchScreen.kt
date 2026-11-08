package com.example.foodmapvn.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.domain.ProvinceLocator
import com.example.foodmapvn.ui.components.FoodImage
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    // Chữ trong ô nhập giữ ngay tại màn hình (cập nhật tức thì theo từng phím gõ).
    // Trước đây ô lấy chữ từ ViewModel (đi qua combine + lọc 800 quán) nên trễ 1 nhịp
    // -> bàn phím nhận lại chữ cũ và làm mất ký tự khi gõ nhanh.
    var text by rememberSaveable { mutableStateOf(state.query) }
    val setText: (String) -> Unit = { text = it; onQueryChange(it) }
    val keyboard = LocalSoftwareKeyboardController.current
    // Mở màn là gõ được ngay: đợi ô nhập gắn vào màn rồi mới focus + bật bàn phím
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
    // Tab Món / Quán / Vùng (Figma). Tự nhảy sang tab đầu tiên có kết quả khi đổi từ khoá.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val counts = listOf(state.dishes.size, state.places.size, state.provinces.size)
    LaunchedEffect(state.query, counts) {
        if (counts[tab] == 0) counts.indexOfFirst { it > 0 }.takeIf { it >= 0 }?.let { tab = it }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Ô tìm kiếm: viền đỏ gạch, nút xoá tròn
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại", tint = Ink)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, BrickRed, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = InkSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text("Món, quán ở ${state.areaName} hoặc tỉnh/thành…", color = InkTertiary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = setText,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Ink),
                        cursorBrush = SolidColor(BrickRed),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (text.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9D3CD))
                            .clickable { setText("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Xoá", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (state.isTyping && state.hasResults) {
            SearchTabs(
                labels = listOf("Món", "Quán", "Vùng"),
                counts = counts,
                selected = tab,
                onSelect = { tab = it },
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!state.isTyping) {
                recentSection(state.recent, { q -> text = q; onRecentClick(q) }, onRecentRemove, onClearRecent)
            } else if (!state.hasResults) {
                item {
                    Text(
                        "Không tìm thấy \"${state.query}\" ở ${state.areaName}. Thử tên khác, gõ không dấu, hoặc tìm tên tỉnh/thành.",
                        color = InkSecondary,
                        modifier = Modifier.padding(20.dp),
                    )
                }
                recentSection(state.recent, { q -> text = q; onRecentClick(q) }, onRecentRemove, onClearRecent, showEmptyHint = false)
            } else {
                when (tab) {
                    0 -> {
                        item { SectionTitle(state.areaName) }
                        if (state.dishes.isEmpty()) item { EmptyTab("Không có món nào khớp ở ${state.areaName}.") }
                        items(state.dishes, key = { "d-" + it.id }) { dish ->
                            DishRow(dish, state.query, onClick = { onDishClick(dish) })
                        }
                    }
                    1 -> {
                        item { SectionTitle(state.areaName) }
                        if (state.places.isEmpty()) item { EmptyTab("Không có quán nào khớp ở ${state.areaName}.") }
                        items(state.places, key = { "p-" + it.place.id }) { r ->
                            PlaceRow(r, state.query, onClick = { onPlaceClick(r.place) })
                        }
                    }
                    else -> {
                        item { SectionTitle("Ở vùng khác") }
                        if (state.provinces.isEmpty()) item { EmptyTab("Không có tỉnh/thành nào khớp.") }
                        items(state.provinces, key = { "r-" + it.id }) { p ->
                            ProvinceCard(p, onClick = { onProvinceClick(p) })
                        }
                    }
                }
                // Figma: "Tìm gần đây" vẫn hiện dưới kết quả
                recentSection(state.recent, { q -> text = q; onRecentClick(q) }, onRecentRemove, onClearRecent, showEmptyHint = false)
            }
        }
    }
}

@Composable
private fun SearchTabs(labels: List<String>, counts: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            labels.forEachIndexed { i, label ->
                val active = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "$label (${counts[i]})",
                        color = if (active) BrickRed else InkSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) BrickRed else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = DividerColor)
    }
}

@Composable
private fun EmptyTab(text: String) {
    Text(text, color = InkSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
}

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.recentSection(
    recent: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    showEmptyHint: Boolean = true,
) {
    if (recent.isEmpty()) {
        if (showEmptyHint) item {
            Text(
                "Gõ tên món (vd. bun bo), tên quán, hoặc tên tỉnh/thành để đổi khu vực.",
                color = InkSecondary,
                modifier = Modifier.padding(20.dp),
            )
        }
        return
    }
    item {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp)) {
            Text("Tìm gần đây", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkSecondary, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Xóa", color = BrickRed, fontWeight = FontWeight.Bold) }
        }
    }
    item {
        // Figma: các từ khoá gần đây là chip bo tròn; giữ nút × để xoá từng từ (chức năng sẵn có)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            recent.forEach { q ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, FieldBorder, RoundedCornerShape(50))
                        .clickable { onClick(q) }
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(q, fontSize = 15.sp, color = Ink)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Xoá khỏi lịch sử",
                        tint = InkTertiary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onRemove(q) }
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = InkSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

/** Tô màu đỏ gạch phần tên khớp với từ khoá (so khớp không dấu, như bộ tìm kiếm). */
private fun highlight(name: String, query: String): AnnotatedString {
    fun fold(c: Char): Char = ProvinceLocator.normalize(c.toString()).firstOrNull() ?: ' '
    val folded = name.map(::fold).joinToString("")
    val q = ProvinceLocator.normalize(query)
    val start = if (q.isBlank()) -1 else folded.indexOf(q)
    return buildAnnotatedString {
        if (start < 0) { append(name); return@buildAnnotatedString }
        append(name.substring(0, start))
        withStyle(SpanStyle(color = BrickRed)) { append(name.substring(start, start + q.length)) }
        append(name.substring(start + q.length))
    }
}

@Composable
private fun ResultRow(
    title: AnnotatedString,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {},
    extra: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp),
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp, bottom = 8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 14.sp, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    extra()
                }
                trailing()
            }
            HorizontalDivider(color = DividerColor)
        }
    }
}

@Composable
private fun TrustBadge(score: Int) {
    Text(
        "$score",
        color = SuccessGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SuccessGreenSoft)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun DishRow(dish: Dish, query: String, onClick: () -> Unit) = ResultRow(
    title = highlight(dish.name, query),
    subtitle = "${dish.verifiedPlaceCount} quán xác thực · ${dish.priceRange}/${dish.priceUnit}",
    onClick = onClick,
    leading = {
        FoodImage(dish.imageUrl, dish.art, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
    },
)

@Composable
private fun PlaceRow(r: PlaceResult, query: String, onClick: () -> Unit) = ResultRow(
    title = highlight(r.place.name, query),
    subtitle = listOfNotNull(r.place.address.takeIf { it.isNotBlank() }, r.distance).joinToString(" · "),
    onClick = onClick,
    leading = {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Sand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_place_outline), contentDescription = null, tint = BrickRed)
        }
    },
    trailing = { TrustBadge(r.place.trustScore) },
    extra = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (r.isOpen) {
                true -> Text("Đang mở", fontSize = 12.sp, color = SuccessGreen)
                false -> Text("Đã đóng", fontSize = 12.sp, color = InkSecondary)
                null -> Unit
            }
            if (r.place.priceWarning) Text("Cảnh báo giá", fontSize = 12.sp, color = BrickRed)
        }
    },
)

/** Figma "Ở vùng khác": thẻ nền cát, nút "Đổi vùng" (bấm = đổi khu vực của cả app). */
@Composable
private fun ProvinceCard(p: Province, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Sand)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
            Text(
                (if (p.isCity) "Thành phố" else "Tỉnh") +
                    if (p.formerNames.isEmpty()) "" else " · gồm ${p.formerNames.joinToString(", ")} cũ",
                fontSize = 13.sp,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("Đổi vùng", color = BrickRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
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
                        PlaceSummary("p1", "Bún bò Mẫu A (Mẫu)", "Phú Hội", LatLng(16.46, 107.59), "06:00", "21:00", 89, true, false, 4.6, listOf("bun-bo-hue")),
                        "350 m", true,
                    )
                ),
            )
        )
    }
}
