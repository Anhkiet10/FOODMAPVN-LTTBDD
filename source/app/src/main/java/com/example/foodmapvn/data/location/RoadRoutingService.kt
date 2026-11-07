package com.example.foodmapvn.data.location

import com.example.foodmapvn.data.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RoadRouteResult(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

object RoadRoutingService {

    /**
     * Dán Google Maps Directions API Key của bạn vào đây (Ví dụ: "AIzaSy...").
     * Nếu để trống, dịch vụ sẽ dùng OSRM API đường bộ công cộng làm dự phòng.
     */
    const val GOOGLE_MAPS_API_KEY: String = ""

    suspend fun fetchRoute(
        start: LatLng,
        end: LatLng,
        mode: String = "driving",
    ): RoadRouteResult? = withContext(Dispatchers.IO) {
        if (GOOGLE_MAPS_API_KEY.isNotBlank()) {
            val googleResult = fetchGoogleDirections(start, end, mode, GOOGLE_MAPS_API_KEY)
            if (googleResult != null) return@withContext googleResult
        }
        fetchOsrmRoute(start, end, mode)
    }

    private fun fetchGoogleDirections(
        start: LatLng,
        end: LatLng,
        mode: String,
        apiKey: String,
    ): RoadRouteResult? {
        return runCatching {
            val travelMode = if (mode == "walking") "walking" else "driving"
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}&mode=$travelMode&key=$apiKey"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.optString("status") == "OK") {
                    val routes = root.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val routeObj = routes.getJSONObject(0)
                        val overviewPoly = routeObj.optJSONObject("overview_polyline")?.optString("points") ?: ""
                        val legs = routeObj.optJSONArray("legs")
                        var distance = 0.0
                        var duration = 0.0
                        if (legs != null && legs.length() > 0) {
                            val leg = legs.getJSONObject(0)
                            distance = leg.optJSONObject("distance")?.optDouble("value", 0.0) ?: 0.0
                            duration = leg.optJSONObject("duration")?.optDouble("value", 0.0) ?: 0.0
                        }

                        val points = decodePolyline(overviewPoly)
                        if (points.isNotEmpty()) {
                            return@runCatching RoadRouteResult(points, distance, duration)
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun fetchOsrmRoute(
        start: LatLng,
        end: LatLng,
        mode: String,
    ): RoadRouteResult? {
        val urls = if (mode == "walking") listOf(
            "https://routing.openstreetmap.de/routed-foot/route/v1/walking/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson",
            "https://router.project-osrm.org/route/v1/walking/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
        ) else listOf(
            "https://routing.openstreetmap.de/routed-car/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson",
            "https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
        )

        for (urlString in urls) {
            val result = runCatching {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                connection.setRequestProperty("Accept", "application/json")

                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val routes = root.optJSONArray("routes")
                    if (routes != null && routes.length() > 0) {
                        val routeObj = routes.getJSONObject(0)
                        val distance = routeObj.optDouble("distance", 0.0)
                        val duration = routeObj.optDouble("duration", 0.0)
                        val geometry = routeObj.optJSONObject("geometry")
                        val coords = geometry?.optJSONArray("coordinates")

                        val points = mutableListOf<LatLng>()
                        if (coords != null) {
                            for (i in 0 until coords.length()) {
                                val coordPair = coords.getJSONArray(i)
                                val lng = coordPair.getDouble(0)
                                val lat = coordPair.getDouble(1)
                                points.add(LatLng(lat, lng))
                            }
                        }
                        if (points.size >= 2) {
                            return@runCatching RoadRouteResult(points, distance, duration)
                        }
                    }
                }
                null
            }.getOrNull()

            if (result != null) return result
        }
        return null
    }

    /** Giải mã chuỗi Google Encoded Polyline thành danh sách tọa độ LatLng thực tế. */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }
}
