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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

// ---------- Route: nối ViewModel với màn hình ----------

@Composable
fun LoginRoute(
    onDone: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::signIn,
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onContinueAsGuest = onDone,
    )
}

@Composable
fun RegisterRoute(
    onDone: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    RegisterScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRegisterClick = viewModel::register,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSendClick = viewModel::sendPasswordReset,
        onBack = onBack,
    )
}

// ---------- Màn hình: chỉ hiển thị ----------

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    AuthScaffold(title = "Đăng nhập", subtitle = "Tìm đặc sản đúng vị, quán đáng tin") {
        EmailField(state.email, onEmailChange)
        PasswordField(state.password, onPasswordChange)
        TextButton(onClick = onForgotPasswordClick, modifier = Modifier.align(Alignment.End)) {
            Text("Quên mật khẩu?")
        }
        StatusText(state)
        PrimaryButton("Đăng nhập", state.isLoading, onLoginClick)
        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text("Chưa có tài khoản? Đăng ký")
        }
        TextButton(onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth()) {
            Text("Dùng thử không cần tài khoản", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    AuthScaffold(title = "Tạo tài khoản", subtitle = "Lưu món muốn ăn, check-in và đánh giá quán") {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Họ tên") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        EmailField(state.email, onEmailChange)
        PasswordField(state.password, onPasswordChange)
        StatusText(state)
        PrimaryButton("Đăng ký", state.isLoading, onRegisterClick)
        TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Đã có tài khoản? Đăng nhập")
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBack: () -> Unit,
) {
    AuthScaffold(title = "Quên mật khẩu", subtitle = "Nhập email, chúng tôi sẽ gửi liên kết đặt lại mật khẩu") {
        EmailField(state.email, onEmailChange)
        StatusText(state)
        PrimaryButton("Gửi email", state.isLoading, onSendClick)
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại đăng nhập")
        }
    }
}

// ---------- Thành phần dùng chung ----------

@Composable
private fun AuthScaffold(
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
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
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Mật khẩu") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusText(state: AuthUiState) {
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    state.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun PrimaryButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
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
private fun LoginScreenPreview() {
    FoodmapvnTheme {
        LoginScreen(AuthUiState(email = "demo@foodmap.vn"), {}, {}, {}, {}, {}, {})
    }
}
