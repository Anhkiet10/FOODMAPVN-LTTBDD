package com.example.foodmapvn.ui.map

import android.content.Context
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.foodmapvn.data.model.FoodCategory
import com.example.foodmapvn.data.model.PlaceSummary
import com.example.foodmapvn.ui.components.iconRes
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.circleTranslate
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import android.graphics.Color as AndroidColor

/**
 * Ghim quán trên MapLibre (UC-07), vẽ bằng lớp (layer) nên vài trăm quán vẫn mượt:
 * - Thu nhỏ xa (zoom < 12): chấm nhỏ màu theo uy tín, cho đỡ rối.
 * - Gần (zoom ≥ 12): vòng tròn trắng, **viền màu = điểm uy tín**, **icon ở giữa = loại món**;
 *   quán bị cảnh báo giá có **chấm đỏ** ở góc trên phải.
 * - Gần hơn (zoom ≥ 14): hiện thêm tên quán.
 */
object PlaceLayers {
    private const val SOURCE = "places"
    private const val SELECTED_SOURCE = "place-selected"
    private const val DOT_LAYER = "places-dot"
    private const val RING_LAYER = "places-ring"
    private const val ICON_LAYER = "places-icon"
    private const val WARNING_LAYER = "places-warning"
    private const val LABEL_LAYER = "places-label"
    private const val SELECTED_LAYER = "place-selected-halo"
    /** Màu vòng quán đang chọn (xanh biển). */
    const val COLOR_SELECTED = "#1E88E5"
    private val CLICKABLE = arrayOf(DOT_LAYER, RING_LAYER, ICON_LAYER)

    // Cùng bảng màu với giao diện (ui/theme/Color.kt)
    const val COLOR_HIGH = "#2E7D4F"     // uy tín ≥ 80
    const val COLOR_MEDIUM = "#C4862C"   // 60–79
    const val COLOR_LOW = "#8A8580"      // < 60
    const val COLOR_NONE = "#D6D1CB"     // chưa có đánh giá nào
    const val COLOR_WARNING = "#D32F2F"  // chấm cảnh báo giá
    private const val COLOR_ICON = "#3A2A20"

    private const val ICON_ZOOM = 12f
    private const val LABEL_ZOOM = 14f

    private const val P_ID = "id"
    private const val P_NAME = "name"
    private const val P_TRUST = "trust"
    private const val P_RATED = "rated"
    private const val P_WARNING = "warning"
    private const val P_ICON = "icon"

    private fun iconName(category: FoodCategory) = "cat_" + category.value

    /** Chưa có đánh giá -> xám nhạt; có rồi -> màu theo điểm uy tín. */
    private fun trustColor(): Expression = Expression.switchCase(
        Expression.get(P_RATED),
        Expression.step(
            Expression.get(P_TRUST), Expression.color(AndroidColor.parseColor(COLOR_LOW)),
            Expression.stop(TrustLevel.MEDIUM.min, Expression.color(AndroidColor.parseColor(COLOR_MEDIUM))),
            Expression.stop(TrustLevel.HIGH.min, Expression.color(AndroidColor.parseColor(COLOR_HIGH))),
        ),
        Expression.color(AndroidColor.parseColor(COLOR_NONE)),
    )

