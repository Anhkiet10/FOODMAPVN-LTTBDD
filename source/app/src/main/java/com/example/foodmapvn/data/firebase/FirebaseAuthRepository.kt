package com.example.foodmapvn.data.firebase

import com.example.foodmapvn.data.model.AuthUser
import com.example.foodmapvn.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    override suspend fun signIn(email: String, password: String): Result<Unit> = runAuth {
        auth.signInWithEmailAndPassword(email.trim(), password).awaitTask()
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> = runAuth {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).awaitTask().user
            ?: error("Không tạo được tài khoản")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).awaitTask()
        // Tạo hồ sơ trong Firestore: users/{uid}
        db.collection(FirestorePaths.USERS).document(user.uid).set(
            mapOf(
                "name" to name.trim(),
                "email" to user.email,
                "avatarUrl" to null,
                "role" to "user",
                "status" to "active",
                "localBadge" to null,
                "stats" to mapOf("checkIns" to 0, "reviews" to 0, "suggestions" to 0),
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).awaitTask()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runAuth {
        auth.sendPasswordResetEmail(email.trim()).awaitTask()
    }

    override fun signOut() = auth.signOut()

    /** Chạy thao tác và đổi lỗi Firebase sang câu tiếng Việt dễ hiểu. */
    private suspend fun runAuth(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: Exception) {
        val message = when (e) {
            is FirebaseAuthWeakPasswordException -> "Mật khẩu cần ít nhất 6 ký tự"
            is FirebaseAuthUserCollisionException -> "Email này đã được dùng cho tài khoản khác"
            is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị khóa"
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không đúng"
            is FirebaseNetworkException -> "Không có kết nối mạng"
            else -> e.message ?: "Đã có lỗi xảy ra"
        }
        Result.failure(Exception(message, e))
    }
}
