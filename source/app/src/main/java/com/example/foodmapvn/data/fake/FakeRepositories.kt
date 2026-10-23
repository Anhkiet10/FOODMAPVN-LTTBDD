package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.DishRepository
import com.example.foodmapvn.data.repository.SavedRepository
import com.example.foodmapvn.data.repository.UserRepository

/**
 * Bộ repository giả, được AppContainer dùng khi chưa cấu hình Firebase
 * (chưa có app/google-services.json).
 */
class FakeRepositories(areaRepository: AreaRepository) {
    val authRepository: AuthRepository = FakeAuthRepository()
    val dishRepository: DishRepository = FakeDishRepository(areaRepository)
    val savedRepository: SavedRepository = FakeSavedRepository()
    val userRepository: UserRepository = FakeUserRepository()
}
