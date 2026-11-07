package com.example.foodmapvn.ui.suggestion

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Dish
import com.example.foodmapvn.data.model.PlacePin
import com.example.foodmapvn.ui.map.MapStyles
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Sand
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Nhật – UC-14: Đề xuất quán 2 bước. */
@Composable
fun SuggestionNewRoute(
    onBack: () -> Unit,
    onFinished: () -> Unit = onBack,
    vm: SuggestionViewModel = viewModel(factory = SuggestionViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        vm.clearMessage()
        if (msg.startsWith("Đã gửi")) onFinished()
    }

    when (state.step) {
        SuggestionStep.MAP -> Step1Map(
            lat = state.lat,
            lng = state.lng,
            nearby = state.nearbyDuplicates,
            checking = state.checkingNearby,
            onPinMoved = vm::onPinMoved,
            onContinue = vm::goToForm,
            onBack = onBack,
            snackbar = snackbar,
        )
        SuggestionStep.FORM -> Step2Form(
            state = state,
            onName = vm::setName,
            onAddress = vm::setAddress,
            onDish = vm::setDishId,
            onPrice = vm::setPriceText,
            onHours = vm::setOpenHours,
            onPhoto = vm::setPhoto,
            onSubmit = vm::submit,
            onBack = vm::backToMap,
            snackbar = snackbar,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Map(
    lat: Double,
    lng: Double,
    nearby: List<PlacePin>,
    checking: Boolean,
    onPinMoved: (Double, Double) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đề xuất quán · Bước 1") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Sand),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                when {
                    nearby.isNotEmpty() -> Text(
                        "⚠ Có thể trùng: ${nearby.joinToString { it.name }} (≤ 15 m)",
                        color = Amber,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    checking -> Text("Đang kiểm tra quán gần…", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Tọa độ: ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                ) { Text("Tiếp tục") }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            map.setStyle(Style.Builder().fromUri(MapStyles.DEFAULT)) {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(MapLatLng(lat, lng), 16.0),
                                )
                                map.addOnCameraIdleListener {
                                    val c = map.cameraPosition.target ?: return@addOnCameraIdleListener
                                    onPinMoved(c.latitude, c.longitude)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Icon(
                painterResource(R.drawable.ic_place),
                contentDescription = "Ghim",
                tint = BrickRed,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .padding(bottom = 20.dp),
            )
            Text(
                "Kéo bản đồ để đặt ghim",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun Step2Form(
    state: SuggestionUiState,
    onName: (String) -> Unit,
    onAddress: (String) -> Unit,
    onDish: (String) -> Unit,
    onPrice: (String) -> Unit,
    onHours: (String) -> Unit,
    onPhoto: (Uri?) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onPhoto(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đề xuất quán · Bước 2") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Sand),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Vùng: ${state.region?.name ?: "—"} · ${"%.5f".format(state.lat)}, ${"%.5f".format(state.lng)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(state.name, onName, label = { Text("Tên quán *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.address, onAddress, label = { Text("Địa chỉ *") }, modifier = Modifier.fillMaxWidth())
            Text("Món chính *", fontWeight = FontWeight.SemiBold)
            if (state.dishes.isEmpty()) {
                Text("Vùng này chưa có món — chọn Huế ở Khám phá.", color = Amber, style = MaterialTheme.typography.bodySmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.dishes.forEach { d: Dish ->
                        FilterChip(
                            selected = d.id == state.selectedDishId,
                            onClick = { onDish(d.id) },
                            label = { Text(d.name) },
                        )
                    }
                }
            }
            OutlinedTextField(state.priceText, onPrice, label = { Text("Giá (đồng)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.openHours, onHours, label = { Text("Giờ mở (06:00–14:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Text("Ảnh quán", fontWeight = FontWeight.SemiBold)
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { pick.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.uploading -> CircularProgressIndicator(Modifier.size(28.dp))
                    state.photoUri != null -> AsyncImage(
                        state.photoUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    )
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_add), null)
                        Text("Thêm", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Button(
                onClick = onSubmit,
                enabled = !state.submitting && !state.uploading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
            ) {
                if (state.submitting || state.uploading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.uploading) "Đang tải ảnh…" else "Đang gửi…")
                } else Text("Gửi đề xuất")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại bản đồ")
            }
        }
    }
}