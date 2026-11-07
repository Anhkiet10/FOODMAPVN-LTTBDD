package com.example.foodmapvn.ui.business

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.R
import com.example.foodmapvn.data.model.Business
import com.example.foodmapvn.data.model.BusinessStatus
import com.example.foodmapvn.data.model.BusinessType
import com.example.foodmapvn.data.repository.BusinessRepository
import com.example.foodmapvn.ui.theme.Amber
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.BrickRedSoft
import com.example.foodmapvn.ui.theme.FoodmapvnTheme
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.SuccessGreen
import com.example.foodmapvn.ui.theme.SuccessGreenSoft

/** Route "business_register" — mở từ Hồ sơ > Đăng ký doanh nghiệp. */
@Composable
fun BusinessRegisterRoute(
    onBack: () -> Unit,
    onNewSuggestionClick: () -> Unit,
    viewModel: BusinessRegisterViewModel = viewModel(factory = BusinessRegisterViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.justSubmitted) {
        if (state.justSubmitted) {
            android.widget.Toast.makeText(context, "Đã gửi đăng ký, Admin sẽ xem xét", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.onSubmittedShown()
        }
    }
    BusinessRegisterScreen(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onTypeChange = viewModel::onTypeChange,
        onSubmit = viewModel::submit,
        onEdit = viewModel::startEditing,
        onCancelEdit = viewModel::cancelEditing,
        onNewSuggestionClick = onNewSuggestionClick,
    )
}

@Composable
fun BusinessRegisterScreen(
    state: BusinessRegisterUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onTypeChange: (BusinessType) -> Unit,
    onSubmit: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onNewSuggestionClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            IconButton(onClick = onBack, enabled = !state.isSubmitting) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại")
            }
            Text("Doanh nghiệp của tôi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }

        if (!state.isLoaded) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Chỉ chủ quán đã đăng ký doanh nghiệp và được Admin xác minh mới được đề xuất quán lên bản đồ. " +
                    "Việc này giúp thông tin quán đáng tin cậy và tránh tài khoản giả mạo.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
            )

            state.business?.let { StatusCard(it, onNewSuggestionClick) }

            if (state.isEditing) {
                RegisterForm(state, onNameChange, onTypeChange, onSubmit, onCancelEdit)
            } else if (state.business?.status == BusinessStatus.PENDING) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Sửa thông tin đăng ký") }
            }
        }
    }
}

@Composable
private fun StatusCard(business: Business, onNewSuggestionClick: () -> Unit) {
    val (bg, fg, icon) = when (business.status) {
        BusinessStatus.APPROVED -> Triple(SuccessGreenSoft, SuccessGreen, R.drawable.ic_shield_check)
        BusinessStatus.PENDING -> Triple(Color(0xFFFBF1E1), Amber, R.drawable.ic_shield)
        BusinessStatus.REJECTED -> Triple(BrickRedSoft, BrickRed, R.drawable.ic_close)
    }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().background(bg, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(business.status.label, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.SemiBold)
        }
        Text(business.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(business.type.label + (business.createdAt?.let { " · gửi ngày $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        when (business.status) {
            BusinessStatus.PENDING -> Text("Admin sẽ xem xét đăng ký của bạn. Khi được duyệt, bạn sẽ đề xuất được quán.", style = MaterialTheme.typography.bodySmall)
            BusinessStatus.REJECTED -> Text("Lý do: ${business.rejectReason ?: "không ghi"}. Sửa thông tin bên dưới rồi gửi lại.", style = MaterialTheme.typography.bodySmall)
            BusinessStatus.APPROVED -> {
                Text("Bạn có thể đề xuất quán của mình lên bản đồ.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onNewSuggestionClick, modifier = Modifier.fillMaxWidth()) { Text("Đề xuất quán mới") }
            }
        }
    }
}

@Composable
private fun RegisterForm(
    state: BusinessRegisterUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (BusinessType) -> Unit,
    onSubmit: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (state.business == null) "Thông tin đăng ký" else "Sửa thông tin",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Tên doanh nghiệp / hộ kinh doanh") },
            placeholder = { Text("vd. Hộ kinh doanh Bún bò Cô Lan") },
            singleLine = true,
            enabled = !state.isSubmitting,
            isError = state.error != null,
            supportingText = { Text(state.error ?: "${state.name.trim().length}/${BusinessRepository.NAME_MAX}") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Loại hình", style = MaterialTheme.typography.labelLarge)
        BusinessType.entries.forEach { type ->
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (state.type == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
                    .selectable(selected = state.type == type, enabled = !state.isSubmitting, role = Role.RadioButton, onClick = { onTypeChange(type) })
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.type == type, onClick = null)
                Spacer(Modifier.width(8.dp))
                Text(type.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (state.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (state.business == null) "Gửi đăng ký" else "Gửi lại để duyệt")
        }
        if (state.business?.status == BusinessStatus.PENDING) {
            TextButton(onClick = onCancelEdit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) { Text("Huỷ") }
        }
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 760)
@Composable
private fun BusinessRegisterNewPreview() {
    FoodmapvnTheme {
        BusinessRegisterScreen(
            state = BusinessRegisterUiState(isLoaded = true, isEditing = true, name = "Hộ kinh doanh Bún bò Cô Lan"),
            onBack = {}, onNameChange = {}, onTypeChange = {}, onSubmit = {}, onEdit = {}, onCancelEdit = {}, onNewSuggestionClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 600)
@Composable
private fun BusinessRegisterApprovedPreview() {
    FoodmapvnTheme {
        BusinessRegisterScreen(
            state = BusinessRegisterUiState(
                isLoaded = true,
                business = Business("u1", "Hộ kinh doanh Bún bò Cô Lan", BusinessType.HOUSEHOLD, BusinessStatus.APPROVED, createdAt = "05/11/2026"),
            ),
            onBack = {}, onNameChange = {}, onTypeChange = {}, onSubmit = {}, onEdit = {}, onCancelEdit = {}, onNewSuggestionClick = {},
        )
    }
}
