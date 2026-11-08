package com.example.foodmapvn.ui.region

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.SandDark
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
    askLocationOnStart: Boolean = true,
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

    // Lần đầu vào app: hỏi quyền vị trí (trừ khi ở màn Giới thiệu người dùng bấm "Để sau")
    LaunchedEffect(Unit) {
        if (state.isOnboarding && askLocationOnStart) requestLocation()
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
        // Figma "Chọn vùng": tiêu đề lớn + nút đóng tròn (chỉ khi mở lại từ Khám phá/Bản đồ)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        ) {
            Text(
                "Bạn muốn khám phá ở đâu?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            if (!state.isOnboarding) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SandDark)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = "Đóng", tint = Ink, modifier = Modifier.size(22.dp))
                }
            }
        }

        StatusBox(state.status)

        ProvinceList(
            provinces = state.provinces,
            selectedId = state.selectedId,
            onSelect = onSelect,
            myLocationSubtitle = "Tự nhận vùng bạn đang đứng",
            onMyLocation = onUseMyLocation,
            summaries = state.regionSummaries,
            showFeatured = true,
            footer = {
                // Figma có link "Đề xuất vùng mới" – app hiện đã có đủ 34 tỉnh nên chỉ báo nhẹ
                val context = LocalContext.current
                Text(
                    buildAnnotatedString {
                        append("Chưa có vùng bạn cần? ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Đề xuất vùng mới") }
                    },
                    color = BrickRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(context, "Tính năng đề xuất vùng đang được phát triển", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                )
            },
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
