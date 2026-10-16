package com.example.foodmapvn.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.R
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen

/** Icon nhỏ + chữ, ví dụ: (✓) 12 quán đã xác thực. */
@Composable
fun InfoTag(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Thanh tiến độ bo tròn. Chiều cao do nơi gọi quyết định qua modifier. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = SuccessGreen,
    trackColor: Color = SandDark,
) {
    val shape = RoundedCornerShape(50)
    Box(modifier.clip(shape).background(trackColor)) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(shape)
                    .background(color)
            )
        }
    }
}

/** 5 ngôi sao đánh giá. */
@Composable
fun StarRating(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 12.dp,
    color: Color = Amber,
) {
    Row(modifier) {
        repeat(5) { i ->
            Icon(
                painter = painterResource(if (i < rating) R.drawable.ic_star else R.drawable.ic_star_border),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(starSize),
            )
        }
    }
}
