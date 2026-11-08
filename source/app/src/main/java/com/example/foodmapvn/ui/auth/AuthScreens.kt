package com.example.foodmapvn.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.foodmapvn.ui.theme.BrickRed
import com.example.foodmapvn.ui.theme.FieldBorder
import com.example.foodmapvn.ui.theme.Ink
import com.example.foodmapvn.ui.theme.InkSecondary
import com.example.foodmapvn.ui.theme.InkTertiary
import com.example.foodmapvn.ui.theme.Sand
import com.example.foodmapvn.ui.theme.SandDark
import com.example.foodmapvn.ui.theme.SuccessGreen
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
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.foodmapvn.R
import kotlinx.coroutines.launch
import com.example.foodmapvn.ui.theme.FoodmapvnTheme

// ---------- Route: nối ViewModel với màn hình ----------

@Composable
fun LoginRoute(
    onDone: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onPhoneClick: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val facebookEnabled = remember { isFacebookConfigured(context) }
    val facebookLogin = rememberFacebookLogin(
        onToken = viewModel::signInWithFacebookToken,
        onError = viewModel::onSocialError,
    )

    LoginScreen(
        onGoogleClick = {
            val activity = context.findActivity() ?: return@LoginScreen
            viewModel.onSocialStart()
            scope.launch {
                requestGoogleIdToken(activity).fold(viewModel::signInWithGoogleToken) { e ->
                    viewModel.onSocialError(e.message ?: "Đăng nhập Google lỗi")
                }
            }
        },
        onFacebookClick = {
            if (facebookEnabled) {
                viewModel.onSocialStart()
                facebookLogin()
            } else {
                viewModel.onSocialError("Chưa cấu hình Facebook: điền App ID và Client Token trong res/values/social_login.xml")
            }
        },
        onPhoneClick = onPhoneClick,
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    RegisterScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onRegisterClick = { confirm, accepted -> viewModel.register(confirm, accepted) },
        onBackToLogin = onBackToLogin,
        onGoogleClick = {
            val activity = context.findActivity() ?: return@RegisterScreen
            viewModel.onSocialStart()
            scope.launch {
                requestGoogleIdToken(activity).fold(viewModel::signInWithGoogleToken) { e ->
                    viewModel.onSocialError(e.message ?: "Đăng nhập Google lỗi")
                }
            }
        },
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

// ---------- Màn hình: chỉ hiển thị (theo Figma "Đăng nhập", "Đăng ký", "Quên mật khẩu") ----------

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onGoogleClick: () -> Unit = {},
    onFacebookClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    onBack: () -> Unit = onContinueAsGuest,
) {
    var rememberMe by remember { mutableStateOf(true) }
    AuthScaffold(onBack = onBack) {
        Image(
            painterResource(R.drawable.logo_mark),
            contentDescription = "FoodMap",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 76.dp, height = 72.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Chào mừng trở lại!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "Đăng nhập để check-in và lưu món muốn ăn",
            fontSize = 15.sp,
            color = InkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(8.dp))
        AuthField(state.email, onEmailChange, "Email", KeyboardType.Email)
        PasswordField(state.password, onPasswordChange, "Mật khẩu")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { rememberMe = !rememberMe },
            ) {
                CheckBox(checked = rememberMe)
                Spacer(Modifier.width(10.dp))
                Text("Ghi nhớ đăng nhập", fontSize = 15.sp, color = Ink)
            }
            Text(
                "Quên mật khẩu?",
                color = BrickRed,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onForgotPasswordClick)
                    .padding(4.dp),
            )
        }
        StatusText(state)
        PrimaryButton("Đăng nhập", state.isLoading, onLoginClick)

        OrDivider("hoặc đăng nhập bằng")
        SocialButton("Tiếp tục với Google", R.drawable.ic_google_logo, tinted = false, enabled = !state.isLoading, onClick = onGoogleClick)
        // Hai cách đăng nhập dưới đây Figma không có – app giữ lại (đã ghi vào báo cáo khác biệt)
        SocialButton("Tiếp tục với Facebook", R.drawable.ic_person, tinted = true, enabled = !state.isLoading, onClick = onFacebookClick)
        SocialButton("Đăng nhập bằng số điện thoại", R.drawable.ic_phone, tinted = true, enabled = !state.isLoading, onClick = onPhoneClick)

        LinkLine("Chưa có tài khoản? ", "Đăng ký", onRegisterClick)
        Spacer(Modifier.height(12.dp))
        GuestCard(onClick = onContinueAsGuest)
    }
}

