package com.example.foodmapvn.ui.region

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.model.RegionSummary
import com.example.foodmapvn.domain.ProvinceLocator
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/**
 * Danh sách 34 tỉnh/thành theo Figma "Chọn vùng":
 * ô tìm nền cát, dòng "Dùng vị trí hiện tại", thẻ "Đã có dữ liệu", danh sách A–Z có thanh chữ cái bên phải.
 * Dùng chung cho màn Chọn vùng và bảng chọn khu vực ở màn Bản đồ.
 *
 * Giữ nguyên chức năng cũ: gõ không dấu, tìm được cả tên tỉnh cũ (vd. "vung tau"), đánh dấu tỉnh đang chọn.
 *
 * @param myLocationSubtitle khác null -> hiện dòng "Dùng vị trí hiện tại" ở đầu danh sách.
 * @param summaries số đặc sản/quán theo id tỉnh; null = chưa biết (không hiện số, không hiện "Sắp có").
 * @param showFeatured hiện hàng thẻ "Đã có dữ liệu".
 */
@Composable
fun ProvinceList(
    provinces: List<Province>,
    selectedId: String?,
    onSelect: (Province) -> Unit,
    modifier: Modifier = Modifier,
    myLocationSubtitle: String? = null,
    onMyLocation: () -> Unit = {},
    summaries: Map<String, RegionSummary>? = null,
    showFeatured: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    val collator = remember { Collator.getInstance(Locale("vi")) }
    val sorted = remember(provinces) { provinces.sortedWith { a, b -> collator.compare(a.name, b.name) } }
    val filtered = remember(query, sorted) {
        val q = ProvinceLocator.normalize(query)
        if (q.isBlank()) sorted
        else sorted.filter { p -> (listOf(p.name) + p.formerNames).any { ProvinceLocator.normalize(it).contains(q) } }
    }
    fun hasData(p: Province) = (summaries?.get(p.id)?.dishCount ?: 0) > 0
    val featured = remember(sorted, summaries, selectedId) {
        sorted.filter { hasData(it) }.sortedByDescending { it.id == selectedId }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showHeader = query.isBlank()
    // Số item đứng trước danh sách tỉnh (để thanh chữ cái cuộn đúng chỗ)
    val headerCount = if (showHeader) {
        (if (myLocationSubtitle != null) 1 else 0) + (if (showFeatured && featured.isNotEmpty()) 1 else 0) + 1
    } else 0
    val letters = remember(filtered) { filtered.map { firstLetter(it.name) }.distinct() }

    Column(modifier) {
        SearchBox(query = query, onQueryChange = { query = it })
        Box(Modifier.weight(1f, fill = false)) {
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
                if (showHeader && myLocationSubtitle != null) {
                    item {
                        MyLocationRow(subtitle = myLocationSubtitle, onClick = onMyLocation)
                    }
                }
                if (showHeader && showFeatured && featured.isNotEmpty()) {
                    item {
                        FeaturedRow(featured, summaries.orEmpty(), onSelect)
                    }
                }
                if (showHeader) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 40.dp, top = 18.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Tất cả ${provinces.size} tỉnh, thành",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text("A – Z", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkSecondary)
                        }
                    }
                }
                items(filtered, key = { it.id }) { p ->
                    ProvinceRow(
                        province = p,
                        summary = summaries?.get(p.id),
                        countsKnown = summaries != null,
                        selected = p.id == selectedId,
                        onClick = { onSelect(p) },
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Không tìm thấy tỉnh/thành nào",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
                if (footer != null) item { footer() }
            }
            if (letters.size > 1) {
                // Thanh chữ cái A–Z (Figma): bấm để nhảy tới tỉnh bắt đầu bằng chữ đó
                Column(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    letters.forEach { letter ->
                        val active = selectedId != null && filtered.firstOrNull { it.id == selectedId }?.let { firstLetter(it.name) } == letter
                        Text(
                            letter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) BrickRed else InkTertiary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val index = filtered.indexOfFirst { firstLetter(it.name) == letter }
                                    if (index >= 0) scope.launch { listState.animateScrollToItem(headerCount + index) }
                                }
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun firstLetter(name: String): String = name.take(1).uppercase()

@Composable
private fun SearchBox(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SandDark)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = InkSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Tìm tỉnh, thành phố… (vd: Đà Nẵng)", color = InkTertiary, fontSize = 15.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Ink),
                cursorBrush = SolidColor(BrickRed),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = "Xóa",
                tint = InkSecondary,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun MyLocationRow(subtitle: String, onClick: () -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_my_location), contentDescription = null, tint = BrickRed, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Dùng vị trí hiện tại", color = BrickRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = InkSecondary, fontSize = 13.sp)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = InkSecondary)
        }
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun FeaturedRow(featured: List<Province>, summaries: Map<String, RegionSummary>, onSelect: (Province) -> Unit) {
    Column(Modifier.padding(top = 16.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Đã có dữ liệu", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
            Spacer(Modifier.width(8.dp))
            Text(
                "${featured.size}",
                color = SuccessGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SuccessGreenSoft)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(featured, key = { it.id }) { p ->
                RegionCard(p, summaries[p.id], onClick = { onSelect(p) })
            }
        }
    }
}

private val cardGradients = listOf(
    listOf(Color(0xFFE8A464), Color(0xFF7A3B2A)),
    listOf(Color(0xFF8CC6D9), Color(0xFF1F4E5F)),
    listOf(Color(0xFFF1C27B), Color(0xFF51603A)),
)

@Composable
private fun RegionCard(province: Province, summary: RegionSummary?, onClick: () -> Unit) {
    val image = when (province.id) {
        "hue" -> R.drawable.region_hue
        "dak-lak" -> R.drawable.region_daklak
        "can-tho" -> R.drawable.region_cantho
        else -> null
    }
    val subtitle = when {
        province.formerNames.isNotEmpty() && province.formerNames.size == 1 -> "${province.formerNames.first()} cũ"
        summary != null -> "${summary.dishCount} đặc sản"
        else -> ""
    }
    Box(
        Modifier
            .width(112.dp)
            .height(104.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        if (image != null) {
            Image(painterResource(image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            val colors = cardGradients[(province.id.hashCode() and 0x7fffffff) % cardGradients.size]
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(province.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            if (subtitle.isNotEmpty()) Text(subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ProvinceRow(
    province: Province,
    summary: RegionSummary?,
    countsKnown: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val hasData = (summary?.dishCount ?: 0) > 0
    Column(Modifier.padding(start = 16.dp, end = 32.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    province.name,
                    fontSize = 17.sp,
                    fontWeight = if (hasData || selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) BrickRed else Ink,
                )
                if (province.formerNames.isNotEmpty() && !hasData) {
                    Text("Gồm cả ${province.formerNames.joinToString(", ")}", fontSize = 12.sp, color = InkTertiary)
                }
            }
            when {
                hasData -> {
                    val former = if (province.formerNames.size == 1) "Gồm ${province.formerNames.first()} cũ · " else ""
                    Text(
                        "$former${summary!!.dishCount} đặc sản · ${summary.placeCount} quán",
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(SuccessGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                countsKnown -> Text(
                    "Sắp có",
                    color = InkTertiary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SandDark)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (selected) {
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(R.drawable.ic_check), contentDescription = "Đang chọn", tint = BrickRed)
            }
        }
        HorizontalDivider(color = DividerColor)
    }
}
