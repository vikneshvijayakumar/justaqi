package me.viknesh.justaqi.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SunscreenTest {

    @Test
    fun uv_levels() {
        assertEquals(UvLevel.LOW, UvLevel.of(0.0))
        assertEquals(UvLevel.LOW, UvLevel.of(2.9))
        assertEquals(UvLevel.MODERATE, UvLevel.of(3.0))
        assertEquals(UvLevel.MODERATE, UvLevel.of(5.9))
        assertEquals(UvLevel.HIGH, UvLevel.of(6.0))
        assertEquals(UvLevel.VERY_HIGH, UvLevel.of(8.0))
        assertEquals(UvLevel.EXTREME, UvLevel.of(11.5))
        assertEquals(UvLevel.LOW, UvLevel.of(-1.0))
    }

    @Test
    fun advice_matches_level() {
        assertEquals(UvLevel.HIGH, Sunscreen.advise(7.0).level)
    }

    @Test
    fun protection_window() {
        val hours = (0..23).toList()
        val uv = hours.map { if (it in 9..15) 6.0 else 0.5 }
        assertEquals(9..15, Sunscreen.protectionWindow(hours, uv))
    }

    @Test
    fun protection_window_none_when_uv_low() {
        val hours = (0..23).toList()
        assertNull(Sunscreen.protectionWindow(hours, hours.map { 2.0 }))
    }

    @Test
    fun protection_window_ignores_missing_values() {
        val hours = listOf(8, 9, 10)
        assertEquals(10..10, Sunscreen.protectionWindow(hours, listOf(null, null, 4.0)))
    }
}
