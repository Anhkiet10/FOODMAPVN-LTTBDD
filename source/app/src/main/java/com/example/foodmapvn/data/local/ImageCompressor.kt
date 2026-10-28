package com.example.foodmapvn.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Thu nhỏ ảnh trước khi tải lên (ảnh điện thoại thường 3–10 MB, ảnh đại diện chỉ cần ~512 px):
 * tải nhanh hơn, đỡ tốn dung lượng Cloudinary gói Free. Trả về Uri của file JPEG trong cache của app.
 */
class ImageCompressor(private val context: Context) {

    suspend fun compress(uri: Uri, maxSize: Int = 512, quality: Int = 85): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(uri, maxSize)
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            bitmap.recycle()
            Uri.fromFile(file)
        }
    }

    private fun decode(uri: Uri, maxSize: Int): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder tự xoay ảnh đúng chiều (EXIF)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSize) {
                    val scale = maxSize.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSize) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Không đọc được ảnh")
        }
}
