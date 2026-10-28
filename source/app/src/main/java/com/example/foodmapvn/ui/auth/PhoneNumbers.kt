package com.example.foodmapvn.ui.auth

/** Chuẩn hoá số điện thoại Việt Nam sang dạng quốc tế E.164 mà Firebase yêu cầu. */
object PhoneNumbers {
    /** "0912 345 678" | "84912345678" | "+84912345678" -> "+84912345678"; sai -> null. Số nước khác: nhập kèm "+". */
    fun toE164(input: String): String? {
        val s = input.replace(" ", "").replace(".", "").replace("-", "")
        val e164 = when {
            s.startsWith("+") -> s
            s.startsWith("84") -> "+$s"
            s.startsWith("0") -> "+84" + s.drop(1)
            else -> return null
        }
        return when {
            e164.startsWith("+84") -> e164.takeIf { Regex("^\\+84[35789]\\d{8}$").matches(it) }
            else -> e164.takeIf { Regex("^\\+\\d{8,15}$").matches(it) }
        }
    }
}
