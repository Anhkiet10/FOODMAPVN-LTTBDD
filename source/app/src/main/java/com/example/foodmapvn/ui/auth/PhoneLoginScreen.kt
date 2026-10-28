package com.example.foodmapvn.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

@Composable
fun PhoneLoginRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneLoginViewModel = viewModel(factory = PhoneLoginViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    PhoneLoginScreen(
        state = state,
        onPhoneChange = viewModel::onPhoneChange,
        onCodeChange = viewModel::onCodeChange,
        onSendCode = { context.findActivity()?.let { viewModel.sendCode(it) } },
        onResend = { context.findActivity()?.let { viewModel.sendCode(it, resend = true) } },
        onVerify = viewModel::verifyCode,
        onChangeNumber = viewModel::changeNumber,
        onBack = onBack,
    )
}

@Composable
fun PhoneLoginScreen(
    state: PhoneLoginUiState,
    onPhoneChange: (String) -> Unit = {},
    onCodeChange: (String) -> Unit = {},
    onSendCode: () -> Unit = {},
    onResend: () -> Unit = {},
    onVerify: () -> Unit = {},
    onChangeNumber: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Food Map VN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Đăng nhập bằng số điện thoại", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (state.isCodeStep) "Nhập mã 6 số vừa gửi tới ${state.sentTo}"
            else "Chúng tôi sẽ gửi mã xác minh qua SMS. Lần đầu đăng nhập sẽ tự tạo tài khoản.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (!state.isCodeStep) {
            OutlinedTextField(
                value = state.phone,
                onValueChange = onPhoneChange,
                label = { Text("Số điện thoại") },
                placeholder = { Text("0912 345 678") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            ActionButton("Gửi mã", state.isLoading, onSendCode)
        } else {
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = { Text("Mã xác minh") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            ActionButton("Xác minh", state.isLoading, onVerify)
            TextButton(onClick = onResend, enabled = state.resendInSeconds == 0 && !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.resendInSeconds > 0) "Gửi lại mã sau ${state.resendInSeconds}s" else "Gửi lại mã")
            }
            TextButton(onClick = onChangeNumber, modifier = Modifier.fillMaxWidth()) { Text("Đổi số điện thoại") }
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại đăng nhập", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ActionButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PhoneLoginPreview() {
    FoodmapvnTheme { PhoneLoginScreen(PhoneLoginUiState(phone = "0912345678")) }
}
