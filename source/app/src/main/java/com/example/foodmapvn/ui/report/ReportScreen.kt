package com.example.foodmapvn.ui.report

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.data.repository.ReportReason
import com.example.foodmapvn.data.repository.ReportRepository
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.SandDark
import kotlinx.coroutines.launch

private const val NOTE_MAX = 300

/**
 * UC-15 Báo cáo vi phạm. Route: report/{targetType}/{targetId} (targetType = "PLACE" | "REVIEW").
 * Người 2 mở màn này bằng navController.navigate(Routes.report("PLACE", placeId)).
 */
@Composable
fun ReportRoute(
    reportRepository: ReportRepository,
    targetType: String,
    targetId: String,
    onBack: () -> Unit,
) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    var note by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val title = if (targetType == "REVIEW") "Báo cáo đánh giá" else "Báo cáo quán"

    Scaffold(containerColor = Color.White) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(SandDark).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Text("←", fontSize = 18.sp, color = Ink) }
                Spacer(Modifier.width(10.dp))
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.height(16.dp))
            Text("Lý do báo cáo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(4.dp))
            ReportReason.entries.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { reason = r }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == r, onClick = { reason = r })
                    Text(r.label, fontSize = 14.sp, color = Ink)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note, onValueChange = { if (it.length <= NOTE_MAX) note = it },
                label = { Text("Mô tả thêm (không bắt buộc)") },
                supportingText = { Text("${note.length}/$NOTE_MAX") },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) Text(error!!, color = BrickRed, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val r = reason ?: return@Button
                    sending = true; error = null
                    scope.launch {
                        reportRepository.report(targetType, targetId, r, note.trim())
                            .onSuccess { sent = true }
                            .onFailure { error = it.message ?: "Không gửi được báo cáo" }
                        sending = false
                    }
                },
                enabled = reason != null && !sending,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
            ) { Text(if (sending) "Đang gửi..." else "Gửi báo cáo") }
            Spacer(Modifier.height(8.dp))
            Text(
                "Báo cáo được gửi cho quản trị viên xem xét. Bạn cần đăng nhập để gửi.",
                fontSize = 12.sp, color = InkSecondary,
            )
        }
    }
    if (sent) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Đã gửi báo cáo") },
            text = { Text("Cảm ơn bạn. Quản trị viên sẽ xem xét sớm.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Đóng") } },
        )
    }
}