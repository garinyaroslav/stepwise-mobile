package com.github.stepwise.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import formatIsoToDdMmYyyy

class DateUtilsTest {

    @Test
    fun `null or blank returns empty string`() {
        assertEquals("", formatIsoToDdMmYyyy(null))
        assertEquals("", formatIsoToDdMmYyyy(""))
        assertEquals("", formatIsoToDdMmYyyy("   "))
    }

    @Test
    fun `date only ISO formatted string returns ddMMyyyy`() {
        val iso = "2025-12-08"
        val expected = "08.12.2025"
        assertEquals(expected, formatIsoToDdMmYyyy(iso))
    }

    @Test
    fun `local datetime ISO returns ddMMyyyy`() {
        val iso = "2025-12-08T14:30:00"
        val expected = "08.12.2025"
        assertEquals(expected, formatIsoToDdMmYyyy(iso))
    }

    @Test
    fun `local datetime with fractional seconds returns ddMMyyyy`() {
        val iso = "2025-12-08T14:30:00.123"
        val expected = "08.12.2025"
        assertEquals(expected, formatIsoToDdMmYyyy(iso))
    }

    @Test
    fun `offset datetime with Z returns ddMMyyyy`() {
        val iso = "2025-12-08T14:30:00Z"
        val expected = "08.12.2025"
        assertEquals(expected, formatIsoToDdMmYyyy(iso))
    }

    @Test
    fun `offset datetime with plus offset returns ddMMyyyy`() {
        val iso = "2025-12-08T14:30:00+03:00"
        val expected = "08.12.2025"
        assertEquals(expected, formatIsoToDdMmYyyy(iso))
    }

    @Test
    fun `invalid format returns original string`() {
        val bad = "not-a-date"
        assertEquals(bad, formatIsoToDdMmYyyy(bad))
    }
}