package com.example.foodmapvn.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.example.foodmapvn.data.model.FoodArtType

/**
 * Ảnh món/quán: có imageUrl (Cloudinary) thì tải ảnh thật bằng Coil,
 * chưa có thì vẽ ảnh minh hoạ FoodArt (cũng dùng làm ảnh chờ và ảnh lỗi).
 */
@Composable
fun FoodImage(
    imageUrl: String?,
    fallbackArt: FoodArtType,
    modifier: Modifier = Modifier,
) {
    if (imageUrl.isNullOrBlank()) {
        FoodArt(fallbackArt, modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            loading = { FoodArt(fallbackArt, Modifier) },
            error = { FoodArt(fallbackArt, Modifier) },
            modifier = modifier,
        )
    }
}
