package com.example.foodmapvn.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    @Test fun vietnameseFormats() {
        assertEquals("+84912345678", PhoneNumbers.toE164("0912 345 678"))
        assertEquals("+84912345678", PhoneNumbers.toE164("84912345678"))
        assertEquals("+84912345678", PhoneNumbers.toE164("+84 912.345.678"))
        assertEquals("+84387654321", PhoneNumbers.toE164("0387-654-321"))
    }

    @Test fun invalid() {
        assertNull(PhoneNumbers.toE164("12345"))
        assertNull(PhoneNumbers.toE164("0212345678"))   // đầu số không phải di động
        assertNull(PhoneNumbers.toE164("091234567"))    // thiếu số
    }

    @Test fun foreignWithPlus() {
        assertEquals("+16505553434", PhoneNumbers.toE164("+1 650 555 3434"))
    }
}
