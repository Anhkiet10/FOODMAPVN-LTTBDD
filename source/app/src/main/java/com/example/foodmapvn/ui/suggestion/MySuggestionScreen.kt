package com.example.foodmapvn.ui.suggestion

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.data.model.PlaceSuggestion
import com.example.foodmapvn.data.model.SuggestionStatus
import com.example.foodmapvn.data.repository.SuggestionRepository
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.BrickRedSoft
import com.example.foodmapvn.ui.theme.DividerColor
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft
import java.text.NumberFormat
import java.util.Locale

/** UC-14 (phần theo dõi): Đề xuất của tôi – 3 tab Chờ duyệt / Đã duyệt / Từ chối (kèm lý do). */
@Composable
fun MySuggestionsRoute(
    repository: SuggestionRepository,
    onBack: () -> Unit,
    onNewSuggestionClick: () -> Unit,
) {
    val flow = remember(repository) { repository.getMySuggestions() }
    val all by flow.collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(SuggestionStatus.PENDING, SuggestionStatus.APPROVED, SuggestionStatus.REJECTED)
    val labels = listOf("Chờ duyệt", "Đã duyệt", "Từ chối")
    val shown = all.filter { it.status == tabs[tab] }

    Scaffold(containerColor = Color.White) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(SandDark).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("←", fontSize = 18.sp, color = Ink) }
                Spacer(Modifier.width(10.dp))
                Text("Đề xuất của tôi", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                labels.forEachIndexed { i, label ->
                    val count = all.count { it.status == tabs[i] }
                    Column(Modifier.weight(1f).clickable { tab = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$label ($count)", fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = if (i == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (i == tab) BrickRed else InkSecondary,
                        )
                        Box(Modifier.fillMaxWidth().height(2.dp).background(if (i == tab) BrickRed else DividerColor))
                    }
                }
            }
            if (shown.isEmpty()) {
                Text("Chưa có đề xuất nào ở mục này", color = InkTertiary, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(40.dp))
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { SuggestionCard(it) }
            }
            Button(
                onClick = onNewSuggestionClick,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
            ) { Text("+ Đề xuất quán mới") }
        }
    }
}

@Composable
private fun SuggestionCard(s: PlaceSuggestion) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SandDark).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
            val (label, bg, fg) = when (s.status) {
                SuggestionStatus.PENDING -> Triple("Chờ duyệt", Color(0xFFFBEFD9), Amber)
                SuggestionStatus.APPROVED -> Triple("Đã duyệt", SuccessGreenSoft, SuccessGreen)
                SuggestionStatus.REJECTED -> Triple("Từ chối", BrickRedSoft, BrickRed)
            }
            Box(Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
        }
        Text(s.address, fontSize = 12.sp, color = InkSecondary, modifier = Modifier.padding(top = 2.dp))
        val price = s.price?.let { NumberFormat.getInstance(Locale("vi", "VN")).format(it) + "đ" }
        Text(
            listOfNotNull(price, s.openHours, s.createdAt).joinToString(" · "),
            fontSize = 12.sp, color = InkTertiary, modifier = Modifier.padding(top = 2.dp),
        )
        if (s.status == SuggestionStatus.REJECTED && !s.rejectReason.isNullOrBlank()) {
            Text(
                "Lý do từ chối: ${s.rejectReason}", fontSize = 12.sp, color = BrickRed,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(BrickRedSoft).padding(8.dp),
            )
        }
    }
}