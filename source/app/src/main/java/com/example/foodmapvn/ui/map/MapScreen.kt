package com.example.foodmapvn.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Province
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng as MapLatLng

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private const val MY_LOCATION_SOURCE = "my-location"
private const val MIN_ZOOM = 9.0 // không cho thu nhỏ ra cả nước -> không tải bản đồ vùng khác
private const val MAX_ZOOM = 19.0

/** Nối MapViewModel với MapScreen. Bản đồ mở theo khu vực đang chọn của cả app. */
@Composable
fun MapRoute(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.goToMyLocation() else viewModel.onPermissionDenied()
    }

    // Có quyền thì vẽ chấm vị trí của tôi (không đổi khu vực đang chọn)
    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) viewModel.showMyLocationDot()
    }

    MapScreen(
        state = state,
        onMyLocationClick = {
            if (viewModel.hasLocationPermission()) viewModel.goToMyLocation()
            else permissionLauncher.launch(LOCATION_PERMISSIONS)
        },
        onProvinceSelected = viewModel::selectProvince,
        modifier = modifier,
    )
}

/**
 * Bản đồ vector OpenFreeMap (MapLibre) theo khu vực: quanh vị trí hiện tại,
 * hoặc tỉnh/thành chọn trên thanh tìm kiếm. Chưa hiển thị quán.
 */
@Composable
fun MapScreen(
    state: MapUiState,
    modifier: Modifier = Modifier,
    onMyLocationClick: () -> Unit = {},
    onProvinceSelected: (Province) -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    // MapView cần được báo đủ vòng đời: start/resume/pause/stop/destroy
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val current = lifecycleOwner.lifecycle.currentState
            if (current.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (current.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Tải kiểu bản đồ + lớp chấm xanh "vị trí của tôi"
    LaunchedEffect(mapView) {
        mapView.addOnDidFailLoadingMapListener { loadFailed = true }
        mapView.getMapAsync { m ->
            m.setMinZoomPreference(MIN_ZOOM)
            m.setMaxZoomPreference(MAX_ZOOM)
            m.setStyle(Style.Builder().fromUri(MapStyles.DEFAULT)) { s ->
                s.addSource(GeoJsonSource(MY_LOCATION_SOURCE))
                s.addLayer(
                    CircleLayer("my-location-halo", MY_LOCATION_SOURCE).withProperties(
                        circleRadius(16f), circleColor("#1A73E8"), circleOpacity(0.18f),
                    )
                )
                s.addLayer(
                    CircleLayer("my-location-dot", MY_LOCATION_SOURCE).withProperties(
                        circleRadius(7f), circleColor("#1A73E8"),
                        circleStrokeWidth(2.5f), circleStrokeColor("#FFFFFF"),
                    )
                )
                loadFailed = false
                style = s
                map = m
            }
        }
    }

    // Khóa bản đồ trong khu vực và đưa camera tới khu vực mới (khi đổi vị trí hoặc chọn tỉnh)
    LaunchedEffect(map, state.cameraVersion) {
        val m = map ?: return@LaunchedEffect
        val b = state.bounds
        m.setLatLngBoundsForCameraTarget(LatLngBounds.from(b.north, b.east, b.south, b.west))
        m.moveCamera(
            CameraUpdateFactory.newLatLngZoom(MapLatLng(state.center.latitude, state.center.longitude), state.zoom)
        )
    }

    // Cập nhật chấm vị trí của tôi
    LaunchedEffect(style, state.myLocation) {
        val me = state.myLocation ?: return@LaunchedEffect
        style?.getSourceAs<GeoJsonSource>(MY_LOCATION_SOURCE)
            ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(me.longitude, me.latitude)))
    }

    Box(modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        AreaSearchBar(
            title = areaTitle(state),
            subtitle = areaSubtitle(state, loadFailed),
            onClick = { showPicker = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
        )

        // Nút phóng to / thu nhỏ (tiện khi dùng máy ảo) + nút vị trí của tôi
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            SmallFloatingActionButton(
                onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn()) },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Phóng to")
            }
            SmallFloatingActionButton(
                onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut()) },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Icon(painterResource(R.drawable.ic_remove), contentDescription = "Thu nhỏ")
            }
            FloatingActionButton(
                onClick = onMyLocationClick,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Icon(painterResource(R.drawable.ic_my_location), contentDescription = "Vị trí của tôi")
            }
        }
    }

    if (showPicker) {
        ProvincePickerSheet(
            provinces = state.provinces,
            selectedId = state.area.id,
            onMyLocation = {
                showPicker = false
                onMyLocationClick()
            },
            onSelect = {
                showPicker = false
                onProvinceSelected(it)
            },
            onDismiss = { showPicker = false },
        )
    }
}

private fun areaTitle(state: MapUiState): String = state.area.name

private fun areaSubtitle(state: MapUiState, loadFailed: Boolean): String = when {
    loadFailed -> "Không tải được bản đồ OpenFreeMap. Kiểm tra kết nối mạng."
    state.status == LocationStatus.Locating -> "Đang xác định vị trí của bạn…"
    state.status == LocationStatus.Denied -> "Chưa có quyền vị trí · chạm để chọn tỉnh/thành khác"
    state.status == LocationStatus.Unavailable -> "Không lấy được vị trí · chạm để chọn tỉnh/thành khác"
    else -> "Khu vực đang xem · chạm để chọn tỉnh/thành khác"
}
