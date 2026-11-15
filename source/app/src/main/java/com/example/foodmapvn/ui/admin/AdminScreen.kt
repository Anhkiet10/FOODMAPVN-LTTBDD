package com.example.foodmapvn.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.AdminDish
import com.example.foodmapvn.data.model.AdminRegion
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
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
import java.text.NumberFormat
import java.util.Locale

private val Bg = Color(0xFFF6F1EA)
private val Surface = Color.White
private val AmberSoft = Color(0xFFFBEFD9)
private val PadList = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** Admin (UC-17..UC-20): 5 tab riêng theo Figma – Tổng quan, Duyệt quán, Báo cáo, Nội dung, Người dùng. */
@Composable
fun AdminScreen(onBack: () -> Unit, adminViewModel: AdminViewModel) {
    val vm = adminViewModel
    DisposableEffect(vm) { onDispose { vm.clear() } }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var userQuery by rememberSaveable { mutableStateOf("") }

    val suggestions by vm.suggestions.collectAsState()
    val regionId by vm.selectedRegionId.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

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
        bottomBar = { if (detail == null) AdminBottomBar(tab) { tab = it; detailId = null } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (detail != null) {
                SuggestionDetail(
                    vm = vm, item = detail,
                    onBack = { detailId = null },
                    onApprove = { vm.approve(detail); detailId = null },
                    onReject = { reason -> vm.reject(detail, reason); detailId = null },
                )
            } else when (tab) {
                0 -> OverviewTab(vm, onGo = { tab = it }, onSearch = { userQuery = it; tab = 4 })
                1 -> ReviewTab(vm, onOpen = { detailId = it.suggestion.id })
                2 -> ReportsTab(vm)
                3 -> ContentTab(vm)
                else -> UsersTab(vm, userQuery) { userQuery = it }
            }
        }
    }
}

// ============================ Thanh điều hướng ============================

@Composable
private fun AdminBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        R.drawable.ic_home to "Tổng quan", R.drawable.ic_check to "Duyệt quán", R.drawable.ic_flag to "Báo cáo",
        R.drawable.ic_place_outline to "Nội dung", R.drawable.ic_person to "Người dùng",
    )
    NavigationBar(containerColor = Surface) {
        items.forEachIndexed { i, (icon, label) ->
            NavigationBarItem(
                selected = selected == i, onClick = { onSelect(i) },
                icon = { Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp)) },
                label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrickRed, selectedTextColor = BrickRed,
                    unselectedIconColor = InkTertiary, unselectedTextColor = InkTertiary,
                    indicatorColor = BrickRedSoft,
                ),
            )
        }
    }
}

// ============================ Thành phần dùng chung ============================

@Composable
private fun Header(
    title: String, hello: String? = null, crumb: String? = null, initial: String = "A",
    onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Surface).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_back), null, tint = Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            if (hello != null) Text(hello, fontSize = 12.sp, color = InkSecondary)
            if (crumb != null) Text(crumb, fontSize = 11.sp, color = InkTertiary)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        if (trailing != null) trailing() else {
            Box(Modifier.size(34.dp).clip(CircleShape).background(BrickRedSoft), contentAlignment = Alignment.Center) {
                Text(initial.ifBlank { "A" }.take(1).uppercase(), color = BrickRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Surface).padding(14.dp), content = content)
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

/** Tab gạch chân (Duyệt quán). */
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

/** Chip bo tròn (Báo cáo, Người dùng). */
@Composable
private fun Chips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (on) BrickRed else Surface)
                    .border(1.dp, if (on) BrickRed else DividerColor, RoundedCornerShape(50))
                    .clickable { onSelect(i) }.padding(horizontal = 12.dp, vertical = 6.dp),
            ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else Ink) }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, onSearch: (() -> Unit)? = null) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true, modifier = modifier,
        placeholder = { Text(hint, fontSize = 13.sp, color = InkTertiary) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, tint = InkTertiary, modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
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

/** Nhập lý do (bắt buộc) – firestore.rules yêu cầu rejectReason khi từ chối. */
@Composable
private fun ReasonDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Lý do từ chối") }, minLines = 2) },
        confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }, enabled = reason.isNotBlank()) { Text("Từ chối", color = BrickRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

/** Menu ⋮ */
@Composable
private fun MoreMenu(items: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text("⋮", fontSize = 22.sp, color = InkSecondary, modifier = Modifier.clickable { open = true }.padding(horizontal = 8.dp, vertical = 2.dp))
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) -> DropdownMenuItem(text = { Text(label) }, onClick = { open = false; action() }) }
        }
    }
}

