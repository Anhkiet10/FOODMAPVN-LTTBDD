package com.example.foodmapvn.domain

data class PriceRange(val low: Long, val high: Long)

/** BR-06, BR-07: mặt bằng giá và cảnh báo giá. Giá tính bằng VNĐ. */
class PriceRangeCalculator(private val rules: BusinessRules = BusinessRules()) {

    /** Mặt bằng giá = từ phân vị 25% đến 75%. Cần ít nhất 4 mẫu giá. */
    fun priceRange(prices: List<Long>): PriceRange? {
        if (prices.size < 4) return null
        val sorted = prices.sorted()
        return PriceRange(percentile(sorted, 0.25), percentile(sorted, 0.75))
    }

    fun median(prices: List<Long>): Long? =
        if (prices.isEmpty()) null else percentile(prices.sorted(), 0.5)

    /** Có cảnh báo giá khi giá trung vị tại quán vượt cận trên quá ngưỡng. */
    fun isOverpriced(placeMedianPrice: Long, regionRange: PriceRange): Boolean =
        placeMedianPrice > regionRange.high * (1 + rules.priceWarningThreshold)

    /** Nội suy tuyến tính trên danh sách đã sắp xếp. */
    private fun percentile(sorted: List<Long>, p: Double): Long {
        val pos = p * (sorted.size - 1)
        val lower = sorted[pos.toInt()]
        val upper = sorted[minOf(pos.toInt() + 1, sorted.lastIndex)]
        return (lower + (upper - lower) * (pos - pos.toInt())).toLong()
    }
}

/** 35000 -> "35.000" */
fun formatVnd(amount: Long): String =
    String.format(java.util.Locale.US, "%,d", amount).replace(',', '.')

/** 35000 -> "35k" */
fun formatThousands(amount: Long): String = "${amount / 1000}k"
