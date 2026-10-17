package com.example.foodmapvn.data.firebase

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.foodmapvn.data.firebase.mapper.str
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Nạp dữ liệu mẫu từ assets/seed-data.json lên Firestore (≈ chạy phần INSERT của file .sql).
 *
 * - File seed-data.json nằm trong app/src/debug/assets nên CHỈ có trong bản debug.
 * - Cần đăng nhập bằng tài khoản có role = "admin" (Security Rules chỉ cho Admin ghi danh mục).
 * - Ghi từng document theo thứ tự regions -> dishes -> places -> ... để Rules kiểm tra
 *   được khoá ngoại (dish phải thuộc region đã có, review phải thuộc place đã có).
 * - Dùng set() với ID cố định nên nạp lại nhiều lần không bị nhân đôi.
 */
class FirestoreSeeder(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    val isAvailable: Boolean by lazy {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        debuggable && runCatching { context.assets.open(ASSET_NAME).close() }.isSuccess
    }

    /** Trả về số document đã ghi. */
    suspend fun seed(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Hãy đăng nhập trước khi nạp dữ liệu mẫu")
            val userRef = db.collection(FirestorePaths.USERS).document(uid)
            val userSnap = userRef.get().awaitTask()
            check(userSnap.str("role") == "admin") {
                "Tài khoản cần role = admin. Vào Firestore Console, mở users/$uid và sửa role thành admin."
            }
            val userName = userSnap.str("name") ?: "Người dùng"
            val root = JSONObject(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() })
            var count = 0

            // 1. Danh mục (thứ tự quan trọng vì khoá ngoại)
            for (collection in listOf(FirestorePaths.REGIONS, FirestorePaths.DISHES, FirestorePaths.PLACES, FirestorePaths.CONFIG)) {
                val docs = root.optJSONObject(collection) ?: continue
                for (id in docs.keys()) {
                    db.collection(collection).document(id).set(toMap(docs.getJSONObject(id))).awaitTask()
                    count++
                }
            }

            // 2. Dữ liệu demo của tài khoản đang đăng nhập
            val demo = root.optJSONObject("currentUser")
            if (demo != null) {
                demo.optJSONObject("profile")?.let {
                    userRef.update(toMap(it)).awaitTask()
                    count++
                }
                for (sub in listOf(FirestorePaths.WISHLIST, FirestorePaths.SAVED_PLACES, FirestorePaths.BADGES, FirestorePaths.NOTIFICATIONS)) {
                    val docs = demo.optJSONObject(sub) ?: continue
                    for (id in docs.keys()) {
                        userRef.collection(sub).document(id).set(toMap(docs.getJSONObject(id))).awaitTask()
                        count++
                    }
                }
                demo.optJSONObject("reviews")?.let { reviews ->
                    for (placeId in reviews.keys()) {
                        val data = toMap(reviews.getJSONObject(placeId)) + mapOf(
                            "placeId" to placeId,
                            "userId" to uid,
                            "userName" to userName,
                        )
                        // ID ghép {placeId}_{uid}: mỗi người 1 đánh giá / quán
                        db.collection(FirestorePaths.REVIEWS).document("${placeId}_$uid").set(data).awaitTask()
                        count++
                    }
                }
            }
            count
        }
    }

    /** JSONObject -> Map cho Firestore, đổi các giá trị đặc biệt (_geo, _serverTimestamp, _date). */
    private fun toMap(obj: JSONObject): Map<String, Any?> =
        obj.keys().asSequence()
            .filterNot { it.startsWith("_") } // bỏ các khoá ghi chú như "_readme"
            .associateWith { convert(obj.get(it)) }

    private fun convert(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> when {
            value.has("_geo") -> value.getJSONArray("_geo").let { GeoPoint(it.getDouble(0), it.getDouble(1)) }
            value.has("_serverTimestamp") -> FieldValue.serverTimestamp()
            value.has("_date") -> Timestamp(DATE_FORMAT.parse(value.getString("_date"))!!)
            else -> toMap(value)
        }
        is JSONArray -> (0 until value.length()).map { convert(value.get(it)) }
        is Int -> value.toLong() // Firestore lưu số nguyên dạng Long
        else -> value // String, Long, Double, Boolean
    }

    companion object {
        const val ASSET_NAME = "seed-data.json"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
