package com.example.foodmapvn.ui.region

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.domain.ProvinceLocator

/**
 * Danh sách 34 tỉnh/thành có ô tìm kiếm (gõ không dấu, tìm được cả tên tỉnh cũ).
 * Dùng chung cho màn Chọn vùng và bảng chọn khu vực ở màn Bản đồ.
 *
 * @param myLocationSubtitle khác null -> hiện dòng "Vị trí của tôi" ở đầu danh sách.
 */
@Composable
fun ProvinceList(
    provinces: List<Province>,
    selectedId: String?,
    onSelect: (Province) -> Unit,
    modifier: Modifier = Modifier,
    myLocationSubtitle: String? = null,
    onMyLocation: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, provinces) {
        val q = ProvinceLocator.normalize(query)
        if (q.isBlank()) provinces
        else provinces.filter { p -> (listOf(p.name) + p.formerNames).any { ProvinceLocator.normalize(it).contains(q) } }
    }

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm tỉnh/thành (vd: da nang, vung tau)") },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyColumn {
            if (myLocationSubtitle != null && query.isBlank()) {
                item {
                    ProvinceRow(
                        icon = R.drawable.ic_my_location,
                        title = "Vị trí của tôi",
                        subtitle = myLocationSubtitle,
                        selected = false,
                        onClick = onMyLocation,
                    )
                    HorizontalDivider()
                }
            }
            items(filtered, key = { it.id }) { p ->
                val former = if (p.formerNames.isEmpty()) "" else " · gồm cả ${p.formerNames.joinToString(", ")}"
                ProvinceRow(
                    icon = R.drawable.ic_place_outline,
                    title = p.name,
                    subtitle = (if (p.isCity) "Thành phố trực thuộc TW" else "Tỉnh") + former,
                    selected = p.id == selectedId,
                    onClick = { onSelect(p) },
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Không tìm thấy tỉnh/thành nào",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvinceRow(icon: Int, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = "Đang chọn", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
