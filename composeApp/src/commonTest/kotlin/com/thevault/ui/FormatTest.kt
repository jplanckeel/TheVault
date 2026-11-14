package com.thevault.ui

import com.thevault.ui.component.formatEuro
import com.thevault.ui.component.formatEuroOrDash
import com.thevault.ui.component.formatSignedEuro
import com.thevault.ui.screen.additem.toAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatTest {
    private val nbsp = ' '

    @Test
    fun formatsEurosWithFrenchGrouping() {
        assertEquals("1${nbsp}234$nbsp€", formatEuro(1234.4))
        assertEquals("12,50$nbsp€", formatEuro(12.5, decimals = 2))
        assertEquals("-3$nbsp€", formatEuro(-3.0))
        assertEquals("0$nbsp€", formatEuro(-0.2))
        assertEquals("+5$nbsp€", formatSignedEuro(5.0))
        assertEquals("—", formatEuroOrDash(null))
    }

    @Test
    fun parsesAmountsWithCommaOrDot() {
        assertEquals(12.5, "12,50".toAmount())
        assertEquals(12.5, " 12.5 ".toAmount())
        assertEquals(1200.0, "1 200".toAmount())
        assertNull("abc".toAmount())
    }
}
