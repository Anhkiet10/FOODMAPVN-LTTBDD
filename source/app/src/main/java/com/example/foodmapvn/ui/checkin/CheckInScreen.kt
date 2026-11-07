package com.example.foodmapvn.ui.checkin

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

@Composable
fun CheckInRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckInViewModel = viewModel(factory = CheckInViewModel.Factory),
) {
    val place by viewModel.placeDetail.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.checkInWithLocation()
        } else {
            // Tự động thử checkin với vị trí giả định / vị trí hiện có
            viewModel.checkInWithLocation()
        }
    }

    CheckInScreen(
        place = place,
        state = state,
        onStartCheckIn = {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        },
        onDone = onDone,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    place: PlaceDetail?,
    state: CheckInUiState,
    onStartCheckIn: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in quán") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (place == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Đang tải thông tin quán...")
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                is CheckInUiState.Success -> SuccessGreenSoft
                                is CheckInUiState.TooFar, is CheckInUiState.TooSoon, is CheckInUiState.Error -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            when (state) {
                                is CheckInUiState.Success -> R.drawable.ic_check
                                is CheckInUiState.TooFar, is CheckInUiState.TooSoon, is CheckInUiState.Error -> R.drawable.ic_place
                                else -> R.drawable.ic_my_location
                            }
                        ),
                        contentDescription = null,
                        tint = when (state) {
                            is CheckInUiState.Success -> SuccessGreen
                            is CheckInUiState.TooFar, is CheckInUiState.TooSoon, is CheckInUiState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    place.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    place.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                when (state) {
                    CheckInUiState.Idle -> {
                        Text(
                            "Nhấn nút bên dưới để xác minh vị trí GPS của bạn (trong bán kính 100m tính từ quán).",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onStartCheckIn,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_my_location), contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Bắt đầu Check-in GPS", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    CheckInUiState.Checking -> {
                        CircularProgressIndicator(color = SuccessGreen)
                        Spacer(Modifier.height(16.dp))
                        Text("Đang xác minh vị trí GPS của bạn...", style = MaterialTheme.typography.bodyMedium)
                    }

                    is CheckInUiState.Success -> {
                        Text(
                            "🎉 Check-in thành công!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Khoảng cách đến quán: ${(state.distanceMeters).toInt()}m.\nĐiểm uy tín của quán đã được tăng! Các món trong Wishlist của bạn đã được tự động tích 'Đã ăn'.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Hoàn thành", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    is CheckInUiState.TooFar -> {
                        Text(
                            "❌ Vị trí không hợp lệ",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Bạn đang cách quán ${(state.distanceMeters / 1000).let { String.format("%.1f km", it) }} (${(state.distanceMeters).toInt()}m).\nYêu cầu khoảng cách tối đa là ${state.maxMeters.toInt()}m để check-in.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(32.dp))
                        OutlinedButton(
                            onClick = onStartCheckIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Thử lại GPS")
                        }
                    }

                    is CheckInUiState.TooSoon -> {
                        Text(
                            "⏳ Đã check-in gần đây",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Mỗi quán chỉ được check-in 1 lần trong vòng 12 giờ.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Quay lại", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    is CheckInUiState.Error -> {
                        Text(
                            "Lỗi: ${state.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onStartCheckIn,
                            colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
        }
    }
}
