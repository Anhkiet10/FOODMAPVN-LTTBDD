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

    /** Số quán cũ (của dữ liệu mẫu trước) đã xoá ở lần nạp gần nhất. */
    var lastRemovedCount: Int = 0
        private set

    /** Trả về số document đã ghi. */
    suspend fun seed(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var removedCount = 0
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
            // Ghi theo lô (tối đa 500 thao tác/lô; dùng 400) -> ~2.000 document chỉ mất vài giây.
            // Hết collection mới sang collection sau, nên vùng luôn có trước món/quán (Rules kiểm tra khoá ngoại).
            for (collection in listOf(FirestorePaths.REGIONS, FirestorePaths.DISHES, FirestorePaths.PLACES, FirestorePaths.CONFIG)) {
                val docs = root.optJSONObject(collection) ?: continue
                batchesOf(docs).forEach { ids ->
                    val batch = db.batch()
                    ids.forEach { id -> batch.set(db.collection(collection).document(id), toMap(docs.getJSONObject(id))) }
                    batch.commit().awaitTask()
                    count += ids.size
                }
            }

            // 1b. Dọn quán do dữ liệu mẫu CŨ tạo ra mà bản mới không còn (vd. quán không bán đặc sản).
            // Chỉ xoá ID "osm-…" / "mau-…" (do file seed sinh ra), không đụng quán người dùng đề xuất.
            root.optJSONObject(FirestorePaths.PLACES)?.let { newPlaces ->
                val keep = newPlaces.keys().asSequence().toSet()
                val stale = db.collection(FirestorePaths.PLACES).get().awaitTask().documents
                    .map { it.id }
                    .filter { (it.startsWith("osm-") || it.startsWith("mau-")) && it !in keep }
                stale.chunked(BATCH_SIZE).forEach { ids ->
                    val batch = db.batch()
                    ids.forEach { batch.delete(db.collection(FirestorePaths.PLACES).document(it)) }
                    batch.commit().awaitTask()
                }
                removedCount = stale.size
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
            lastRemovedCount = removedCount
            count
        }
    }

    /**
     * Chia document thành các lô: ≤ [BATCH_SIZE] document và ≤ [MAX_REGIONS_PER_BATCH] tỉnh khác nhau.
     * Rules kiểm tra regionExists() cho từng món/quán; một lô chỉ được gọi exists/get tới khoảng 20
     * document khác nhau, nên gom theo tỉnh để không vượt giới hạn.
     */
    private fun batchesOf(docs: JSONObject): List<List<String>> {
        val ids = docs.keys().asSequence().toList()
            .sortedBy { docs.getJSONObject(it).optString("regionId") }
        val result = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        val regions = mutableSetOf<String>()
        for (id in ids) {
            val region = docs.getJSONObject(id).optString("regionId")
            if (current.size >= BATCH_SIZE || (region !in regions && regions.size >= MAX_REGIONS_PER_BATCH)) {
                result += current
                current = mutableListOf()
                regions.clear()
            }
            current += id
            regions += region
        }
        if (current.isNotEmpty()) result += current
        return result
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
        private const val BATCH_SIZE = 400
        private const val MAX_REGIONS_PER_BATCH = 10
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
