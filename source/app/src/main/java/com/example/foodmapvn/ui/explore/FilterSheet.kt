package com.example.foodmapvn.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.domain.ExploreFilters
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen

private const val PRICE_MIN = 10_000f
private const val PRICE_MAX = 150_000f
// Tối đa 10 km
private val DISTANCE_OPTIONS = listOf("1 km" to 1.0, "3 km" to 3.0, "5 km" to 5.0, "10 km" to 10.0)

/**
 * Bộ lọc nâng cao (UC-05) theo Figma "Bộ lọc":
 * khoảng giá 2 đầu, chip khoảng cách, thanh điểm uy tín, 3 công tắc, nút "Xem N quán".
 * Chỉnh trong bảng, bấm nút dưới cùng mới lọc (như cũ).
 *
 * @param countPlaces đếm số quán thỏa bộ lọc đang chỉnh (null -> nút ghi "Áp dụng").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    initial: ExploreFilters,
    hasLocation: Boolean,
    onApply: (ExploreFilters) -> Unit,
    onDismiss: () -> Unit,
    countPlaces: ((ExploreFilters) -> Int)? = null,
) {
    var draft by remember { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bộ lọc", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                Text(
                    "Đặt lại",
                    color = BrickRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { draft = ExploreFilters() }
                        .padding(6.dp),
                )
            }
            Spacer(Modifier.height(18.dp))

            // Khoảng giá (mỗi phần)
            val low = (draft.minPrice ?: PRICE_MIN.toLong()).toFloat()
            val high = (draft.maxPrice ?: PRICE_MAX.toLong()).toFloat()
            SectionHeader("Khoảng giá (mỗi phần)", "${k(low)} – ${k(high)}${if (draft.maxPrice == null) "+" else ""}", BrickRed)
            RangeSlider(
                value = low..high,
                onValueChange = { r ->
                    val lo = (r.start / 5_000).toInt() * 5_000L
                    val hi = (r.endInclusive / 5_000).toInt() * 5_000L
                    draft = draft.copy(
                        minPrice = lo.takeIf { it > PRICE_MIN.toLong() },
                        maxPrice = hi.takeIf { it < PRICE_MAX.toLong() },
                    )
                },
                valueRange = PRICE_MIN..PRICE_MAX,
                colors = sliderColors(BrickRed),
            )
            Row {
                Text("10k", fontSize = 12.sp, color = InkTertiary, modifier = Modifier.weight(1f))
                Text("150k", fontSize = 12.sp, color = InkTertiary)
            }
            Spacer(Modifier.height(20.dp))

            // Khoảng cách
            Text(
                if (hasLocation) "Khoảng cách" else "Khoảng cách (cần quyền vị trí)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (hasLocation) Ink else InkTertiary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DISTANCE_OPTIONS.forEach { (label, km) ->
                    val selected = draft.maxDistanceKm == km
                    Text(
                        label,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            selected -> Color.White
                            hasLocation -> Ink
                            else -> InkTertiary
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) BrickRed else Color.White)
                            .then(if (selected) Modifier else Modifier.border(1.dp, FieldBorder, RoundedCornerShape(50)))
                            .clickable(enabled = hasLocation) {
                                // Bấm lại chip đang chọn -> bỏ lọc khoảng cách
                                draft = draft.copy(maxDistanceKm = if (selected) null else km)
                            }
                            .padding(vertical = 9.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Điểm uy tín tối thiểu
            val trust = draft.minTrust ?: 0
            SectionHeader("Điểm uy tín tối thiểu", if (trust == 0) "Bất kỳ" else "≥ $trust", SuccessGreen)
            Slider(
                value = trust.toFloat(),
                onValueChange = { v ->
                    val t = (v / 5).toInt() * 5
                    draft = draft.copy(minTrust = t.takeIf { it > 0 })
                },
                valueRange = 0f..100f,
                colors = sliderColors(SuccessGreen),
            )
            Spacer(Modifier.height(8.dp))

            SwitchRow("Đang mở cửa", "Ẩn quán đã đóng giờ này", draft.openNow) { draft = draft.copy(openNow = it) }
            // Figma có công tắc này nhưng dữ liệu huy hiệu chưa có -> hiện để giữ bố cục, tạm khoá
            SwitchRow("Có người địa phương đánh giá", "Ít nhất 1 đánh giá có huy hiệu · sắp có", false, enabled = false) {}
            SwitchRow("Ẩn quán bị cảnh báo giá", "Giá > 1,5 lần trung vị vùng", draft.hidePriceWarning) {
                draft = draft.copy(hidePriceWarning = it)
            }

            Spacer(Modifier.height(20.dp))
            val count = countPlaces?.invoke(draft)
            Button(
                onClick = { onApply(draft) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    if (count != null) "Xem $count quán" else if (draft.isEmpty) "Áp dụng" else "Áp dụng (${draft.activeCount})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun k(value: Float): String = "${(value / 1000).toInt()}k"

@Composable
private fun SectionHeader(title: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = valueColor)
    }
}

@Composable
private fun sliderColors(active: Color) = SliderDefaults.colors(
    // Nút kéo cùng màu thanh để thấy rõ (nút trắng bị chìm vào nền trắng)
    thumbColor = active,
    activeTrackColor = active,
    inactiveTrackColor = SandDark,
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (enabled) Ink else InkTertiary)
                Text(subtitle, fontSize = 14.sp, color = InkSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SuccessGreen,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = SandDark,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = SandDark,
                ),
            )
        }
        HorizontalDivider(color = DividerColor)
    }
}