// ============================ 1. Tổng quan (UC-20) ============================

@Composable
private fun OverviewTab(vm: AdminViewModel, onGo: (Int) -> Unit, onSearch: (String) -> Unit) {
    val s by vm.stats.collectAsState()
    val name by vm.adminName.collectAsState()
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Header("Quản trị", hello = "Xin chào, ${name.ifBlank { "Admin" }}", initial = name)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchField(query, { query = it }, "Tìm quán, món, người dùng...", Modifier.weight(1f)) { onSearch(query) }
                    Button(onClick = { onGo(3) }, colors = ButtonDefaults.buttonColors(containerColor = BrickRed), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(painterResource(R.drawable.ic_place), null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Tất cả", fontSize = 12.sp)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Đề xuất chờ duyệt", s.pendingSuggestions.toString(), "+${s.pendingToday} hôm nay", BrickRed, Modifier.weight(1f))
                    StatCard("Báo cáo mới", s.newReports.toString(), "${s.newReports} chưa xử lý", Amber, Modifier.weight(1f))
                }
            }
            item {
                val delta = if (s.reviewsLastWeek > 0) (s.reviewsThisWeek - s.reviewsLastWeek) * 100 / s.reviewsLastWeek else null
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Quán bị cảnh báo giá", s.warnedPlaces.toString(), "Trên ${s.warnedRegions} vùng", BrickRed, Modifier.weight(1f))
                    StatCard(
                        "Đánh giá tuần này", s.reviewsThisWeek.toString(),
                        if (delta != null) "${if (delta >= 0) "+" else ""}$delta% so tuần trước" else "7 ngày qua",
                        SuccessGreen, Modifier.weight(1f),
                    )
                }
            }
            item { Text("Lối tắt", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 6.dp)) }
            item { Shortcut(R.drawable.ic_check, BrickRedSoft, BrickRed, "Duyệt quán đề xuất", "${s.pendingSuggestions} quán đang chờ", s.pendingSuggestions, BrickRed) { onGo(1) } }
            item { Shortcut(R.drawable.ic_flag, AmberSoft, Amber, "Xử lý báo cáo", "${s.newReports} báo cáo mới", s.newReports, Amber) { onGo(2) } }
            item { Shortcut(R.drawable.ic_place_outline, SuccessGreenSoft, SuccessGreen, "Vùng & đặc sản", "${s.regionCount} vùng · ${s.dishCount} món", 0, BrickRed) { onGo(3) } }
            item { Shortcut(R.drawable.ic_person, Sand, InkSecondary, "Người dùng", "${NumberFormat.getInstance(Locale.US).format(s.userCount)} tài khoản", 0, BrickRed) { onGo(4) } }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, subColor: Color, modifier: Modifier) {
    Panel(modifier) {
        Text(label, fontSize = 11.sp, color = InkSecondary, maxLines = 1)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(sub, fontSize = 11.sp, color = subColor, maxLines = 1)
    }
}

@Composable
private fun Shortcut(icon: Int, tileBg: Color, tileFg: Color, title: String, sub: String, badge: Int, badgeColor: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(tileBg), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = tileFg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(sub, fontSize = 12.sp, color = InkSecondary)
        }
        if (badge > 0) Pill(badge.toString(), badgeColor, Color.White)
        else Icon(painterResource(R.drawable.ic_chevron_right), null, tint = InkTertiary, modifier = Modifier.size(20.dp))
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
        Segments(listOf("Chờ duyệt (${stats.pendingSuggestions})", "Đã duyệt", "Từ chối"), tabs.indexOf(tab)) { vm.selectSuggestionTab(tabs[it]) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        ReasonDialog("Từ chối doanh nghiệp", { rejectingBusiness = null }) { reason -> vm.reviewBusiness(b.ownerId, false, reason); rejectingBusiness = null }
    }
}

@Composable
private fun StatusPill(status: SuggestionStatus) = when (status) {
    SuggestionStatus.PENDING -> Pill("Chờ duyệt", AmberSoft, Amber)
    SuggestionStatus.APPROVED -> Pill("Đã duyệt", SuccessGreenSoft, SuccessGreen)
    SuggestionStatus.REJECTED -> Pill("Từ chối", BrickRedSoft, BrickRed)
}