    /** Thêm icon + nguồn + lớp vào style (gọi 1 lần khi style tải xong, trước lớp "vị trí của tôi"). */
    fun addTo(style: Style, context: Context) {
        // Icon loại món: vector -> bitmap 18dp màu nâu đậm, đăng ký vào style theo tên "cat_<loại>"
        val px = (18 * context.resources.displayMetrics.density).toInt()
        FoodCategory.entries.forEach { category ->
            val drawable = ContextCompat.getDrawable(context, category.iconRes)?.mutate() ?: return@forEach
            DrawableCompat.setTint(drawable, AndroidColor.parseColor(COLOR_ICON))
            style.addImage(iconName(category), drawable.toBitmap(px, px))
        }

        style.addSource(GeoJsonSource(SOURCE))
        style.addSource(GeoJsonSource(SELECTED_SOURCE))

        // Vòng xanh biển quanh quán đang chọn (dễ thấy trên nền bản đồ, không trùng màu uy tín)
        style.addLayer(
            CircleLayer(SELECTED_LAYER, SELECTED_SOURCE).withProperties(
                circleRadius(26f),
                circleColor(COLOR_SELECTED),
                circleOpacity(0.22f),
                circleStrokeWidth(3f),
                circleStrokeColor(COLOR_SELECTED),
                circleStrokeOpacity(1f),
            )
        )
        // Xa: chấm nhỏ
        style.addLayer(
            CircleLayer(DOT_LAYER, SOURCE).withProperties(
                circleRadius(6f),
                circleColor(trustColor()),
                circleStrokeWidth(1.5f),
                circleStrokeColor(AndroidColor.WHITE),
            ).maxZoomOf(ICON_ZOOM)
        )
        // Gần: vòng uy tín + nền trắng
        style.addLayer(
            CircleLayer(RING_LAYER, SOURCE).withProperties(
                circleRadius(15f),
                circleColor(AndroidColor.WHITE),
                circleStrokeWidth(3.5f),
                circleStrokeColor(trustColor()),
            ).minZoomOf(ICON_ZOOM)
        )
        // Icon loại món ở giữa
        style.addLayer(
            SymbolLayer(ICON_LAYER, SOURCE).withProperties(
                iconImage(Expression.get(P_ICON)),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
            ).minZoomOf(ICON_ZOOM)
        )
        // Chấm đỏ cảnh báo giá (góc trên phải)
        style.addLayer(
            CircleLayer(WARNING_LAYER, SOURCE).withProperties(
                circleRadius(5f),
                circleColor(AndroidColor.parseColor(COLOR_WARNING)),
                circleStrokeWidth(1.5f),
                circleStrokeColor(AndroidColor.WHITE),
                circleTranslate(arrayOf(11f, -11f)),
            ).apply { setFilter(Expression.eq(Expression.get(P_WARNING), Expression.literal(true))) }
                .minZoomOf(ICON_ZOOM)
        )
        // Tên quán dưới ghim, tự ẩn nếu chồng chữ
        style.addLayer(
            SymbolLayer(LABEL_LAYER, SOURCE).withProperties(
                textField(Expression.get(P_NAME)),
                textFont(arrayOf("Noto Sans Regular")),
                textSize(11f),
                textAnchor(Property.TEXT_ANCHOR_TOP),
                textOffset(arrayOf(0f, 1.7f)),
                textColor("#1E1E1E"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(1.5f),
                textOptional(true),
            ).minZoomOf(LABEL_ZOOM)
        )
    }

    fun setPlaces(style: Style, places: List<PlaceSummary>) {
        val features = places.map { p ->
            Feature.fromGeometry(Point.fromLngLat(p.location.longitude, p.location.latitude)).apply {
                addStringProperty(P_ID, p.id)
                addStringProperty(P_NAME, p.name)
                addNumberProperty(P_TRUST, p.trustScore)
                addBooleanProperty(P_RATED, p.hasReviews)
                addBooleanProperty(P_WARNING, p.priceWarning)
                addStringProperty(P_ICON, iconName(p.category))
            }
        }
        style.getSourceAs<GeoJsonSource>(SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    fun setSelected(style: Style, place: PlaceSummary?) {
        val features = listOfNotNull(
            place?.let { Feature.fromGeometry(Point.fromLngLat(it.location.longitude, it.location.latitude)) }
        )
        style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Ghim nào nằm gần điểm vừa bấm (nới 24 px để dễ bấm trúng); null = bấm vào chỗ trống. */
    fun placeIdAt(map: MapLibreMap, point: LatLng): String? {
        val screen = map.projection.toScreenLocation(point)
        val box = RectF(screen.x - 24, screen.y - 24, screen.x + 24, screen.y + 24)
        return map.queryRenderedFeatures(box, *CLICKABLE).firstOrNull()?.getStringProperty(P_ID)
    }

    private fun <T : Layer> T.minZoomOf(zoom: Float): T = apply { minZoom = zoom }
    private fun <T : Layer> T.maxZoomOf(zoom: Float): T = apply { maxZoom = zoom }
}
