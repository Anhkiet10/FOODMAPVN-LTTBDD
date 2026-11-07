package com.example.foodmapvn.ui.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng as MapLatLng

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private const val MY_LOCATION_SOURCE = "my-location"
private const val ROUTE_SOURCE = "navigation-route-source"
private const val ROUTE_LAYER = "navigation-route-layer"
private const val DEST_SOURCE = "dest-location-source"
private const val MIN_ZOOM = 8.0
private const val MAX_ZOOM = 19.0

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
        onTravelModeChange = viewModel::setTravelMode,
        onClearRoute = viewModel::clearActiveRoute,
        modifier = modifier,
    )
}

@Composable
fun MapScreen(
    state: MapUiState,
    modifier: Modifier = Modifier,
    onMyLocationClick: () -> Unit = {},
    onProvinceSelected: (Province) -> Unit = {},
    onTravelModeChange: (String) -> Unit = {},
    onClearRoute: () -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

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

    LaunchedEffect(mapView) {
        mapView.addOnDidFailLoadingMapListener { loadFailed = true }
        mapView.getMapAsync { m ->
            m.setMinZoomPreference(MIN_ZOOM)
            m.setMaxZoomPreference(MAX_ZOOM)
            m.setStyle(Style.Builder().fromUri(MapStyles.DEFAULT)) { s ->
                s.addSource(GeoJsonSource(MY_LOCATION_SOURCE))
                s.addSource(GeoJsonSource(ROUTE_SOURCE))
                s.addSource(GeoJsonSource(DEST_SOURCE))

                s.addLayer(
                    LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                        lineColor("#1A73E8"),
                        lineWidth(6f),
                        lineCap(Property.LINE_CAP_ROUND),
                        lineJoin(Property.LINE_JOIN_ROUND),
                    )
                )

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

                s.addLayer(
                    CircleLayer("dest-halo", DEST_SOURCE).withProperties(
                        circleRadius(16f), circleColor("#D32F2F"), circleOpacity(0.25f),
                    )
                )
                s.addLayer(
                    CircleLayer("dest-dot", DEST_SOURCE).withProperties(
                        circleRadius(8f), circleColor("#D32F2F"),
                        circleStrokeWidth(2.5f), circleStrokeColor("#FFFFFF"),
                    )
                )

                loadFailed = false
                style = s
                map = m
            }
        }
    }

    LaunchedEffect(map, state.cameraVersion) {
        val m = map ?: return@LaunchedEffect
        val b = state.bounds
        m.setLatLngBoundsForCameraTarget(LatLngBounds.from(b.north, b.east, b.south, b.west))
        m.moveCamera(
            CameraUpdateFactory.newLatLngZoom(MapLatLng(state.center.latitude, state.center.longitude), state.zoom)
        )
    }

    LaunchedEffect(style, state.myLocation) {
        val me = state.myLocation ?: return@LaunchedEffect
        style?.getSourceAs<GeoJsonSource>(MY_LOCATION_SOURCE)
            ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(me.longitude, me.latitude)))
    }

    LaunchedEffect(style, state.activeRoute) {
        val route = state.activeRoute
        val rSource = style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)
        val dSource = style?.getSourceAs<GeoJsonSource>(DEST_SOURCE)

        if (route != null) {
            val points = route.routePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            rSource?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points)))
            dSource?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(route.destination.location.longitude, route.destination.location.latitude)))
        } else {
            rSource?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(emptyList())))
            dSource?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(0.0, 0.0)))
        }
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (state.activeRoute != null) 210.dp else 16.dp, end = 16.dp),
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

        if (state.activeRoute != null) {
            InAppNavigationCard(
                route = state.activeRoute,
                onTravelModeChange = onTravelModeChange,
                onClearRoute = onClearRoute,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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

@Composable
private fun InAppNavigationCard(
    route: ActiveNavigationRoute,
    onTravelModeChange: (String) -> Unit,
    onClearRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_my_location),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Vị trí của bạn",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_place),
                            contentDescription = null,
                            tint = BrickRed,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${route.destination.placeName} (${route.destination.address})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = onClearRoute) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Đóng")
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Sand, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val isDriving = route.mode == "driving"
                    Button(
                        onClick = { onTravelModeChange("driving") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDriving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDriving) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("🚗 Đi xe", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { onTravelModeChange("walking") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDriving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isDriving) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("🚶 Đi bộ", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Text(
                    text = route.estimatedTimeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrickRed,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        val gmmIntentUri = Uri.parse("google.navigation:q=${route.destination.location.latitude},${route.destination.location.longitude}&mode=d")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${route.destination.location.latitude},${route.destination.location.longitude}&travelmode=driving")
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text("Bắt đầu dẫn đường Google Maps", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onClearRoute,
                    modifier = Modifier.weight(0.4f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text("Đóng", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

private fun areaTitle(state: MapUiState): String = state.area.name

private fun areaSubtitle(state: MapUiState, loadFailed: Boolean): String = when {
    loadFailed -> "Không tải được bản đồ OpenFreeMap. Kiểm tra kết nối mạng."
    state.activeRoute != null -> "Đang hiển thị tuyến đường chỉ dẫn thực tế"
    state.status == LocationStatus.Locating -> "Đang xác định vị trí của bạn…"
    state.status == LocationStatus.Denied -> "Chưa có quyền vị trí · chạm để chọn tỉnh/thành khác"
    state.status == LocationStatus.Unavailable -> "Không lấy được vị trí · chạm để chọn tỉnh/thành khác"
    else -> "Khu vực đang xem · chạm để chọn tỉnh/thành khác"
}