@Composable
private fun SuggestionRow(item: AdminSuggestion, onClick: () -> Unit) {
    val s = item.suggestion
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumb(s.photoUrl)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text("${item.dishName} · ${item.regionName} · ${money(s.price)}", fontSize = 12.sp, color = InkSecondary, maxLines = 1)
            Text("Đề xuất bởi ${item.proposerName} · ${s.createdAt ?: ""}", fontSize = 11.sp, color = InkTertiary, maxLines = 1)
        }
        StatusPill(s.status)
    }
}

/** Chi tiết đề xuất (Figma "Admin duyệt quán (chi tiết)"): bản đồ, ảnh, thông tin, quán trùng, Từ chối / Sửa / Duyệt. */
@Composable
private fun SuggestionDetail(vm: AdminViewModel, item: AdminSuggestion, onBack: () -> Unit, onApprove: () -> Unit, onReject: (String) -> Unit) {
    val s = item.suggestion
    val nearby by vm.nearby.collectAsState()
    var rejecting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(s.id) { vm.loadNearby(item) }

    Column(Modifier.fillMaxSize()) {
        Header("Chi tiết đề xuất", onBack = onBack, trailing = { StatusPill(s.status) })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE3EAE4))) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_place), null, tint = BrickRed, modifier = Modifier.size(36.dp))
                        Text("%.5f, %.5f".format(Locale.US, s.location.latitude, s.location.longitude), fontSize = 11.sp, color = InkSecondary)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { Pill(item.regionName, Surface, Ink) }
                }
            }
            if (!s.photoUrl.isNullOrBlank()) item { Thumb(s.photoUrl, 110.dp) }
            item {
                Panel {
                    InfoRow("Tên quán", s.name)
                    InfoRow("Món chính", item.dishName)
                    InfoRow("Giá tham khảo", money(s.price) + " / tô")
                    InfoRow("Giờ mở cửa", s.openHours ?: "—")
                    InfoRow("Địa chỉ", s.address)
                    InfoRow("Người đề xuất", item.proposerName)
                    if (s.status == SuggestionStatus.REJECTED) InfoRow("Lý do từ chối", s.rejectReason ?: "—")
                }
            }
            item {
                val n = nearby
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (n.isNullOrEmpty()) SuccessGreenSoft else AmberSoft).padding(12.dp)) {
                    when {
                        n == null -> Text("Đang kiểm tra quán trùng trong 15 m...", fontSize = 12.sp, color = InkSecondary)
                        n.isEmpty() -> Text("Không có quán nào trùng trong bán kính 15 m", fontSize = 12.sp, color = SuccessGreen)
                        else -> Column {
                            Text("Quán đã có gần đó (≤ 15 m)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber)
                            n.forEach { Text("• ${it.name} – cách ${it.distanceM} m", fontSize = 12.sp, color = Ink) }
                        }
                    }
                }
            }
        }
        if (s.status == SuggestionStatus.PENDING) {
            Row(Modifier.fillMaxWidth().background(Surface).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { rejecting = true }, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, BrickRed)) { Text("Từ chối", color = BrickRed) }
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) { Text("Sửa", color = Ink) }
                Button(onClick = onApprove, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Duyệt") }
            }
        }
    }
    if (rejecting) ReasonDialog("Từ chối đề xuất", { rejecting = false }) { reason -> rejecting = false; onReject(reason) }
    if (editing) EditSuggestionDialog(item, { editing = false }) { name, address, price, hours ->
        editing = false; vm.editSuggestion(item, name, address, price, hours)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 13.sp, color = InkSecondary, modifier = Modifier.weight(0.4f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun EditSuggestionDialog(item: AdminSuggestion, onDismiss: () -> Unit, onConfirm: (String, String, Long?, String?) -> Unit) {
    val s = item.suggestion
    var name by remember { mutableStateOf(s.name) }
    var address by remember { mutableStateOf(s.address) }
    var price by remember { mutableStateOf(s.price?.toString() ?: "") }
    var hours by remember { mutableStateOf(s.openHours ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Sửa đề xuất") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên quán") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Địa chỉ") })
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Giá tham khảo (đ)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(hours, { hours = it }, label = { Text("Giờ mở cửa") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, address, price.toLongOrNull(), hours.ifBlank { null }) }, enabled = name.isNotBlank() && address.isNotBlank()) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
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
        Chips(listOf("Chưa xử lý (${stats.newReports})", "Đã ẩn", "Bỏ qua"), tabs.indexOf(tab)) { vm.selectReportTab(tabs[it]) }
        LazyColumn(contentPadding = PadList, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (list.isEmpty()) item { EmptyHint("Không có báo cáo nào") }
            items(list, key = { it.id }) { r -> ReportCard(r, vm, showActions = tab == AdminReportStatus.PENDING) }
        }
    }
}

@Composable
private fun ReportCard(r: AdminReport, vm: AdminViewModel, showActions: Boolean) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(r.reasonLabel, AmberSoft, Amber)
            Spacer(Modifier.weight(1f))
            Text(r.createdAt ?: "", fontSize = 11.sp, color = InkTertiary)
        }
        Spacer(Modifier.height(8.dp))
        Text(r.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        if (!r.quote.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Sand).padding(8.dp)) {
                Text("\"${r.quote}\"", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = InkSecondary)
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
                Button(onClick = { action() }, modifier = Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = BrickRed)) { Text(label, maxLines = 1) }
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
    var addingDish by remember { mutableStateOf(false) }
    var editingDish by remember { mutableStateOf<AdminDish?>(null) }
    var addingRegion by remember { mutableStateOf(false) }
    var editingRegion by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        if (region == null) {
            Header("Nội dung")
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Button(onClick = { addingRegion = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BrickRed)) { Text("+ Thêm vùng") }
                }
                if (regions.isEmpty()) item { EmptyHint("Chưa có vùng nào") }
                items(regions, key = { it.id }) { r ->
                    Shortcut(R.drawable.ic_place, if (r.visible) SuccessGreenSoft else Sand, if (r.visible) SuccessGreen else InkSecondary,
                        r.name + if (r.visible) "" else " (đã ẩn)", "${r.dishCount} món · ${r.placeCount} quán", 0, BrickRed) { query = ""; vm.selectRegion(r.id) }
                }
            }
        } else {
            Header(
                "Đặc sản ${region.name}", crumb = "Vùng › ${region.name}", onBack = { vm.selectRegion(null) },
                trailing = { MoreMenu(listOf("Đổi tên vùng" to { editingRegion = true }, (if (region.visible) "Ẩn vùng" else "Hiện vùng") to { vm.editRegion(region, region.name, !region.visible) })) },
            )
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Số món", region.dishCount.toString(), "", Ink, Modifier.weight(1f))
                        StatCard("Số quán", region.placeCount.toString(), "", Ink, Modifier.weight(1f))
                    }
                }
                item { SearchField(query, { query = it }, "Tìm món trong vùng...", Modifier.fillMaxWidth()) }
                val shown = dishes.filter { query.isBlank() || it.name.contains(query, true) }
                if (shown.isEmpty()) item { EmptyHint("Vùng này chưa có món") }
                items(shown, key = { it.id }) { d ->
                    DishRow(d, onEdit = { editingDish = d }, onToggle = { vm.setDishVisible(d, !d.visible) })
                }
                item {
                    Button(onClick = { addingDish = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BrickRed)) { Text("+ Thêm món mới") }
                }
            }
        }
    }
    if (addingDish) DishDialog("Thêm món mới", "", "tô", { addingDish = false }) { n, u -> addingDish = false; vm.addDish(n, u) }
    editingDish?.let { d -> DishDialog("Sửa món", d.name, d.unit, { editingDish = null }) { n, u -> editingDish = null; vm.editDish(d, n, u) } }
    if (addingRegion) RegionDialog("Thêm vùng", "", true, { addingRegion = false }) { n, lat, lng -> addingRegion = false; vm.addRegion(n, lat ?: 0.0, lng ?: 0.0) }
    if (editingRegion && region != null) RegionDialog("Đổi tên vùng", region.name, false, { editingRegion = false }) { n, _, _ -> editingRegion = false; vm.editRegion(region, n, region.visible) }
}

