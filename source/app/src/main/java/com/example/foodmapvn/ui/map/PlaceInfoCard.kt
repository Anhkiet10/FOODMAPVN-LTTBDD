package com.example.foodmapvn.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.domain.formatVnd
import com.example.foodmapvn.ui.components.InfoTag
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.ui.components.iconRes
import androidx.compose.foundation.border
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.SuccessGreen

private fun trustColor(p: PlaceSummary): Color =
    if (p.hasReviews) trustColor(p.trustScore) else Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_NONE))

private fun trustColor(score: Int): Color = when (TrustLevel.of(score)) {
    TrustLevel.HIGH -> Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_HIGH))
    TrustLevel.MEDIUM -> Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_MEDIUM))
    TrustLevel.LOW -> Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_LOW))
}

/**
 * Thẻ tóm tắt quán khi bấm ghim (bố cục theo Figma "Bản đồ quán"):
 * tên, uy tín / khoảng cách / giờ mở / cảnh báo giá, giá từ … và 2 nút "Chi tiết" + "Chỉ đường".
 * Giữ ghim thu nhỏ (icon loại món trong vòng màu uy tín) như ghim trên bản đồ của app.
 */
@Composable
fun PlaceInfoCard(
    card: PlaceCard,
    onDetailClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDirectionsClick: () -> Unit = {},
) {
    val p = card.place
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(end = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(FieldBorder)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(3.dp, trustColor(p), CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(p.category.iconRes), contentDescription = p.category.label, tint = Color(0xFF3A2A20), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(p.category.label, p.address).filter { it.isNotBlank() }.joinToString(" · "),
                        fontSize = 13.sp,
                        color = InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Đóng", tint = InkSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoTag(
                    icon = R.drawable.ic_verified,
                    text = if (p.hasReviews) "Uy tín ${p.trustScore}" else "Chưa có đánh giá",
                    iconTint = trustColor(p),
                )
                card.distance?.let { InfoTag(icon = R.drawable.ic_place_outline, text = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.ratingAvg > 0) {
                    InfoTag(icon = R.drawable.ic_star, text = String.format(java.util.Locale.US, "%.1f", p.ratingAvg).replace('.', ','), iconTint = Amber)
                }
                when (card.isOpen) {
                    true -> Text("Đang mở", fontSize = 14.sp, color = SuccessGreen)
                    false -> Text("Đã đóng", fontSize = 14.sp, color = InkSecondary)
                    null -> Unit
                }
            }
            if (p.priceWarning) {
                Spacer(Modifier.size(4.dp))
                Text("⚠ Quán bị cảnh báo giá cao hơn mặt bằng", fontSize = 13.sp, color = Amber, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                val minPrice = p.minPriceByDish.values.minOrNull()
                Text(
                    buildAnnotatedString {
                        if (minPrice != null) {
                            withStyle(SpanStyle(fontSize = 13.sp, color = InkSecondary)) { append("từ ") }
                            withStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)) { append("${formatVnd(minPrice)}đ") }
                        } else {
                            withStyle(SpanStyle(fontSize = 14.sp, color = InkSecondary)) { append("Chưa có giá") }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = onDetailClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FieldBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(40.dp),
                ) { Text("Chi tiết", color = Ink, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDirectionsClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(40.dp),
                ) { Text("Chỉ đường", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** Chú thích ghim (góc dưới trái): viền = điểm uy tín, chấm đỏ = cảnh báo giá, icon giữa = loại món. */
@Composable
fun PinLegend(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), shadowElevation = 2.dp) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Viền = uy tín", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            RingRow(trustColor(90), "≥ 80")
            RingRow(trustColor(70), "60 – 79")
            RingRow(trustColor(40), "Dưới 60")
            RingRow(Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_NONE)), "Chưa có đánh giá")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(PlaceLayers.COLOR_WARNING))))
                Spacer(Modifier.width(6.dp))
                Text("Cảnh báo giá", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RingRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).border(2.5.dp, color, CircleShape).background(Color.White))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
