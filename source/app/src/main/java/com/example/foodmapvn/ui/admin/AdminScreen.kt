package com.example.foodmapvn.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminReport
import com.example.foodmapvn.data.model.AdminReportStatus
import com.example.foodmapvn.data.model.AdminSuggestion
import com.example.foodmapvn.data.model.AdminUser
import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.BrickRedSoft
import com.example.foodmapvn.ui.theme.DishBrown
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
import java.text.NumberFormat
import java.util.Locale

private val Bg = Color(0xFFF6F1EA)
private val Card = Color.White

/** Admin trong cùng app: 5 tab riêng (Tổng quan, Duyệt quán, Báo cáo, Nội dung, Người dùng) theo Figma. */
@Composable
fun AdminScreen(onBack: () -> Unit, adminViewModel: AdminViewModel) {
    val vm = adminViewModel
    DisposableEffect(vm) { onDispose { vm.clear() } }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    val stats by vm.stats.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val regionId by vm.selectedRegionId.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    val detail = detailId?.let { id -> suggestions.firstOrNull { it.suggestion.id == id } }

    BackHandler {
        when {
            detail != null -> detailId = null
            tab == 3 && regionId != null -> vm.selectRegion(null)
            tab != 0 -> tab = 0
            else -> onBack()
        }
    }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (detail == null) AdminBottomBar(tab, stats.pendingSuggestions, stats.newReports) { tab = it; detailId = null }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (detail != null) {
                SuggestionDetail(
                    item = detail,
                    onBack = { detailId = null },
                    onApprove = { vm.approve(detail); detailId = null },
                    onReject = { reason -> vm.reject(detail, reason); detailId = null },
                )
            } else when (tab) {
                0 -> OverviewTab(vm, onGo = { tab = it }, onExit = onBack)
                1 -> ReviewTab(vm, onOpen = { detailId = it.suggestion.id })
                2 -> ReportsTab(vm)
                3 -> ContentTab(vm)
                else -> UsersTab(vm)
            }
        }
    }
}

// ============================ Thanh điều hướng ============================

@Composable
private fun AdminBottomBar(selected: Int, pending: Int, reports: Int, onSelect: (Int) -> Unit) {
    val items = listOf("⌂" to "Tổng quan", "✓" to "Duyệt quán", "⚑" to "Báo cáo", "▦" to "Nội dung", "☺" to "Người dùng")
    NavigationBar(containerColor = Card) {
        items.forEachIndexed { i, (glyph, label) ->
            val badge = when (i) { 1 -> pending; 2 -> reports; else -> 0 }
            NavigationBarItem(
                selected = selected == i,
                onClick = { onSelect(i) },
                icon = { Text(if (badge > 0) "$glyph" else glyph, fontSize = 20.sp) },
                label = { Text(if (badge > 0) "$label ($badge)" else label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrickRed, selectedTextColor = BrickRed,
                    unselectedIconColor = InkTertiary, unselectedTextColor = InkTertiary,
                    indicatorColor = BrickRedSoft,
                ),
            )
        }
    }
}

@Composable
private fun Header(title: String, hello: String? = null, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Card).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 18.sp, color = Ink) }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            if (hello != null) Text(hello, fontSize = 12.sp, color = InkSecondary)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Box(Modifier.size(34.dp).clip(CircleShape).background(BrickRedSoft), contentAlignment = Alignment.Center) {
            Text("A", color = BrickRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Card).padding(14.dp),
        content = content,
    )
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun Segments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        labels.forEachIndexed { i, label ->
            Column(Modifier.weight(1f).clickable { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == selected) BrickRed else InkSecondary,
                )
                Box(Modifier.fillMaxWidth().height(2.dp).background(if (i == selected) BrickRed else DividerColor))
            }
        }
    }
}

@Composable
private fun Thumb(url: String?, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(10.dp)
    if (url.isNullOrBlank()) {
        Box(Modifier.size(size).clip(shape).background(DishBrown), contentAlignment = Alignment.Center) { Text("🍜", fontSize = 20.sp) }
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(shape))
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = InkTertiary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
}

private fun money(v: Long?): String =
    v?.let { NumberFormat.getInstance(Locale("vi", "VN")).format(it) + "đ" } ?: "—"

/** Hộp nhập lý do từ chối (bắt buộc – firestore.rules > isValidSuggestion / isValidBusiness). */
@Composable
private fun RejectDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Lý do từ chối") }, minLines = 2)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }, enabled = reason.isNotBlank()) { Text("Từ chối", color = BrickRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