@Composable
private fun DishRow(d: AdminDish, onEdit: () -> Unit, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumb(null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
            Text("${d.priceText} · ${d.placeCount} quán", fontSize = 12.sp, color = InkSecondary)
        }
        if (d.visible) Pill("Hiển thị", SuccessGreenSoft, SuccessGreen) else Pill("Bản nháp", Sand, InkSecondary)
        MoreMenu(listOf("Sửa món" to onEdit, (if (d.visible) "Ẩn món" else "Hiện món") to onToggle))
    }
}

@Composable
private fun DishDialog(title: String, initialName: String, initialUnit: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(initialUnit) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên món") }, singleLine = true)
                Text("Đơn vị tính", fontSize = 12.sp, color = InkSecondary)
                // Khớp danh sách cho phép trong firestore.rules > isValidDish
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("tô", "cốc", "đĩa", "phần").forEach { u ->
                        val on = unit == u
                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) BrickRed else Sand).clickable { unit = u }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(u, color = if (on) Color.White else Ink, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, unit) }, enabled = name.isNotBlank()) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
private fun RegionDialog(title: String, initialName: String, withCoords: Boolean, onDismiss: () -> Unit, onConfirm: (String, Double?, Double?) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    val coordsOk = !withCoords || (lat.toDoubleOrNull() != null && lng.toDoubleOrNull() != null)
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên vùng") }, singleLine = true)
                if (withCoords) {
                    // Rules: vùng bắt buộc có tâm bản đồ (center: geopoint)
                    OutlinedTextField(lat, { lat = it }, label = { Text("Vĩ độ tâm (vd. 16.4637)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(lng, { lng = it }, label = { Text("Kinh độ tâm (vd. 107.5909)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, lat.toDoubleOrNull(), lng.toDoubleOrNull()) }, enabled = name.isNotBlank() && coordsOk) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

// ============================ 5. Người dùng ============================

private enum class UserState { ADMIN, LOCKED, REPORTED, ACTIVE }

private fun AdminUser.state() = when {
    isAdmin -> UserState.ADMIN
    locked -> UserState.LOCKED
    reportCount > 0 -> UserState.REPORTED
    else -> UserState.ACTIVE
}

@Composable
private fun UsersTab(vm: AdminViewModel, query: String, onQuery: (String) -> Unit) {
    val users by vm.users.collectAsState()
    var filter by rememberSaveable { mutableIntStateOf(0) }   // 0 Tất cả, 1 Hoạt động, 2 Bị báo cáo, 3 Bị khóa
    var selected by remember { mutableStateOf<AdminUser?>(null) }
    val shown = users.filter { u ->
        (query.isBlank() || u.name.contains(query, true) || u.email.contains(query, true)) &&
                when (filter) { 1 -> !u.locked; 2 -> u.reportCount > 0; 3 -> u.locked; else -> true }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Người dùng")
        SearchField(query, onQuery, "Tìm theo tên hoặc email...", Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Chips(listOf("Tất cả", "Hoạt động", "Bị báo cáo", "Bị khóa"), filter) { filter = it }
        LazyColumn(contentPadding = PadList, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shown.isEmpty()) item { EmptyHint("Không có người dùng phù hợp") }
            items(shown, key = { it.uid }) { u -> UserRow(u, vm) { selected = u } }
        }
    }
    selected?.let { u -> UserDialog(u, { selected = null }) { vm.setUserLocked(u, !u.locked); selected = null } }
}

@Composable
private fun UserRow(u: AdminUser, vm: AdminViewModel, onClick: () -> Unit) {
    val st = u.state()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(BrickRedSoft), contentAlignment = Alignment.Center) {
                Text(u.name.take(1).uppercase(), color = BrickRed, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
                Text(u.email, fontSize = 12.sp, color = InkSecondary, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            when (st) {
                UserState.ADMIN -> Pill("Admin", Sand, InkSecondary)
                UserState.LOCKED -> Pill("Bị khóa", BrickRedSoft, BrickRed)
                UserState.REPORTED -> Pill("Bị báo cáo", AmberSoft, Amber)
                UserState.ACTIVE -> Pill("Hoạt động", SuccessGreenSoft, SuccessGreen)
            }
        }
        if (st == UserState.REPORTED) {
            Text("${u.reportCount} báo cáo đang chờ xử lý", fontSize = 11.sp, color = BrickRed, modifier = Modifier.padding(top = 6.dp))
            Button(
                onClick = { vm.setUserLocked(u, true) }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
            ) { Text("Khóa tài khoản") }
        }
    }
}

@Composable
private fun UserDialog(u: AdminUser, onDismiss: () -> Unit, onToggleLock: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(u.name) },
        text = {
            Column {
                Text(u.email, fontSize = 13.sp, color = InkSecondary)
                Text("Trạng thái: " + when (u.state()) { UserState.ADMIN -> "Quản trị viên"; UserState.LOCKED -> "Bị khóa"; UserState.REPORTED -> "Bị báo cáo (${u.reportCount})"; UserState.ACTIVE -> "Hoạt động" }, fontSize = 13.sp, color = Ink, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = {
            if (!u.isAdmin) TextButton(onClick = onToggleLock) { Text(if (u.locked) "Mở khóa tài khoản" else "Khóa tài khoản", color = if (u.locked) SuccessGreen else BrickRed) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}