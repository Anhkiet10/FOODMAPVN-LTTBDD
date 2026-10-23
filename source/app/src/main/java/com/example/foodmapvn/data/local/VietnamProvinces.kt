package com.example.foodmapvn.data.local

import com.example.foodmapvn.data.model.LatLng
import com.example.foodmapvn.data.model.Province

/**
 * 34 tỉnh, thành phố của Việt Nam từ 01/07/2025 (Nghị quyết 202/2025/QH15).
 * Tọa độ là trung tâm hành chính mới (tương đối), dùng để chuyển bản đồ.
 * Dữ liệu tĩnh trong app; sau này có thể chuyển lên collection regions trên Firestore.
 */
object VietnamProvinces {

    /** Mã tỉnh mặc định khi chưa chọn vùng (có dữ liệu mẫu). */
    const val DEFAULT_ID = "hue"

    val all: List<Province> = listOf(
        // 6 thành phố trực thuộc trung ương
        Province("ha-noi", "Hà Nội", true, LatLng(21.0285, 105.8542)),
        Province("hue", "Huế", true, LatLng(16.4637, 107.5909), listOf("Thừa Thiên Huế")),
        Province("hai-phong", "Hải Phòng", true, LatLng(20.8449, 106.6881), listOf("Hải Dương")),
        Province("da-nang", "Đà Nẵng", true, LatLng(16.0544, 108.2022), listOf("Quảng Nam")),
        Province("ho-chi-minh", "TP. Hồ Chí Minh", true, LatLng(10.7769, 106.7009), listOf("Sài Gòn", "Bình Dương", "Bà Rịa - Vũng Tàu")),
        Province("can-tho", "Cần Thơ", true, LatLng(10.0452, 105.7469), listOf("Sóc Trăng", "Hậu Giang")),

        // 28 tỉnh
        Province("an-giang", "An Giang", false, LatLng(10.0125, 105.0809), listOf("Kiên Giang")),
        Province("bac-ninh", "Bắc Ninh", false, LatLng(21.2731, 106.1946), listOf("Bắc Giang")),
        Province("ca-mau", "Cà Mau", false, LatLng(9.1769, 105.1524), listOf("Bạc Liêu")),
        Province("cao-bang", "Cao Bằng", false, LatLng(22.6657, 106.2570)),
        Province("dak-lak", "Đắk Lắk", false, LatLng(12.6667, 108.0500), listOf("Phú Yên")),
        Province("dien-bien", "Điện Biên", false, LatLng(21.3860, 103.0230)),
        Province("dong-nai", "Đồng Nai", false, LatLng(10.9574, 106.8427), listOf("Bình Phước")),
        Province("dong-thap", "Đồng Tháp", false, LatLng(10.3600, 106.3600), listOf("Tiền Giang")),
        Province("gia-lai", "Gia Lai", false, LatLng(13.7820, 109.2196), listOf("Bình Định")),
        Province("ha-tinh", "Hà Tĩnh", false, LatLng(18.3428, 105.9057)),
        Province("hung-yen", "Hưng Yên", false, LatLng(20.6464, 106.0511), listOf("Thái Bình")),
        Province("khanh-hoa", "Khánh Hòa", false, LatLng(12.2388, 109.1967), listOf("Ninh Thuận")),
        Province("lai-chau", "Lai Châu", false, LatLng(22.3964, 103.4582)),
        Province("lam-dong", "Lâm Đồng", false, LatLng(11.9404, 108.4583), listOf("Đắk Nông", "Bình Thuận")),
        Province("lang-son", "Lạng Sơn", false, LatLng(21.8537, 106.7610)),
        Province("lao-cai", "Lào Cai", false, LatLng(21.7229, 104.9113), listOf("Yên Bái")),
        Province("nghe-an", "Nghệ An", false, LatLng(18.6796, 105.6813)),
        Province("ninh-binh", "Ninh Bình", false, LatLng(20.2506, 105.9745), listOf("Hà Nam", "Nam Định")),
        Province("phu-tho", "Phú Thọ", false, LatLng(21.3227, 105.4020), listOf("Vĩnh Phúc", "Hòa Bình")),
        Province("quang-ngai", "Quảng Ngãi", false, LatLng(15.1205, 108.7923), listOf("Kon Tum")),
        Province("quang-ninh", "Quảng Ninh", false, LatLng(20.9590, 107.0448)),
        Province("quang-tri", "Quảng Trị", false, LatLng(17.4689, 106.6223), listOf("Quảng Bình")),
        Province("son-la", "Sơn La", false, LatLng(21.3256, 103.9188)),
        Province("tay-ninh", "Tây Ninh", false, LatLng(10.5359, 106.4137), listOf("Long An")),
        Province("thai-nguyen", "Thái Nguyên", false, LatLng(21.5942, 105.8482), listOf("Bắc Kạn")),
        Province("thanh-hoa", "Thanh Hóa", false, LatLng(19.8067, 105.7852)),
        Province("tuyen-quang", "Tuyên Quang", false, LatLng(21.8236, 105.2180), listOf("Hà Giang")),
        Province("vinh-long", "Vĩnh Long", false, LatLng(10.2537, 105.9722), listOf("Bến Tre", "Trà Vinh")),
    )

    val DEFAULT: Province = all.first { it.id == DEFAULT_ID }

    fun byId(id: String): Province? = all.firstOrNull { it.id == id }
}
