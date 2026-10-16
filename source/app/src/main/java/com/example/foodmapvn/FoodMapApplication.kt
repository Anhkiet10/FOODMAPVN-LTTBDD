package com.example.foodmapvn

import android.app.Application
import com.example.foodmapvn.di.AppContainer
import org.osmdroid.config.Configuration

class FoodMapApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // osmdroid yêu cầu User-Agent riêng khi tải bản đồ OpenStreetMap
        Configuration.getInstance().userAgentValue = packageName
    }
}
