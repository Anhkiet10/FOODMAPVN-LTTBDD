package com.example.foodmapvn.data.firebase

import kotlin.random.Random

/** Sinh tên ngắn dễ đọc: 4 chữ thường + 3 chữ số, vd. "afad123" (~457 triệu khả năng). */
object UsernameGenerator {
    private const val LETTERS = "abcdefghijkmnpqrstuvwxyz" // bỏ l, o để khỏi nhầm với 1, 0

    fun next(random: Random = Random.Default): String =
        buildString {
            repeat(4) { append(LETTERS[random.nextInt(LETTERS.length)]) }
            repeat(3) { append(random.nextInt(10)) }
        }
}
