package com.example.foodmapvn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.data.model.FoodArtType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ảnh minh hoạ món ăn vẽ bằng Canvas, dùng tạm thay cho ảnh thật.
 * Khi có ảnh thật: thay bằng AsyncImage (thư viện Coil) mà không cần sửa nơi gọi.
 */
@Composable
fun FoodArt(type: FoodArtType, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (type) {
            FoodArtType.BUN_BO -> drawBunBo()
            FoodArtType.CHE -> drawChe()
            FoodArtType.COM_HEN -> drawComHen()
        }
    }
}

/** Chấm phân trang ảnh (1/5, 2/5...) đặt đè lên ảnh. */
@Composable
fun PagerDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (i == selected) Color.White else Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

private val DrawScope.isWide get() = size.width > size.height * 1.4f

private fun DrawScope.drawBunBo() {
    drawRect(Color(0xFF6B4732))
    val r = size.minDimension * 0.44f
    val c = center
    drawCircle(Color(0xFF2E5A88), r, c)
    drawCircle(Color(0xFFF6EFE4), r * 0.92f, c)
    drawCircle(Color(0xFFD4602A), r * 0.82f, c)
    val noodle = Color(0xFFFFF3DD)
    for (i in 0 until 4) {
        val y = c.y - r * 0.25f + i * r * 0.17f
        drawLine(
            noodle,
            Offset(c.x - r * 0.55f, y),
            Offset(c.x + r * 0.5f, y - r * 0.08f),
            strokeWidth = r * 0.045f,
            cap = StrokeCap.Round,
        )
    }
    val beef = Color(0xFF5B2C20)
    drawOval(beef, Offset(c.x - r * 0.55f, c.y - r * 0.5f), Size(r * 0.42f, r * 0.24f))
    drawOval(beef, Offset(c.x + r * 0.05f, c.y - r * 0.55f), Size(r * 0.4f, r * 0.22f))
    drawOval(beef, Offset(c.x - r * 0.05f, c.y + r * 0.2f), Size(r * 0.42f, r * 0.24f))
    drawCircle(Color(0xFFF1DCC8), r * 0.12f, Offset(c.x - r * 0.35f, c.y + r * 0.15f))
    val herb = Color(0xFF4E9A3E)
    drawCircle(herb, r * 0.05f, Offset(c.x + r * 0.02f, c.y - r * 0.1f))
    drawCircle(herb, r * 0.04f, Offset(c.x + r * 0.15f, c.y - r * 0.02f))
    if (isWide) {
        // Lát chanh + đĩa rau bên cạnh
        drawArc(
            Color(0xFF9BCB3C), 180f, 180f, true,
            topLeft = Offset(c.x - r * 2.2f, c.y + r * 0.3f),
            size = Size(r * 0.6f, r * 0.6f),
        )
        drawCircle(Color(0xFFF4F4EC), r * 0.4f, Offset(c.x + r * 1.6f, c.y - r * 0.55f))
        drawCircle(Color(0xFF6BAF4A), r * 0.12f, Offset(c.x + r * 1.55f, c.y - r * 0.6f))
        drawCircle(Color(0xFF6BAF4A), r * 0.1f, Offset(c.x + r * 1.7f, c.y - r * 0.5f))
        drawLine(
            Color(0xFFC62828),
            Offset(c.x + r * 1.7f, c.y + r * 0.6f),
            Offset(c.x + r * 2.3f, c.y + r * 0.45f),
            strokeWidth = r * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCheBowl(c: Offset, r: Float, toppings: List<Color>) {
    drawCircle(Color(0xFFF3EEE4), r, c)
    drawCircle(Color(0xFFE9E1D0), r * 0.82f, c)
    toppings.forEachIndexed { i, color ->
        val a = i * 2 * PI / toppings.size
        drawCircle(
            color, r * 0.1f,
            Offset(c.x + cos(a).toFloat() * r * 0.45f, c.y + sin(a).toFloat() * r * 0.45f),
        )
    }
}

private fun DrawScope.drawChe() {
    drawRect(Color(0xFFD8C2A0))
    val r = size.minDimension * 0.42f
    val purple = Color(0xFF7B3F8C)
    val yellow = Color(0xFFF2C230)
    val brown = Color(0xFF6B3D2A)
    val jelly = Color(0xFFDDEFE3)
    if (isWide) {
        drawCheBowl(
            Offset(size.width * 0.38f, center.y), r,
            listOf(purple, yellow, brown, jelly, purple, yellow, brown, jelly),
        )
        val green = Color(0xFF2E7D4F)
        val red = Color(0xFFD84343)
        drawCheBowl(
            Offset(size.width * 0.68f, center.y), r * 0.75f,
            listOf(green, red, green, green, red, green),
        )
    } else {
        drawCheBowl(center, r, listOf(purple, yellow, brown, jelly, purple, yellow))
    }
}

private fun DrawScope.drawComHen() {
    drawRect(Color(0xFF7A5537))
    val r = size.minDimension * 0.42f
    val c = center
    drawCircle(Color(0xFFF3EEE4), r, c)
    drawCircle(Color(0xFFF8F3E6), r * 0.8f, c)
    val dots = listOf(
        Offset(-0.2f, -0.15f) to Color(0xFF4E9A3E),
        Offset(0.15f, -0.25f) to Color(0xFF4E9A3E),
        Offset(0.25f, 0.1f) to Color(0xFFE07A2E),
        Offset(-0.1f, 0.2f) to Color(0xFFE07A2E),
        Offset(0.05f, 0f) to Color(0xFF4E9A3E),
    )
    dots.forEach { (o, color) ->
        drawCircle(color, r * 0.08f, Offset(c.x + o.x * r, c.y + o.y * r))
    }
}

@Preview(widthDp = 320, heightDp = 130)
@Composable
private fun FoodArtWidePreview() {
    FoodArt(FoodArtType.BUN_BO, Modifier.size(320.dp, 130.dp))
}
