package com.example.foodmapvn.data.firebase

import android.app.Activity
import android.util.Log
import com.example.foodmapvn.data.model.AuthUser
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.PhoneCodeSent
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.let { AuthUser(it.uid, it.email, it.displayName) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val isAdmin: Flow<Boolean> = currentUser
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) flowOf(false)
            else db.collection(FirestorePaths.USERS).document(uid).asFlow()
                .map { it.getString("role") == "admin" }
                .catch { emit(false) }
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runAuth {
        auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> = runAuth {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask().user
            ?: error("Không tạo được tài khoản")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).awaitTask()
        createProfile(user, name = name.trim(), provider = "password")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runAuth {
        auth.sendPasswordResetEmail(email.trim()).awaitTask()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))

    override suspend fun signInWithFacebook(accessToken: String): Result<Unit> =
        signInWithCredential(FacebookAuthProvider.getCredential(accessToken))

    override suspend fun sendPhoneCode(activity: Activity, phoneNumber: String, resendToken: Any?): Result<PhoneCodeSent> =
        try {
            val sent = suspendCancellableCoroutine<PhoneCodeSent> { cont ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    // Firebase tự xác minh được (tự đọc SMS / số thử nghiệm) -> đăng nhập luôn
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        auth.signInWithCredential(credential).addOnCompleteListener { task ->
                            if (!cont.isActive) return@addOnCompleteListener
                            if (task.isSuccessful) cont.resume(PhoneCodeSent(null, autoSignedIn = true, resendToken = null))
                            else cont.resumeWithException(task.exception ?: IllegalStateException("Đăng nhập thất bại"))
                        }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        if (cont.isActive) cont.resume(PhoneCodeSent(verificationId, autoSignedIn = false, resendToken = token))
                    }
                }
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                (resendToken as? PhoneAuthProvider.ForceResendingToken)?.let(options::setForceResendingToken)
                PhoneAuthProvider.verifyPhoneNumber(options.build())
            }
            if (sent.autoSignedIn) ensureProfileOrThrow()
            Result.success(sent)
        } catch (e: Exception) {
            Log.w("FoodMapAuth", "Phone auth lỗi: ${e.message}", e)
            Result.failure(Exception(authErrorMessage(e), e))
        }

    override suspend fun verifyPhoneCode(verificationId: String, code: String): Result<Unit> =
        signInWithCredential(PhoneAuthProvider.getCredential(verificationId, code.trim()))

    override suspend fun ensureProfile(): Result<Unit> = runAuth { ensureProfileOrThrow() }

    override fun signOut() = auth.signOut()

    // ---------- nội bộ ----------

    private suspend fun signInWithCredential(credential: AuthCredential): Result<Unit> = runAuth {
        auth.signInWithCredential(credential).awaitTask()
        ensureProfileOrThrow()
    }

    /** Lần đầu đăng nhập bằng Google/Facebook/SĐT: tạo hồ sơ users/{uid}. */
    private suspend fun ensureProfileOrThrow() {
        val user = auth.currentUser ?: return
        val ref = db.collection(FirestorePaths.USERS).document(user.uid)
        val doc = ref.get().awaitTask()
        if (doc.exists()) {
            // Hồ sơ cũ còn tên chung "Người dùng …" (bản trước) -> đổi sang tên tự sinh không trùng
            val old = doc.getString("name").orEmpty()
            if (old.isBlank() || old.startsWith("Người dùng")) ref.update("name", reserveUsername(user.uid)).awaitTask()
            return
        }
        val provider = user.providerData.map { it.providerId }.firstOrNull { it != "firebase" } ?: "unknown"
        val name = user.displayName?.takeIf { it.isNotBlank() } ?: reserveUsername(user.uid)
        createProfile(user, name = name, provider = provider)
    }

    /**
     * Tên tự sinh cho tài khoản không có tên (đăng nhập bằng SĐT, Facebook không chia sẻ tên), vd. "afad123".
     * Không trùng trong CSDL: giữ chỗ bằng document usernames/{tên} trong một transaction —
     * tên đã có người giữ thì sinh tên khác (Firestore không có UNIQUE như SQL nên phải tự làm).
     */
    private suspend fun reserveUsername(uid: String): String {
        repeat(5) {
            val candidate = UsernameGenerator.next()
            val ref = db.collection(FirestorePaths.USERNAMES).document(candidate)
            val ok = db.runTransaction { tx ->
                if (tx.get(ref).exists()) false
                else {
                    tx.set(ref, mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()))
                    true
                }
            }.awaitTask()
            if (ok == true) return candidate
        }
        // Rất hiếm khi 5 lần đều trùng: dùng một phần uid (vốn đã duy nhất)
        return "user" + uid.take(8).lowercase()
    }

    private suspend fun createProfile(user: FirebaseUser, name: String, provider: String) {
        db.collection(FirestorePaths.USERS).document(user.uid).set(
            mapOf(
                "name" to name,
                "email" to user.email,                 // có thể null (đăng nhập bằng SĐT, Facebook không chia sẻ email)
                "phone" to user.phoneNumber,           // chỉ có khi đăng nhập bằng SĐT
                "authProvider" to provider,            // password | google.com | facebook.com | phone
                "avatarUrl" to user.photoUrl?.toString(),
                "role" to "user",
                "status" to "active",
                "localBadge" to null,
                "stats" to mapOf("checkIns" to 0, "reviews" to 0, "suggestions" to 0),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).awaitTask()
    }

    /** Chạy thao tác và đổi lỗi Firebase sang câu tiếng Việt dễ hiểu (AuthErrors.kt). */
    private suspend fun runAuth(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: Exception) {
        // Ghi lỗi gốc ra Logcat (lọc "FoodMapAuth") để biết chính xác Firebase từ chối vì sao
        Log.w("FoodMapAuth", "Auth lỗi: ${(e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode} ${e.message}", e)
        Result.failure(Exception(authErrorMessage(e), e))
    }
}
