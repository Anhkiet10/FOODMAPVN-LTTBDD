package com.example.foodmapvn.domain

import java.text.Normalizer

/** So khớp chữ cho Tìm kiếm: bỏ dấu, không phân biệt hoa thường ("bun bo" khớp "Bún bò Huế"). */
object TextSearch {

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('đ', 'd').replace('Đ', 'D')
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Mọi từ trong câu tìm đều xuất hiện trong một trong các [texts]. */
    fun matches(query: String, vararg texts: String): Boolean {
        val words = normalize(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        val haystack = texts.joinToString(" ") { normalize(it) }
        return words.all { haystack.contains(it) }
    }
}
