package com.github.stepwise.ui.profile

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ProfileFragmentTest {

    private fun callIsValidPassword(input: String): Boolean {
        val fragment = ProfileFragment()
        val m = fragment.javaClass.getDeclaredMethod("isValidPassword", String::class.java)
        m.isAccessible = true
        return m.invoke(fragment, input) as Boolean
    }

    private fun callIsValidEmail(input: String): Boolean {
        val fragment = ProfileFragment()
        val m = fragment.javaClass.getDeclaredMethod("isValidEmail", String::class.java)
        m.isAccessible = true
        return m.invoke(fragment, input) as Boolean
    }

    @Test
    fun `isValidPassword returns true for good passwords`() {
        assertTrue(callIsValidPassword("Qq@123456"))
        assertTrue(callIsValidPassword("Aa1@aaaa"))
        assertTrue(callIsValidPassword("StrongP@ssw0rd"))
    }

    @Test
    fun `isValidPassword returns false for bad passwords`() {
        assertFalse(callIsValidPassword("Qq@1"))
        assertFalse(callIsValidPassword("qq@123456"))
        assertFalse(callIsValidPassword("QQ@123456"))
        assertFalse(callIsValidPassword("Qq@abcdef"))
        assertFalse(callIsValidPassword("Qq12345678"))
    }

    @Test
    fun `isValidEmail recognizes valid emails`() {
        assertTrue(callIsValidEmail("user@example.com"))
        assertTrue(callIsValidEmail("user.name+tag@sub.domain.ru"))
        assertTrue(callIsValidEmail("a@b.co"))
    }

    @Test
    fun `isValidEmail rejects invalid emails`() {
        assertFalse(callIsValidEmail(""))
        assertFalse(callIsValidEmail("plainaddress"))
        assertFalse(callIsValidEmail("missing@domain"))
        assertFalse(callIsValidEmail("missing.domain@.com"))
    }

    @Test
    fun `phone regex used in saveProfile accepts valid numbers`() {
        val good = listOf("+71234567890", "81234567890")
        val bad = listOf("+7123456789", "+7123abcd890", "71234567890", "+712345678901")
        val pattern = Regex("^(\\+7|8)[0-9]{10}$")
        good.forEach { assertTrue("Should match $it", pattern.matches(it)) }
        bad.forEach { assertFalse("Should not match $it", pattern.matches(it)) }
    }
}