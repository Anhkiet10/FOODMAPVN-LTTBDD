package com.example.foodmapvn.data.firebase.mapper

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint

/*
 * Đọc trường của document một cách an toàn.
 *
 * Firestore không có lược đồ: một trường có thể thiếu hoặc bị nhập sai kiểu
 * (ví dụ priceP25 = "35000" là chuỗi thay vì số). Các hàm getLong()/getString() của SDK
 * sẽ ném lỗi khi sai kiểu và làm app crash. Các hàm dưới đây trả về null thay vào đó,
 * đồng thời ghi log (Logcat, tag FoodMapData) để biết document nào nhập sai.
 */

private const val TAG = "FoodMapData"

private fun DocumentSnapshot.warnType(field: String, expected: String, actual: Any) {
    Log.w(TAG, "${reference.path}.$field: cần $expected nhưng nhận ${actual::class.java.simpleName} ($actual)")
}

internal fun DocumentSnapshot.str(field: String): String? = when (val v = get(field)) {
    null -> null
    is String -> v
    else -> { warnType(field, "string", v); null }
}

/** Số nguyên. Chấp nhận cả số thực (35000.0) vì Console đôi khi lưu kiểu double. */
internal fun DocumentSnapshot.long(field: String): Long? = when (val v = get(field)) {
    null -> null
    is Number -> v.toLong()
    else -> { warnType(field, "number", v); null }
}

internal fun DocumentSnapshot.int(field: String, default: Int = 0): Int = long(field)?.toInt() ?: default

internal fun DocumentSnapshot.double(field: String): Double? = when (val v = get(field)) {
    null -> null
    is Number -> v.toDouble()
    else -> { warnType(field, "number", v); null }
}

internal fun DocumentSnapshot.bool(field: String): Boolean? = when (val v = get(field)) {
    null -> null
    is Boolean -> v
    else -> { warnType(field, "boolean", v); null }
}

internal fun DocumentSnapshot.geo(field: String): GeoPoint? = when (val v = get(field)) {
    null -> null
    is GeoPoint -> v
    else -> { warnType(field, "geopoint", v); null }
}

internal fun DocumentSnapshot.time(field: String): Timestamp? = when (val v = get(field)) {
    null -> null
    is Timestamp -> v
    else -> { warnType(field, "timestamp", v); null }
}

internal fun DocumentSnapshot.strings(field: String): List<String> = when (val v = get(field)) {
    null -> emptyList()
    is List<*> -> v.filterIsInstance<String>()
    else -> { warnType(field, "array", v); emptyList() }
}
