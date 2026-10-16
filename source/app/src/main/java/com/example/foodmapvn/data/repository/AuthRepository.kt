package com.example.foodmapvn.data.repository

import com.example.foodmapvn.data.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** null = chưa đăng nhập (Khách). */
    val currentUser: Flow<AuthUser?>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()
}
