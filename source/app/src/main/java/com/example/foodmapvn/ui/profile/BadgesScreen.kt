package com.example.foodmapvn.ui.profile

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.foodmapvn.R
import com.example.foodmapvn.data.local.VietnamProvinces
import com.example.foodmapvn.data.model.BadgeStatus
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.model.RegionCheckIns
import com.example.foodmapvn.data.repository.UserRepository
import com.example.foodmapvn.di.appContainer
import com.example.foodmapvn.domain.BadgeCalculator
import com.example.foodmapvn.domain.BusinessRules
import com.example.foodmapvn.ui.components.ProgressBar
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Huy hiệu của một vùng, đã tính sẵn để hiển thị. */
data class RegionBadgeItem(
    val province: Province,
    val status: BadgeStatus,
    val description: String,
    val progress: Float,
    val data: RegionCheckIns?,
)

/** Màn "Huy hiệu của tôi": 34 vùng + các quán đã check-in ở mỗi vùng. */
class BadgesViewModel(
    userRepository: UserRepository,
    private val badgeCalculator: BadgeCalculator,
) : ViewModel() {

    val checkIns: StateFlow<Map<String, RegionCheckIns>?> = userRepository.getCheckInsByRegion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun items(map: Map<String, RegionCheckIns>): List<RegionBadgeItem> {
        val collator = Collator.getInstance(Locale("vi"))
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return VietnamProvinces.all.map { p ->
            val d = map[p.id]
            val places = d?.places?.size ?: 0
            val prog = badgeCalculator.progress(
                checkIns = d?.checkIns ?: 0,
                distinctPlaces = places,
                achievedDate = d?.achievedAtMillis?.let { fmt.format(Date(it)) },
            )
            RegionBadgeItem(p, prog.status, prog.description, prog.progress, d)
        }.sortedWith(
            compareBy<RegionBadgeItem> { it.status.ordinal }   // Đã đạt -> Đang tiến -> Chưa có
                .thenByDescending { it.data?.checkIns ?: 0 }
                .thenComparator { a, b -> collator.compare(a.province.name, b.province.name) }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                BadgesViewModel(c.userRepository, c.badgeCalculator)
            }
        }
    }
}

private enum class BadgeFilter(val label: String) { ALL("Tất cả"), ACHIEVED("Đã đạt"), PROGRESS("Đang tích luỹ"), NONE("Chưa có") }

@Composable
fun BadgesRoute(
    onBack: () -> Unit,
    onPlaceClick: (placeId: String) -> Unit,
    viewModel: BadgesViewModel = viewModel(factory = BadgesViewModel.Factory),
) {
    val map by viewModel.checkIns.collectAsStateWithLifecycle()
    val items = remember(map) { viewModel.items(map.orEmpty()) }
    BadgesScreen(items = items, isLoading = map == null, onBack = onBack, onPlaceClick = onPlaceClick)
}

@Composable
fun BadgesScreen(
    items: List<RegionBadgeItem>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlaceClick: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(BadgeFilter.ALL) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val rules = remember { BusinessRules() }
    val achieved = items.count { it.status == BadgeStatus.ACHIEVED }
    val totalCheckIns = items.sumOf { it.data?.checkIns ?: 0 }
    val shown = items.filter {
        when (filter) {
            BadgeFilter.ALL -> true
            BadgeFilter.ACHIEVED -> it.status == BadgeStatus.ACHIEVED
            BadgeFilter.PROGRESS -> it.status == BadgeStatus.IN_PROGRESS
            BadgeFilter.NONE -> it.status == BadgeStatus.LOCKED
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(SandDark).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại", tint = Ink, modifier = Modifier.size(22.dp))
                }
                Text("Huy hiệu của tôi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.align(Alignment.Center))
            }
        }
        // Giải thích cách đạt huy hiệu
        item {
            Column(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Sand)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Huy hiệu Người địa phương", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                }
                Text(
                    "Đạt khi bạn check-in ít nhất ${rules.localBadgeCheckIns} lần tại ít nhất ${rules.localBadgeMinPlaces} quán khác nhau " +
                        "trong cùng một tỉnh/thành. Có huy hiệu, đánh giá của bạn ở vùng đó được tính trọng số ×" +
                        "${rules.localReviewWeight.toString().replace('.', ',')} khi tính điểm quán.",
                    fontSize = 14.sp,
                    color = InkSecondary,
                )
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("$achieved / ${items.size}", "vùng đã đạt", Modifier.weight(1f))
                StatBox("$totalCheckIns", "lần check-in", Modifier.weight(1f))
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BadgeFilter.entries) { f ->
                    val sel = f == filter
                    Text(
                        f.label,
                        fontSize = 14.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                        color = if (sel) Color.White else Ink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) BrickRed else Color.White)
                            .then(if (sel) Modifier else Modifier.border(1.dp, FieldBorder, RoundedCornerShape(50)))
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
        if (isLoading) {
            item { Text("Đang tải check-in của bạn…", color = InkSecondary, modifier = Modifier.padding(16.dp)) }
        }
        if (!isLoading && shown.isEmpty()) {
            item { Text("Không có vùng nào ở mục này.", color = InkSecondary, modifier = Modifier.padding(16.dp)) }
        }
        items(shown, key = { it.province.id }) { item ->
            BadgeRow(
                item = item,
                rules = rules,
                expanded = expanded == item.province.id,
                onToggle = { expanded = if (expanded == item.province.id) null else item.province.id },
                onPlaceClick = onPlaceClick,
            )
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Sand).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, fontSize = 13.sp, color = InkSecondary)
    }
}

@Composable
private fun BadgeRow(
    item: RegionBadgeItem,
    rules: BusinessRules,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlaceClick: (String) -> Unit,
) {
    val (tint, accent, bar) = when (item.status) {
        BadgeStatus.ACHIEVED -> Triple(SuccessGreen, SuccessGreen, SuccessGreen)
        BadgeStatus.IN_PROGRESS -> Triple(SandDark, Amber, Amber)
        BadgeStatus.LOCKED -> Triple(SandDark, InkTertiary, SandDark)
    }
    val places = item.data?.places.orEmpty()
    val fmt = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    Column(Modifier.animateContentSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = places.isNotEmpty(), onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(if (item.status == BadgeStatus.ACHIEVED) R.drawable.ic_shield_check else R.drawable.ic_shield),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.province.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    item.description + if (places.isNotEmpty()) " · ${places.size}/${rules.localBadgeMinPlaces} quán" else "",
                    fontSize = 13.sp,
                    color = accent,
                )
            }
            ProgressBar(progress = item.progress, color = bar, trackColor = SandDark, modifier = Modifier.width(90.dp).height(8.dp))
            if (places.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    painterResource(if (expanded) R.drawable.ic_expand_more else R.drawable.ic_chevron_right),
                    contentDescription = if (expanded) "Thu gọn" else "Xem quán đã check-in",
                    tint = InkSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // Các quán đã check-in ở vùng này
        if (expanded) {
            Column(Modifier.padding(start = 64.dp, end = 16.dp, bottom = 8.dp)) {
                places.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPlaceClick(p.placeId) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                        Spacer(Modifier.width(10.dp))
                        Text(p.name, fontSize = 14.sp, color = Ink, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(
                            "${p.count} lần · ${fmt.format(Date(p.lastAtMillis))}",
                            fontSize = 12.sp,
                            color = InkSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SuccessGreenSoft)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
    }
}
