package com.example.foodmapvn.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.fake.FakeData
import com.example.foodmapvn.data.model.BadgeStatus
import com.example.foodmapvn.data.model.RegionBadge
import com.example.foodmapvn.data.model.ReviewSummary
import com.example.foodmapvn.data.model.UserProfile
import com.example.foodmapvn.ui.components.ProgressBar
import com.example.foodmapvn.ui.components.StarRating
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

/** Nối ProfileViewModel với ProfileScreen. */
@Composable
fun ProfileRoute(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        ProfileScreenState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        ProfileScreenState.Guest -> GuestProfile(onLoginClick = onLoginClick, modifier = modifier)
        is ProfileScreenState.Content -> ProfileScreen(
            state = current.data,
            onSignOutClick = viewModel::signOut,
            modifier = modifier,
        )
    }
}

/** Màn Hồ sơ khi chưa đăng nhập. */
@Composable
fun GuestProfile(onLoginClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Bạn đang dùng thử", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Đăng nhập để lưu món muốn ăn, check-in, viết đánh giá và nhận huy hiệu Người địa phương.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLoginClick,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Đăng nhập / Đăng ký", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Chỉ dùng cho @Preview. */
private fun fakeProfileState() = ProfileUiState(
    user = FakeData.user,
    hasUnreadNotification = true,
    regionBadges = FakeData.regionBadges,
    recentReviews = FakeData.recentReviews,
    wishlistCount = FakeData.savedDishes.size,
    mySuggestionCount = FakeData.user.suggestedPlaceCount,
)

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSeeAllReviewsClick: () -> Unit = {},
    onReviewClick: (ReviewSummary) -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onMySuggestionsClick: () -> Unit = {},
    onOfflineDataClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        ProfileTopBar(state.hasUnreadNotification, onNotificationsClick, onSettingsClick)
        Spacer(Modifier.height(12.dp))
        UserHeader(state.user)
        Spacer(Modifier.height(16.dp))
        StatsRow(state.user)

        Spacer(Modifier.height(24.dp))
        SectionTitle("Huy hiệu theo vùng")
        Spacer(Modifier.height(4.dp))
        state.regionBadges.forEach { RegionBadgeRow(it) }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Đánh giá gần đây", Modifier.weight(1f))
            Text(
                "Xem tất cả",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAllReviewsClick)
                    .padding(4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.recentReviews.forEach { review ->
                ReviewCard(review, onClick = { onReviewClick(review) })
            }
        }

        Spacer(Modifier.height(12.dp))
        MenuRow("Món muốn ăn", state.wishlistCount, onWishlistClick)
        MenuDivider()
        MenuRow("Đề xuất quán của tôi", state.mySuggestionCount, onMySuggestionsClick)
        MenuDivider()
        MenuRow("Tải dữ liệu vùng để dùng offline", null, onOfflineDataClick)
        MenuDivider()
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

@Composable
private fun ProfileTopBar(
    hasUnread: Boolean,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Hồ sơ", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onNotificationsClick) {
            Box {
                Icon(painterResource(R.drawable.ic_notifications), contentDescription = "Thông báo")
                if (hasUnread) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(painterResource(R.drawable.ic_settings), contentDescription = "Cài đặt")
        }
    }
}

@Composable
private fun UserHeader(user: UserProfile) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                user.avatarInitial,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(user.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "Thành viên từ ${user.memberSince}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            user.localBadge?.let { badge ->
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SuccessGreenSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(badge, style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(user: UserProfile) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(user.checkInCount, "Check-in", Modifier.weight(1f))
        StatCard(user.reviewCount, "Đánh giá", Modifier.weight(1f))
        StatCard(user.suggestedPlaceCount, "Quán đề xuất", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Sand)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

@Composable
private fun RegionBadgeRow(badge: RegionBadge) {
    val iconTint: Color
    val accent: Color
    val barColor: Color
    when (badge.status) {
        BadgeStatus.ACHIEVED -> { iconTint = SuccessGreen; accent = SuccessGreen; barColor = SuccessGreen }
        BadgeStatus.IN_PROGRESS -> { iconTint = SandDark; accent = Amber; barColor = Amber }
        BadgeStatus.LOCKED -> { iconTint = SandDark; accent = InkTertiary; barColor = SandDark }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(
                if (badge.status == BadgeStatus.ACHIEVED) R.drawable.ic_shield_check else R.drawable.ic_shield
            ),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(badge.region, style = MaterialTheme.typography.titleMedium)
            Text(badge.description, style = MaterialTheme.typography.bodySmall, color = accent)
        }
        ProgressBar(
            progress = badge.progress,
            color = barColor,
            trackColor = Sand,
            modifier = Modifier
                .width(110.dp)
                .height(6.dp),
        )
    }
}

@Composable
private fun ReviewCard(review: ReviewSummary, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            review.placeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StarRating(review.rating)
        Spacer(Modifier.width(10.dp))
        if (review.isCheckedIn) {
            Icon(
                painterResource(R.drawable.ic_verified),
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text("Check-in", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
            Spacer(Modifier.width(10.dp))
        }
        Text(review.price, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MenuRow(label: String, count: Int?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (count != null) {
            Text("$count", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
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
private fun MenuDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(showBackground = true, widthDp = 375, heightDp = 900)
@Composable
private fun ProfileScreenPreview() {
    FoodmapvnTheme { ProfileScreen(state = fakeProfileState()) }
}
