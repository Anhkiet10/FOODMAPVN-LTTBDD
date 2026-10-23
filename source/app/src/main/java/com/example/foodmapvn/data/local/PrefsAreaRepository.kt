package com.example.foodmapvn.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.foodmapvn.data.model.Province
import com.example.foodmapvn.data.repository.AreaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lưu khu vực đang chọn trong SharedPreferences (chỉ lưu mã tỉnh, vd. "ho-chi-minh"). */
class PrefsAreaRepository(context: Context) : AreaRepository {

    private val prefs = context.getSharedPreferences("area", Context.MODE_PRIVATE)

    private val _selectedArea = MutableStateFlow(
        prefs.getString(KEY_PROVINCE_ID, null)?.let { id -> VietnamProvinces.all.firstOrNull { it.id == id } }
    )
    override val selectedArea: StateFlow<Province?> = _selectedArea.asStateFlow()

    override fun select(province: Province) {
        prefs.edit { putString(KEY_PROVINCE_ID, province.id) }
        _selectedArea.value = province
    }

    private companion object {
        const val KEY_PROVINCE_ID = "provinceId"
    }
}
