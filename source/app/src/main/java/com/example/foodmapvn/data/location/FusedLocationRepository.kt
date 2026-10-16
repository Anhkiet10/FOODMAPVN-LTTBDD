package com.example.foodmapvn.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.foodmapvn.data.firebase.awaitTask
import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class FusedLocationRepository(private val context: Context) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // đã kiểm tra bằng hasPermission()
    override suspend fun currentLocation(): LatLng? {
        if (!hasPermission()) return null
        return runCatching {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .awaitTask()
                ?.let { LatLng(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
