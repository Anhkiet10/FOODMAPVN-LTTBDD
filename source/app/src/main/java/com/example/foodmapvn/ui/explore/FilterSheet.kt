package com.example.foodmapvn.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.domain.ExploreFilters

private val PRICE_OPTIONS = listOf<Pair<String, Long?>>("Bất kỳ" to null, "≤ 30k" to 30_000L, "≤ 50k" to 50_000L, "≤ 100k" to 100_000L)
private val DISTANCE_OPTIONS = listOf<Pair<String, Double?>>("Bất kỳ" to null, "≤ 1 km" to 1.0, "≤ 3 km" to 3.0, "≤ 5 km" to 5.0)
private val TRUST_OPTIONS = listOf<Pair<String, Int?>>("Bất kỳ" to null, "≥ 70" to 70, "≥ 80" to 80, "≥ 90" to 90)

/** Bộ lọc nâng cao (UC-05). Chỉnh trong bảng, bấm "Áp dụng" mới lọc. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    initial: ExploreFilters,
    hasLocation: Boolean,
    onApply: (ExploreFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Bộ lọc", style = MaterialTheme.typography.titleLarge)

            OptionGroup("Giá một phần", PRICE_OPTIONS, draft.maxPrice) { draft = draft.copy(maxPrice = it) }
            OptionGroup(
                title = if (hasLocation) "Khoảng cách từ bạn" else "Khoảng cách (cần quyền vị trí)",
                options = DISTANCE_OPTIONS,
                selected = draft.maxDistanceKm,
                enabled = hasLocation,
            ) { draft = draft.copy(maxDistanceKm = it) }
            OptionGroup("Điểm uy tín của quán", TRUST_OPTIONS, draft.minTrust) { draft = draft.copy(minTrust = it) }

            SwitchRow("Chỉ quán đang mở cửa", draft.openNow) { draft = draft.copy(openNow = it) }
            SwitchRow("Ẩn quán bị cảnh báo giá", draft.hidePriceWarning) { draft = draft.copy(hidePriceWarning = it) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                OutlinedButton(onClick = { draft = ExploreFilters() }, modifier = Modifier.weight(1f)) { Text("Đặt lại") }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text(if (draft.isEmpty) "Áp dụng" else "Áp dụng (${draft.activeCount})")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionGroup(
    title: String,
    options: List<Pair<String, T?>>,
    selected: T?,
    enabled: Boolean = true,
    onSelect: (T?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
