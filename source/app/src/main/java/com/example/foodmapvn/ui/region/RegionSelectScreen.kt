package com.example.foodmapvn.ui.region

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Nối RegionSelectViewModel với màn. Lần đầu mở app: tự xin quyền vị trí ngay. */
@Composable
fun RegionSelectRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegionSelectViewModel = viewModel(factory = RegionSelectViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.useMyLocation() else viewModel.onPermissionDenied()
    }
    val requestLocation = {
        if (viewModel.hasLocationPermission()) viewModel.useMyLocation()
        else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    // Lần đầu vào app: bắt buộc hỏi quyền vị trí
    LaunchedEffect(Unit) {
        if (state.isOnboarding) requestLocation()
    }
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    RegionSelectScreen(
        state = state,
        onUseMyLocation = requestLocation,
        onSelect = viewModel::select,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun RegionSelectScreen(
    state: RegionSelectUiState,
    modifier: Modifier = Modifier,
    onUseMyLocation: () -> Unit = {},
    onSelect: (com.example.foodmapvn.data.model.Province) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp)) {
            if (!state.isOnboarding) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Text(
                if (state.isOnboarding) "Bạn đang ở đâu?" else "Chọn khu vực",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            if (state.isOnboarding) "Food Map VN cần vị trí để gợi ý đặc sản và quán ngon ở tỉnh/thành bạn đang đứng."
            else "Khu vực này dùng cho Khám phá, Bản đồ và Tìm kiếm.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        StatusBox(state.status)

        ProvinceList(
            provinces = state.provinces,
            selectedId = state.selectedId,
            onSelect = onSelect,
            myLocationSubtitle = "Tự nhận tỉnh/thành theo vị trí hiện tại",
            onMyLocation = onUseMyLocation,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusBox(status: LocateStatus) {
    val text = when (status) {
        LocateStatus.Idle -> return
        LocateStatus.Locating -> "Đang xác định vị trí của bạn…"
        LocateStatus.Denied -> "Bạn chưa cho phép truy cập vị trí. Hãy chọn tỉnh/thành bên dưới, hoặc bấm \"Vị trí của tôi\" để cấp quyền lại."
        LocateStatus.Failed -> "Không lấy được vị trí (kiểm tra GPS / vị trí của máy ảo). Hãy chọn tỉnh/thành bên dưới."
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            if (status == LocateStatus.Locating) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionSelectPreview() {
    FoodmapvnTheme {
        RegionSelectScreen(RegionSelectUiState(isOnboarding = true, status = LocateStatus.Denied))
    }
}
