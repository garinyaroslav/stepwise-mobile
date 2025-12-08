package com.github.stepwise.ui.login

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class LoginFragmentTest {

    private fun callPrivateIsValidPassword(input: String): Boolean {
        val fragment = LoginFragment()
        val m = fragment.javaClass.getDeclaredMethod("isValidPassword", String::class.java)
        m.isAccessible = true
        return m.invoke(fragment, input) as Boolean
    }

    private fun callPrivateIsValidEmail(input: String): Boolean {
        val fragment = LoginFragment()
        val m = fragment.javaClass.getDeclaredMethod("isValidEmail", String::class.java)
        m.isAccessible = true
        return m.invoke(fragment, input) as Boolean
    }

    @Test
    fun `isValidPassword returns true for valid password`() {
        assertTrue(callPrivateIsValidPassword("Qq@123456"))
        assertTrue(callPrivateIsValidPassword("Aa1@aaaa"))
        assertTrue(callPrivateIsValidPassword("StrongP@ssw0rd"))
    }

    @Test
    fun `isValidPassword returns false for invalid passwords`() {
        assertFalse(callPrivateIsValidPassword("Qq@1"))
        assertFalse(callPrivateIsValidPassword("qq@123456"))
        assertFalse(callPrivateIsValidPassword("QQ@123456"))
        assertFalse(callPrivateIsValidPassword("Qq@abcdef"))
        assertFalse(callPrivateIsValidPassword("Qq12345678"))
    }

    @Test
    fun `isValidEmail recognizes valid emails`() {
        assertTrue(callPrivateIsValidEmail("user@example.com"))
        assertTrue(callPrivateIsValidEmail("user.name+tag@sub.domain.ru"))
        assertTrue(callPrivateIsValidEmail("a@b.co"))
    }

    @Test
    fun `isValidEmail rejects invalid emails`() {
        assertFalse(callPrivateIsValidEmail(""))
        assertFalse(callPrivateIsValidEmail("plainaddress"))
        assertFalse(callPrivateIsValidEmail("missing@domain"))
        assertFalse(callPrivateIsValidEmail("missing.domain@.com"))
    }
}