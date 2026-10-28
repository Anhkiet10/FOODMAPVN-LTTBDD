package com.example.foodmapvn.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.foodmapvn.data.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lưu tối đa [MAX] lượt tìm gần nhất trong SharedPreferences, không trùng lặp (không phân biệt hoa thường). */
class PrefsSearchHistoryRepository(context: Context) : SearchHistoryRepository {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val _recent = MutableStateFlow(
        prefs.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()
    )
    override val recent: StateFlow<List<String>> = _recent.asStateFlow()

    override fun add(query: String) {
        val q = query.trim().replace(SEPARATOR, " ")
        if (q.isBlank()) return
        save(listOf(q) + _recent.value.filterNot { it.equals(q, ignoreCase = true) })
    }

    override fun remove(query: String) = save(_recent.value - query)

    override fun clear() = save(emptyList())

    private fun save(list: List<String>) {
        val trimmed = list.take(MAX)
        prefs.edit { putString(KEY, trimmed.joinToString(SEPARATOR)) }
        _recent.value = trimmed
    }

    private companion object {
        const val KEY = "recent"
        const val SEPARATOR = "\n"
        const val MAX = 10
    }
}