// ============================ 1. Tổng quan (UC-20) ============================

@Composable
private fun OverviewTab(vm: AdminViewModel, onGo: (Int) -> Unit, onExit: () -> Unit) {
    val s by vm.stats.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Header("Quản trị", hello = "Xin chào,")
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Đề xuất chờ duyệt", s.pendingSuggestions.toString(), "Cần xử lý", BrickRed, Modifier.weight(1f))
                    StatCard("Báo cáo mới", s.newReports.toString(), "Chưa xem", Amber, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Quán bị cảnh báo giá", s.warnedPlaces.toString(), "Trên toàn vùng", BrickRed, Modifier.weight(1f))
                    StatCard("Đánh giá tuần này", s.reviewsThisWeek.toString(), "7 ngày qua", SuccessGreen, Modifier.weight(1f))
                }
            }
            item { Text("Lối tắt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 6.dp)) }
            item { Shortcut("Duyệt quán đề xuất", "${s.pendingSuggestions} quán đang chờ", s.pendingSuggestions) { onGo(1) } }
            item { Shortcut("Xử lý báo cáo", "${s.newReports} báo cáo mới", s.newReports) { onGo(2) } }
            item { Shortcut("Vùng & đặc sản", "${s.regionCount} vùng · ${s.dishCount} món", 0) { onGo(3) } }
            item { Shortcut("Người dùng", "%,d tài khoản".format(s.userCount), 0) { onGo(4) } }
            item { TextButton(onClick = onExit) { Text("Thoát khu vực Admin", color = InkSecondary) } }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, subColor: Color, modifier: Modifier) {
    Panel(modifier) {
        Text(label, fontSize = 11.sp, color = InkSecondary, maxLines = 1)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(sub, fontSize = 11.sp, color = subColor)
    }
}

@Composable
private fun Shortcut(title: String, sub: String, badge: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(sub, fontSize = 12.sp, color = InkSecondary)
        }
        if (badge > 0) Pill(badge.toString(), BrickRed, Color.White) else Text("›", fontSize = 20.sp, color = InkTertiary)
    }
}

// ============================ 2. Duyệt quán (UC-17) ============================

@Composable
private fun ReviewTab(vm: AdminViewModel, onOpen: (AdminSuggestion) -> Unit) {
    val tab by vm.suggestionTab.collectAsState()
    val list by vm.suggestions.collectAsState()
    val stats by vm.stats.collectAsState()
    val businesses by vm.pendingBusinesses.collectAsState()
    val tabs = listOf(SuggestionStatus.PENDING, SuggestionStatus.APPROVED, SuggestionStatus.REJECTED)
    var rejectingBusiness by remember { mutableStateOf<Business?>(null) }

    Column(Modifier.fillMaxSize()) {
        Header("Duyệt quán")
        Segments(
            listOf("Chờ duyệt (${stats.pendingSuggestions})", "Đã duyệt", "Từ chối"),
            tabs.indexOf(tab),
        ) { vm.selectSuggestionTab(tabs[it]) }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == SuggestionStatus.PENDING && businesses.isNotEmpty()) {
                item { Text("Doanh nghiệp chờ duyệt (${businesses.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink) }
                items(businesses, key = { "b_" + it.ownerId }) { b ->
                    Panel {
                        Text(b.name, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("${b.type.label} · gửi ${b.createdAt ?: "—"}", fontSize = 12.sp, color = InkSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { rejectingBusiness = b }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, BrickRed)) { Text("Từ chối", color = BrickRed) }
                            Button(onClick = { vm.reviewBusiness(b.ownerId, true) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Duyệt") }
                        }
                    }
                }
                item { Text("Quán đề xuất", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 6.dp)) }
            }
            if (list.isEmpty()) item { EmptyHint("Chưa có đề xuất nào") }
            items(list, key = { it.suggestion.id }) { item -> SuggestionRow(item) { onOpen(item) } }
        }
    }
    rejectingBusiness?.let { b ->
        RejectDialog("Từ chối doanh nghiệp", { rejectingBusiness = null }) { reason ->
            vm.reviewBusiness(b.ownerId, false, reason); rejectingBusiness = null
        }
    }
}

