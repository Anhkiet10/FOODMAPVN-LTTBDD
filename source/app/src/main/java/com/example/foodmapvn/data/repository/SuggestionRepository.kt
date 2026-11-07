package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.NewSuggestion
import com.example.foodmapvn.data.model.PlaceSuggestion
import kotlinx.coroutines.flow.Flow

// Đề xuất quán mới của người dùng (UC-14)
interface SuggestionRepository {

    // Danh sách đề xuất của tôi mới nhất trước
    fun getMySuggestions(): Flow<List<PlaceSuggestion>>

    // Gửi đề xuất mới trạng thái "Chờ duyệt"
    suspend fun submit(input: NewSuggestion): Result<Unit>
}