package com.example.foodmapvn.ui.map

import androidx.compose.foundation.background
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

/** Thẻ tóm tắt quán khi bấm ghim: tên, uy tín, khoảng cách, giờ mở cửa, cảnh báo giá -> "Xem chi tiết". */
@Composable
fun PlaceInfoCard(
    card: PlaceCard,
    onDetailClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = card.place
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ghim thu nhỏ: icon loại món trong vòng màu uy tín
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(3.dp, trustColor(p), CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(p.category.iconRes), contentDescription = p.category.label, tint = Color(0xFF3A2A20), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    p.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Đóng", modifier = Modifier.size(18.dp))
                }
            }
            Text(p.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (p.address.isNotBlank()) {
                Text(p.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(trustColor(p)))
                    Spacer(Modifier.width(6.dp))
                    Text(if (p.hasReviews) "Uy tín ${p.trustScore}" else "Chưa có đánh giá", style = MaterialTheme.typography.bodyMedium)
                }
                if (p.ratingAvg > 0) Text("★ " + String.format(java.util.Locale.US, "%.1f", p.ratingAvg).replace('.', ','), style = MaterialTheme.typography.bodyMedium)
                card.distance?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                when (card.isOpen) {
                    true -> Text("Đang mở", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                    false -> Text("Đã đóng", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    null -> Unit
                }
            }
            if (p.priceWarning) {
                Spacer(Modifier.size(6.dp))
                Text("⚠ Quán bị cảnh báo giá cao hơn mặt bằng", style = MaterialTheme.typography.bodySmall, color = BrickRed)
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onDetailClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
            ) { Text("Xem chi tiết quán") }
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
