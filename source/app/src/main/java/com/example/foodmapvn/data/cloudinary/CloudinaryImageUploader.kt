package com.example.foodmapvn.data.cloudinary

import android.content.Context
import android.net.Uri
import com.example.foodmapvn.data.repository.ImageUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Tải ảnh lên Cloudinary bằng "unsigned upload preset" (không cần API secret trong app).
 * Cấu hình cloud name và preset trong res/values/cloudinary.xml.
 */
class CloudinaryImageUploader(
    private val context: Context,
    private val cloudName: String,
    private val uploadPreset: String,
    private val folder: String = "foodmapvn",
) : ImageUploader {

    val isConfigured: Boolean
        get() = !cloudName.startsWith("YOUR_") && !uploadPreset.startsWith("YOUR_")

    override suspend fun upload(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            check(isConfigured) { "Chưa cấu hình Cloudinary (res/values/cloudinary.xml)" }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Không đọc được ảnh")
            val boundary = "----FoodMap${UUID.randomUUID()}"
            val connection = (URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            DataOutputStream(connection.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("upload_preset", uploadPreset)
                field("folder", folder)
                out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n")
                out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            check(code in 200..299) { "Tải ảnh thất bại ($code): $body" }
            JSONObject(body).getString("secure_url")
        }
    }
}