@Composable
fun RegisterScreen(
    state: AuthUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRegisterClick: (confirmPassword: String, acceptedTerms: Boolean) -> Unit,
    onBackToLogin: () -> Unit,
    onGoogleClick: () -> Unit = {},
) {
    var confirm by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(true) }
    AuthScaffold(onBack = onBackToLogin) {
        Text("Tạo tài khoản", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Lưu món muốn ăn, check-in và nhận huy hiệu", fontSize = 15.sp, color = InkSecondary)
        Spacer(Modifier.height(8.dp))
        AuthField(state.name, onNameChange, "Họ và tên", KeyboardType.Text)
        AuthField(state.email, onEmailChange, "Email", KeyboardType.Email)
        PasswordField(state.password, onPasswordChange, "Mật khẩu (ít nhất 6 ký tự)")
        PasswordField(confirm, { confirm = it }, "Nhập lại mật khẩu")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { accepted = !accepted },
        ) {
            CheckBox(checked = accepted, checkedColor = SuccessGreen)
            Spacer(Modifier.width(10.dp))
            Text(
                buildAnnotatedString {
                    append("Tôi đồng ý với ")
                    withStyle(SpanStyle(color = BrickRed, fontWeight = FontWeight.Bold)) { append("Điều khoản") }
                    append(" và ")
                    withStyle(SpanStyle(color = BrickRed, fontWeight = FontWeight.Bold)) { append("Chính sách") }
                },
                fontSize = 15.sp,
                color = Ink,
            )
        }
        StatusText(state)
        PrimaryButton("Đăng ký", state.isLoading) { onRegisterClick(confirm, accepted) }
        OrDivider("hoặc đăng ký bằng")
        SocialButton("Tiếp tục với Google", R.drawable.ic_google_logo, tinted = false, enabled = !state.isLoading, onClick = onGoogleClick)
        LinkLine("Đã có tài khoản? ", "Đăng nhập", onBackToLogin)
    }
}

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBack: () -> Unit,
) {
    AuthScaffold(onBack = onBack, title = "Quên mật khẩu") {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(128.dp)
                .clip(CircleShape)
                .background(Sand),
            contentAlignment = Alignment.Center,
        ) {
            MailIcon()
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Nhập email bạn đã dùng để đăng ký.\nChúng tôi sẽ gửi liên kết đặt lại mật khẩu.",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = InkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        AuthField(state.email, onEmailChange, "Email của bạn", KeyboardType.Email)
        StatusText(state)
        PrimaryButton("Gửi liên kết", state.isLoading, onSendClick)
        Text(
            "Quay lại đăng nhập",
            color = BrickRed,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(8.dp),
        )
    }
}

// ---------- Thành phần dùng chung ----------

/** Phong bì đỏ gạch như Figma (vẽ bằng Canvas). */
@Composable
private fun MailIcon() {
    Canvas(Modifier.size(width = 66.dp, height = 48.dp)) {
        drawRoundRect(BrickRed, cornerRadius = CornerRadius(8.dp.toPx()))
        val stroke = 2.dp.toPx()
        drawLine(Color.White, Offset(4.dp.toPx(), 6.dp.toPx()), Offset(size.width / 2, size.height * 0.58f), stroke)
        drawLine(Color.White, Offset(size.width - 4.dp.toPx(), 6.dp.toPx()), Offset(size.width / 2, size.height * 0.58f), stroke)
    }
}

@Composable
private fun CheckBox(checked: Boolean, checkedColor: Color = BrickRed) {
    Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) checkedColor else Color.White)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, FieldBorder, RoundedCornerShape(6.dp))),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LinkLine(prefix: String, link: String, onClick: () -> Unit) {
    Text(
        buildAnnotatedString {
            append(prefix)
            withStyle(SpanStyle(color = BrickRed, fontWeight = FontWeight.Bold)) { append(link) }
        },
        fontSize = 15.sp,
        color = InkSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    )
}

@Composable
private fun GuestCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Sand)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Dùng thử không cần tài khoản", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
            Text("Xem quán, bản đồ, đánh giá — chưa check-in được", fontSize = 13.sp, color = InkSecondary)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = InkSecondary)
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
        Text("  $text  ", fontSize = 14.sp, color = InkSecondary)
        HorizontalDivider(Modifier.weight(1f), color = FieldBorder)
    }
}

@Composable
private fun SocialButton(text: String, icon: Int, tinted: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, FieldBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (tinted) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        else Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Khung chung: nút quay lại tròn nền cát (Figma), tiêu đề giữa tùy chọn, nội dung cuộn được. */
@Composable
private fun AuthScaffold(
    onBack: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SandDark)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Quay lại", tint = Ink, modifier = Modifier.size(22.dp))
            }
            if (title != null) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink, modifier = Modifier.align(Alignment.Center))
            }
        }
        content()
    }
}

@Composable
private fun AuthField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = InkTertiary) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = InkTertiary) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            // Nút "con mắt" hiện/ẩn mật khẩu (Figma)
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    contentDescription = if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = InkSecondary,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = FieldBorder,
    focusedBorderColor = BrickRed,
    cursorColor = BrickRed,
)

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
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrickRed),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun LoginScreenPreview() {
    FoodmapvnTheme {
        LoginScreen(AuthUiState(email = "demo@foodmap.vn"), {}, {}, {}, {}, {}, {})
    }
}
