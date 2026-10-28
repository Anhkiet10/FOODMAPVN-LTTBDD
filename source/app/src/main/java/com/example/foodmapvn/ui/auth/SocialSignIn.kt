package com.example.foodmapvn.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.foodmapvn.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Tìm Activity từ Context của Compose (cần cho Google, Facebook, SĐT). */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ---------------- Google (Credential Manager) ----------------

/**
 * Mở hộp chọn tài khoản Google, trả về ID token để đưa cho Firebase.
 * Web client ID ("default_web_client_id") do plugin google-services tự sinh từ google-services.json
 * khi đã bật Google trong Firebase Console và đã thêm SHA-1 — chưa có thì báo hướng dẫn.
 */
suspend fun requestGoogleIdToken(activity: Activity): Result<String> {
    @Suppress("DiscouragedApi")
    val resId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
    if (resId == 0) {
        return Result.failure(
            IllegalStateException(
                "Chưa cấu hình đăng nhập Google: bật Google trong Firebase Console, thêm SHA-1, rồi tải lại google-services.json"
            )
        )
    }
    val option = GetSignInWithGoogleOption.Builder(activity.getString(resId)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            Result.failure(IllegalStateException("Không nhận được tài khoản Google"))
        }
    } catch (e: GetCredentialCancellationException) {
        Result.failure(IllegalStateException("Bạn đã huỷ đăng nhập Google"))
    } catch (e: NoCredentialException) {
        Result.failure(IllegalStateException("Máy chưa có tài khoản Google. Thêm tài khoản trong Cài đặt của máy rồi thử lại."))
    } catch (e: GetCredentialException) {
        Result.failure(IllegalStateException("Đăng nhập Google lỗi: ${e.message}"))
    }
}

// ---------------- Facebook ----------------

/** Đã điền App ID / Client Token trong res/values/social_login.xml chưa. */
fun isFacebookConfigured(context: Context): Boolean =
    !context.getString(R.string.facebook_app_id).startsWith("YOUR_") &&
        !context.getString(R.string.facebook_client_token).startsWith("YOUR_")

/**
 * Trả về hàm bấm nút "Tiếp tục với Facebook".
 * Thành công -> [onToken] (access token đưa cho Firebase); huỷ / lỗi -> [onError].
 */
@Composable
fun rememberFacebookLogin(onToken: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val currentOnToken by rememberUpdatedState(onToken)
    val currentOnError by rememberUpdatedState(onError)
    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = remember { LoginManager.getInstance() }
    val launcher = rememberLauncherForActivityResult(
        loginManager.createLogInActivityResultContract(callbackManager, null)
    ) { /* kết quả đi qua callback bên dưới */ }

    DisposableEffect(callbackManager) {
        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) = currentOnToken(result.accessToken.token)
            override fun onCancel() = currentOnError("Bạn đã huỷ đăng nhập Facebook")
            override fun onError(error: FacebookException) = currentOnError("Đăng nhập Facebook lỗi: ${error.message}")
        })
        onDispose { loginManager.unregisterCallback(callbackManager) }
    }
    return { launcher.launch(listOf("email", "public_profile")) }
}