@Composable
private fun SuggestionRow(item: AdminSuggestion, onClick: () -> Unit) {
    val s = item.suggestion
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumb(s.photoUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text("${item.dishName} · ${item.regionName} · ${money(s.price)}", fontSize = 12.sp, color = InkSecondary, maxLines = 1)
            Text("Đề xuất bởi ${item.proposerName} · ${s.createdAt ?: ""}", fontSize = 11.sp, color = InkTertiary, maxLines = 1)
        }
        when (s.status) {
            SuggestionStatus.PENDING -> Pill("Chờ duyệt", Color(0xFFFBEFD9), Amber)
            SuggestionStatus.APPROVED -> Pill("Đã duyệt", SuccessGreenSoft, SuccessGreen)
            SuggestionStatus.REJECTED -> Pill("Từ chối", BrickRedSoft, BrickRed)
        }
    }
}

/** Chi tiết đề xuất: bản đồ, ảnh, thông tin, Từ chối / Duyệt. */
@Composable
private fun SuggestionDetail(item: AdminSuggestion, onBack: () -> Unit, onApprove: () -> Unit, onReject: (String) -> Unit) {
    val s = item.suggestion
    var rejecting by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Header("Chi tiết đề xuất", onBack = onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                // Vị trí đề xuất (bản đồ đầy đủ nằm ở màn Bản đồ; ở đây hiện toạ độ)
                Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE3EAE4)), contentAlignment = Alignment.Center) {
                    Text("📍 %.5f, %.5f".format(Locale.US, s.location.latitude, s.location.longitude), color = Ink, fontSize = 13.sp)
                }
            }
            if (!s.photoUrl.isNullOrBlank()) item { Thumb(s.photoUrl, 120.dp) }
            item {
                Panel {
                    InfoRow("Tên quán", s.name)
                    InfoRow("Món chính", item.dishName)
                    InfoRow("Giá tham khảo", money(s.price) + " / tô")
                    InfoRow("Giờ mở cửa", s.openHours ?: "—")
                    InfoRow("Địa chỉ", s.address)
                    InfoRow("Người đề xuất", item.proposerName)
                    InfoRow("Vùng", item.regionName)
                    if (s.status == SuggestionStatus.REJECTED) InfoRow("Lý do từ chối", s.rejectReason ?: "—")
                }
            }
        }
        if (s.status == SuggestionStatus.PENDING) {
            Row(Modifier.fillMaxWidth().background(Card).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { rejecting = true }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, BrickRed)) { Text("Từ chối", color = BrickRed) }
                Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Duyệt") }
            }
        }
    }
    if (rejecting) RejectDialog("Từ chối đề xuất", { rejecting = false }) { reason -> rejecting = false; onReject(reason) }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 13.sp, color = InkSecondary, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
    }
}

// ============================ 3. Báo cáo (UC-18) ============================

@Composable
private fun ReportsTab(vm: AdminViewModel) {
    val tab by vm.reportTab.collectAsState()
    val list by vm.reports.collectAsState()
    val stats by vm.stats.collectAsState()
    val tabs = listOf(AdminReportStatus.PENDING, AdminReportStatus.RESOLVED, AdminReportStatus.DISMISSED)
    Column(Modifier.fillMaxSize()) {
        Header("Báo cáo")
        Segments(listOf("Chưa xử lý (${stats.newReports})", "Đã xử lý", "Bỏ qua"), tabs.indexOf(tab)) { vm.selectReportTab(tabs[it]) }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (list.isEmpty()) item { EmptyHint("Không có báo cáo nào") }
            items(list, key = { it.id }) { r -> ReportCard(r, vm, showActions = tab == AdminReportStatus.PENDING) }
        }
    }
}

