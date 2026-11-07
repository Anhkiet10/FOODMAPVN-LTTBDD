package com.example.foodmapvn.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.R
import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.di.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Màn Cài đặt (route settings): mở từ nút bánh răng ở Hồ sơ. */
class SettingsViewModel(
    areaRepository: AreaRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val areaName: StateFlow<String?> = areaRepository.selectedArea
        .map { it?.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), areaRepository.selectedArea.value?.name)

    val isLoggedIn: StateFlow<Boolean> = authRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun signOut() = authRepository.signOut()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                SettingsViewModel(c.areaRepository, c.authRepository)
            }
        }
    }
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onRegionClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val areaName by viewModel.areaName.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val version = remember { appVersion(context) }
    SettingsScreen(
        areaName = areaName,
        isLoggedIn = isLoggedIn,
        version = version,
        onBack = onBack,
        onRegionClick = onRegionClick,
        onLocationPermissionClick = { openAppSettings(context) },
        onSignOutClick = {
            viewModel.signOut()
            onBack()
        },
    )
}

@Composable
fun SettingsScreen(
    areaName: String?,
    isLoggedIn: Boolean,
    version: String,
    onBack: () -> Unit,
    onRegionClick: () -> Unit,
    onLocationPermissionClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
            }
            Text("Cài đặt", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Section("Chung")
            SettingRow("Khu vực đang chọn", areaName ?: "Chưa chọn", onRegionClick)
            Divider()
            SettingRow("Quyền vị trí", "Mở cài đặt của app để bật/tắt", onLocationPermissionClick)
            Divider()

            Section("Giới thiệu")
            InfoRow("Phiên bản", version)
            Divider()
            InfoRow("Bản đồ", "© OpenStreetMap contributors · OpenFreeMap · MapLibre")
            Divider()
            InfoRow("Biểu tượng", "Material Icons (Google, giấy phép Apache 2.0)")
            Divider()

            if (isLoggedIn) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Đăng xuất",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOutClick)
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "1.0"

/** Mở trang "Thông tin ứng dụng" của Android, nơi bật/tắt quyền vị trí. */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
