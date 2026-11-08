package com.example.foodmapvn.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.R
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

/**
 * Màn "Giới thiệu" (Figma) – chỉ hiện lần đầu mở app (chưa chọn khu vực).
 * Trang cuối bấm "Bắt đầu" -> sheet "Cho phép FoodMap dùng vị trí?" (Figma "Xin quyền vị trí").
 *
 * @param onFinished askLocation = true: đã cấp quyền vị trí -> màn Chọn vùng tự nhận tỉnh theo GPS;
 *                   false: "Để sau" hoặc từ chối -> Chọn vùng không hỏi lại (vẫn có nút "Dùng vị trí hiện tại").
 */
@Composable
fun OnboardingScreen(onFinished: (askLocation: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val pages = remember { onboardingPages() }
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var showPermission by remember { mutableStateOf(false) }
    // "Cho phép khi dùng app" -> hiện hộp xin quyền của Android ngay tại đây (như Figma), rồi đi tiếp
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted -> onFinished(granted.values.any { it }) }
    val isLast = pager.currentPage == pages.lastIndex

    Box(modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showPermission = true }) {
                    Text("Bỏ qua", color = InkSecondary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                OnboardingPageContent(pages[index])
            }
            PageDots(count = pages.size, selected = pager.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    if (isLast) showPermission = true
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
            ) {
                Text(if (isLast) "Bắt đầu" else "Tiếp tục", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showPermission) {
            LocationPermissionSheet(
                onAllow = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                },
                onLater = { onFinished(false) },
            )
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit,
)

private fun onboardingPages() = listOf(
    OnboardingPage(
        title = "Tra theo món,\nkhông phải theo quán",
        body = "Chọn đặc sản bạn muốn ăn, app sẽ chỉ ra những quán được người địa phương xác thực.",
        illustration = { IllustrationImage(R.drawable.onb_dish) },
    ),
    // Figma chưa có trang 2 – tự thiết kế theo cùng phong cách (đã ghi vào báo cáo khác biệt)
    OnboardingPage(
        title = "Biết giá trước\nkhi ngồi xuống",
        body = "Giá được người ăn báo lại sau mỗi lần check-in. Quán bị báo giá cao sẽ có cảnh báo rõ ràng.",
        illustration = { PriceIllustration() },
    ),
    OnboardingPage(
        title = "Đóng góp để được tin tưởng",
        body = "Check-in đúng tại quán, chấm điểm và báo giá thật.\n5 lần check-in ở một vùng = huy hiệu Người địa phương.",
        illustration = { IllustrationImage(R.drawable.onb_map, rounded = true) },
    ),
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { page.illustration() }
        Spacer(Modifier.height(36.dp))
        Text(
            page.title,
            color = Ink,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            page.body,
            color = InkSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IllustrationImage(res: Int, rounded: Boolean = false) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (rounded) Modifier.clip(RoundedCornerShape(20.dp)) else Modifier),
    )
}

/** Minh hoạ trang 2: thẻ quán với giá được báo và cảnh báo giá (vẽ bằng Compose). */
@Composable
private fun PriceIllustration() {
    Box(
        Modifier
            .size(260.dp)
            .clip(CircleShape)
            .background(Sand),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(210.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Bún bò Huế", fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
            Text("35.000 – 50.000đ / tô", fontWeight = FontWeight.SemiBold, color = BrickRed, fontSize = 15.sp)
            Pill("✓ 46 người báo giá", SuccessGreen)
            Pill("⚠ 3 báo giá cao", Amber)
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == selected
            Box(
                Modifier
                    .animateContentSize()
                    .height(8.dp)
                    .width(if (active) 22.dp else 8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) BrickRed else Color(0xFFD4CCC4))
            )
        }
    }
}

/** Figma "Xin quyền vị trí": hộp trắng bo góc ở đáy, nền mờ phía sau. */
@Composable
private fun LocationPermissionSheet(onAllow: () -> Unit, onLater: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.ic_place),
                contentDescription = null,
                tint = BrickRed,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text("Cho phép FoodMap dùng vị trí?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Để tìm quán gần bạn và xác minh check-in.\nChỉ dùng khi bạn đang mở app.",
                color = InkSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAllow,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) { Text("Cho phép khi dùng app", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            TextButton(onClick = onLater) {
                Text("Để sau", color = InkSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun OnboardingPreview() {
    FoodmapvnTheme { OnboardingScreen(onFinished = {}) }
}
