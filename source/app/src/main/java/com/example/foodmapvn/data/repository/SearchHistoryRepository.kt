package com.example.foodmapvn.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Lịch sử tìm kiếm gần đây (lưu trên máy, mới nhất trước). */
interface SearchHistoryRepository {
    val recent: StateFlow<List<String>>
    fun add(query: String)
    fun remove(query: String)
    fun clear()
}
