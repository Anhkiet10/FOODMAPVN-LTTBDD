package com.example.foodmapvn.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController

/**
 * Mở màn theo route; nếu màn đó chưa được đăng ký trong NavHost (người phụ trách chưa làm xong)
 * thì báo nhẹ thay vì crash. Dùng khi mở màn của người khác, vd. Routes.dish(id) của Người 2.
 */
fun NavController.navigateIfReady(route: String, context: Context, screenName: String) {
    try {
        navigate(route)
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, "Màn $screenName đang được làm, chưa mở được", Toast.LENGTH_SHORT).show()
    }
}
