package com.example.foodmapvn.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.data.model.NotificationItem
import com.example.foodmapvn.data.model.NotificationType
import com.example.foodmapvn.data.repository.NotificationRepository
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.BrickRedSoft
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SandDark
import kotlinx.coroutines.launch

/** UC-16: danh sách thông báo + đánh dấu đã đọc (từng cái hoặc tất cả). */
@Composable
fun NotificationsRoute(
    repository: NotificationRepository,
    onBack: () -> Unit,
    onPlaceClick: (placeId: String) -> Unit,
) {
    val flow = remember(repository) { repository.getNotifications() }
    val items by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val unread = items.count { !it.read }

    Scaffold(containerColor = Color.White) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(SandDark).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("←", fontSize = 18.sp, color = Ink) }
                Spacer(Modifier.width(10.dp))
                Text("Thông báo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                if (unread > 0) {
                    TextButton(onClick = { scope.launch { repository.markAllRead() } }) {
                        Text("Đọc tất cả", color = BrickRed, fontSize = 13.sp)
                    }
                }
            }
            if (items.isEmpty()) {
                Text(
                    "Chưa có thông báo nào", color = InkTertiary, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { n ->
                    NotificationRow(n) {
                        if (!n.read) scope.launch { repository.markRead(n.id) }
                        // Đề xuất được duyệt: mở thẳng quán mới tạo
                        if (n.type == NotificationType.SUGGESTION_APPROVED && !n.refId.isNullOrBlank()) onPlaceClick(n.refId)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificationItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (n.read) Color.White else BrickRedSoft)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(if (n.read) Color.Transparent else BrickRed))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(n.title, fontSize = 14.sp, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold, color = Ink)
            if (!n.body.isNullOrBlank()) Text(n.body, fontSize = 13.sp, color = InkSecondary)
            if (n.createdAt != null) Text(n.createdAt, fontSize = 11.sp, color = InkTertiary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}