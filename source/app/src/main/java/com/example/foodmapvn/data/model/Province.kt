package com.example.foodmapvn.data.model

/** Tỉnh/thành phố (sau sắp xếp đơn vị hành chính 2025: 34 đơn vị). */
data class Province(
    val id: String,
    val name: String,
    /** true = thành phố trực thuộc trung ương. */
    val isCity: Boolean,
    /** Tọa độ trung tâm hành chính — bản đồ sẽ chuyển tới đây khi chọn. */
    val center: LatLng,
    /** Các tỉnh/thành cũ được sáp nhập vào (để tìm kiếm theo tên cũ). */
    val formerNames: List<String> = emptyList(),
)
