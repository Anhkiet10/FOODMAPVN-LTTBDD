package com.example.foodmapvn.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.ui.region.ProvinceList

/** Thanh tìm kiếm phía trên bản đồ: hiện khu vực đang xem, bấm vào để chọn tỉnh/thành khác. */
@Composable
fun AreaSearchBar(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "Chọn khu vực")
        }
    }
}

/** Bảng chọn khu vực ở màn Bản đồ: dùng lại danh sách tỉnh/thành của màn Chọn vùng. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvincePickerSheet(
    provinces: List<Province>,
    selectedId: String?,
    onMyLocation: () -> Unit,
    onSelect: (Province) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            "Chọn tỉnh, thành phố",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        ProvinceList(
            provinces = provinces,
            selectedId = selectedId,
            onSelect = onSelect,
            myLocationSubtitle = "Chuyển tới tỉnh/thành bạn đang đứng",
            onMyLocation = onMyLocation,
        )
    }
}