@Composable
private fun ReportCard(r: AdminReport, vm: AdminViewModel, showActions: Boolean) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(r.reasonLabel, BrickRedSoft, BrickRed)
            Spacer(Modifier.weight(1f))
            Text(r.createdAt ?: "", fontSize = 11.sp, color = InkTertiary)
        }
        Spacer(Modifier.height(8.dp))
        Text(r.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        if (!r.quote.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Sand).padding(8.dp)) {
                Text("\"${r.quote}\"", fontSize = 12.sp, color = InkSecondary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Báo bởi ${r.reporterName}", fontSize = 11.sp, color = InkTertiary)
        if (showActions) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.dismiss(r) }, modifier = Modifier.weight(1f)) { Text("Bỏ qua", color = Ink) }
                val (label, action) = when {
                    r.targetType == "REVIEW" -> "Ẩn đánh giá" to { vm.hideReview(r) }
                    r.reasonCode == "NOT_EXIST" -> "Đánh dấu đóng cửa" to { vm.markClosed(r) }
                    else -> "Đã xử lý" to { vm.resolve(r) }
                }
                Button(onClick = { action() }, modifier = Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = BrickRed)) {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

// ============================ 4. Đặc sản & vùng (UC-19) ============================

@Composable
private fun ContentTab(vm: AdminViewModel) {
    val regionId by vm.selectedRegionId.collectAsState()
    val regions by vm.regions.collectAsState()
    val dishes by vm.dishes.collectAsState()
    val region = regions.firstOrNull { it.id == regionId }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (region == null) {
            Header("Nội dung")
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (regions.isEmpty()) item { EmptyHint("Chưa có vùng nào") }
                items(regions, key = { it.id }) { r ->
                    Shortcut(r.name, "${r.dishCount} món · ${r.placeCount} quán", 0) { vm.selectRegion(r.id) }
                }
            }
        } else {
            Header("Đặc sản ${region.name}", onBack = { vm.selectRegion(null) })
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Số món", region.dishCount.toString(), "", Ink, Modifier.weight(1f))
                        StatCard("Số quán", region.placeCount.toString(), "", Ink, Modifier.weight(1f))
                    }
                }
                if (dishes.isEmpty()) item { EmptyHint("Vùng này chưa có món") }
                items(dishes, key = { it.id }) { d -> DishRow(d) { vm.setDishVisible(d, it) } }
                item {
                    Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BrickRed)) {
                        Text("+ Thêm món mới")
                    }
                }
            }
        }
    }
    if (adding) AddDishDialog({ adding = false }) { name, unit -> adding = false; vm.addDish(name, unit) }
}

@Composable
private fun DishRow(d: AdminDish, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumb(null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text("${d.priceText} · ${d.placeCount} quán", fontSize = 12.sp, color = InkSecondary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(checked = d.visible, onCheckedChange = onToggle)
            Text(if (d.visible) "Hiển thị" else "Đã ẩn", fontSize = 10.sp, color = if (d.visible) SuccessGreen else InkTertiary)
        }
    }
}

@Composable
private fun AddDishDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("tô") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm món mới") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên món") }, singleLine = true)
                Text("Đơn vị tính", fontSize = 12.sp, color = InkSecondary)
                // Khớp danh sách cho phép trong firestore.rules > isValidDish
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("tô", "cốc", "đĩa", "phần").forEach { u ->
                        val on = unit == u
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(if (on) BrickRed else Sand)
                                .clickable { unit = u }.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) { Text(u, color = if (on) Color.White else Ink, fontSize = 13.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, unit) }, enabled = name.isNotBlank()) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

// ============================ 5. Người dùng ============================

@Composable
private fun UsersTab(vm: AdminViewModel) {
    val users by vm.users.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }   // 0 Tất cả, 1 Hoạt động, 2 Bị khóa
    val shown = users.filter { u ->
        (query.isBlank() || u.name.contains(query, true) || u.email.contains(query, true)) &&
                when (filter) { 1 -> !u.locked; 2 -> u.locked; else -> true }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Người dùng")
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("Tìm theo tên hoặc email...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Tất cả", "Hoạt động", "Bị khóa").forEachIndexed { i, label ->
                val on = filter == i
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(if (on) BrickRed else Card)
                        .clickable { filter = i }.padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text(label, fontSize = 12.sp, color = if (on) Color.White else Ink) }
            }
        }
        LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.isEmpty()) item { EmptyHint("Không có người dùng phù hợp") }
            items(shown, key = { it.uid }) { u -> UserRow(u) { vm.setUserLocked(u, !u.locked) } }
        }
    }
}

@Composable
private fun UserRow(u: AdminUser, onToggleLock: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(BrickRedSoft), contentAlignment = Alignment.Center) {
            Text(u.name.take(1).uppercase(), color = BrickRed, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(u.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text(u.email, fontSize = 12.sp, color = InkSecondary, maxLines = 1)
        }
        when {
            u.isAdmin -> Pill("Admin", Sand, InkSecondary)
            u.locked -> {
                Pill("Bị khóa", BrickRedSoft, BrickRed)
                TextButton(onClick = onToggleLock) { Text("Mở khóa", color = SuccessGreen, fontSize = 12.sp) }
            }
            else -> {
                Pill("Hoạt động", SuccessGreenSoft, SuccessGreen)
                TextButton(onClick = onToggleLock) { Text("Khóa", color = BrickRed, fontSize = 12.sp) }
            }
        }
    }
}