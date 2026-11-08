package com.example.foodmapvn.ui.place

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.PlaceDetail
import com.example.foodmapvn.data.model.ReviewItem
import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.domain.formatVnd
import com.example.foodmapvn.ui.auth.LocalLoginGate
import com.example.foodmapvn.ui.components.StarRating
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.AvatarSoft
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

/** Danh sách đánh giá của quán (Figma "Danh sách đánh giá") + báo cáo đánh giá (Figma "Báo cáo vi phạm"). */
@Composable
fun PlaceReviewsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceReviewsViewModel = viewModel(factory = PlaceReviewsViewModel.Factory),
) {
    val place by viewModel.placeDetail.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isReporting by viewModel.isReporting.collectAsStateWithLifecycle()
    val loginGate = LocalLoginGate.current
    var reportTarget by remember { mutableStateOf<ReviewItem?>(null) }

    PlaceReviewsScreen(
        place = place,
        reviews = reviews,
        filter = filter,
        message = message,
        onFilterChange = viewModel::setFilter,
        onBack = onBack,
        // Báo cáo cần tài khoản: khách được đưa sang Đăng nhập
        onReport = { review -> loginGate.requireLogin { reportTarget = review } },
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )

    reportTarget?.let { review ->
        ReportReviewSheet(
            review = review,
            placeName = place?.name.orEmpty(),
            isSending = isReporting,
            onSend = { reason, note -> viewModel.reportReview(review, reason, note) { reportTarget = null } },
            onDismiss = { reportTarget = null },
        )
    }
}

@Composable
fun PlaceReviewsScreen(
    place: PlaceDetail?,
    reviews: List<ReviewItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    filter: ReviewFilter = ReviewFilter.ALL,
    message: String? = null,
    onFilterChange: (ReviewFilter) -> Unit = {},
    onReport: (ReviewItem) -> Unit = {},
    onMessageShown: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }
    val shown = remember(reviews, filter) { reviews.filter(filter::matches) }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { Header(place?.name, onBack) }
            item { Summary(reviews) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ReviewFilter.entries) { f ->
                        val selected = f == filter
                        Text(
                            f.label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Ink,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) BrickRed else Color.White)
                                .then(if (selected) Modifier else Modifier.border(1.dp, FieldBorder, RoundedCornerShape(50)))
                                .clickable { onFilterChange(f) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (reviews.isEmpty()) "Chưa có đánh giá nào cho quán. Hãy check-in và là người đầu tiên đánh giá!"
                        else "Không có đánh giá nào khớp bộ lọc \"${filter.label}\".",
                        fontSize = 14.sp,
                        color = InkSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { review ->
                ReviewListCard(review, onReport = { onReport(review) })
            }
        }
    }
}

@Composable
private fun Header(placeName: String?, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SandDark)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại", tint = Ink, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Đánh giá", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (!placeName.isNullOrBlank()) Text(placeName, fontSize = 13.sp, color = InkSecondary, maxLines = 1)
        }
    }
}

/** Điểm trung bình + biểu đồ số sao + hộp "N đánh giá có check-in · M từ người địa phương". */
@Composable
private fun Summary(reviews: List<ReviewItem>) {
    val count = reviews.size
    val avg = if (count == 0) 0.0 else reviews.sumOf { it.rating }.toDouble() / count
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
                Text(
                    if (count == 0) "–" else String.format(java.util.Locale.US, "%.1f", avg),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                StarRating(rating = Math.round(avg).toInt(), starSize = 16.dp)
                Spacer(Modifier.height(4.dp))
                Text("$count đánh giá", fontSize = 13.sp, color = InkSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (5 downTo 1).forEach { star ->
                    val n = reviews.count { it.rating == star }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$star", fontSize = 12.sp, color = InkSecondary, modifier = Modifier.width(14.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SandDark)
                        ) {
                            if (n > 0) Box(
                                Modifier
                                    .fillMaxWidth(n.toFloat() / count)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Amber)
                            )
                        }
                    }
                }
            }
        }
        if (count > 0) {
            val checked = reviews.count { it.isCheckedIn }
            val local = reviews.count { it.isLocal }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SuccessGreenSoft)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Icon(painterResource(R.drawable.ic_verified), contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "$checked đánh giá có check-in · $local từ người địa phương",
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun ReviewListCard(review: ReviewItem, onReport: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AvatarSoft),
                contentAlignment = Alignment.Center,
            ) {
                if (!review.userAvatar.isNullOrBlank()) {
                    AsyncImage(review.userAvatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp))
                } else {
                    Text(review.userName.take(1).uppercase(), color = BrickRed, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(review.userName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink, maxLines = 1)
                StarRating(rating = review.rating, starSize = 14.dp)
            }
            when {
                review.isLocal -> Badge("✓ Người địa phương")
                review.isCheckedIn -> Badge("✓ Đã check-in")
            }
        }
        if (!review.content.isNullOrBlank()) {
            Text(review.content, fontSize = 15.sp, color = Ink)
        }
        if (review.photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(review.photos) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Ảnh đánh giá",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull(review.createdAt, review.price?.let { "Giá: ${formatVnd(it)}đ" }).joinToString(" · "),
                fontSize = 13.sp,
                color = InkSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(R.drawable.ic_flag),
                contentDescription = "Báo cáo đánh giá",
                tint = InkTertiary,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onReport)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        color = SuccessGreen,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SuccessGreenSoft)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Figma "Báo cáo vi phạm": bảng chọn lý do + ghi chú + Hủy / Gửi báo cáo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportReviewSheet(
    review: ReviewItem,
    placeName: String,
    isSending: Boolean,
    onSend: (ReportReason, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf(ReportReason.FAKE) }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Báo cáo đánh giá", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                "Đánh giá của ${review.userName}" + if (placeName.isNotBlank()) " tại $placeName" else "",
                fontSize = 14.sp,
                color = InkSecondary,
            )
            Spacer(Modifier.height(8.dp))
            ReportReason.entries.forEach { r ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { reason = r }
                        .padding(vertical = 2.dp),
                ) {
                    RadioButton(
                        selected = reason == r,
                        onClick = { reason = r },
                        colors = RadioButtonDefaults.colors(selectedColor = BrickRed),
                    )
                    Text(
                        r.label,
                        fontSize = 15.sp,
                        fontWeight = if (reason == r) FontWeight.Bold else FontWeight.Normal,
                        color = Ink,
                    )
                }
                HorizontalDivider(color = DividerColor)
            }
            Spacer(Modifier.height(14.dp))
            Text("Ghi chú (không bắt buộc)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                placeholder = { Text("Cùng nội dung với 3 đánh giá khác…", color = InkTertiary) },
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = FieldBorder, focusedBorderColor = BrickRed),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp),
                ) { Text("Hủy", color = Ink, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { onSend(reason, note) },
                    enabled = !isSending,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
                    modifier = Modifier.weight(2f).height(50.dp),
                ) {
                    if (isSending) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Gửi báo cáo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
