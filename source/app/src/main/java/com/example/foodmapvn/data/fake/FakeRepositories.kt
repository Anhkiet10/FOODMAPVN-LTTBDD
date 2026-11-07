package com.example.foodmapvn.data.fake

import com.example.foodmapvn.data.repository.AreaRepository
import com.example.foodmapvn.data.repository.AuthRepository
import com.example.foodmapvn.data.repository.BusinessRepository
import com.example.foodmapvn.data.repository.DishDetailRepository
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
    // Người 2
    val dishDetailRepository: DishDetailRepository = FakeDishDetailRepository(dishRepository)
    val userRepository: UserRepository = FakeUserRepository()
    val businessRepository: BusinessRepository = FakeBusinessRepository()
}
