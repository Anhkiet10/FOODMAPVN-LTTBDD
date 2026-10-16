package com.example.foodmapvn.data.repository

import android.net.Uri

interface ImageUploader {
    /** Tải ảnh lên, trả về URL công khai để lưu vào Firestore. */
    suspend fun upload(uri: Uri): Result<String>
}
