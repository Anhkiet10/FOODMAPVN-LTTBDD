package com.example.foodmapvn

import android.app.Application
import com.example.foodmapvn.di.AppContainer
import org.maplibre.android.MapLibre

class FoodMapApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // MapLibre phải được khởi tạo trước khi tạo MapView (bản đồ OpenFreeMap, không cần API key)
        MapLibre.getInstance(this)
    }
}
