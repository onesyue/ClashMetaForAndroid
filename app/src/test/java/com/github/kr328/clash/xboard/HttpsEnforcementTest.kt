package com.github.kr328.clash.xboard

import org.junit.Assert.*
import org.junit.Test

class HttpsEnforcementTest {

    @Test
    fun `https URL passes and is returned trimmed`() {
        val result = XBoardApi.requireHttps("https://example.com")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `https URL with trailing slash is trimmed`() {
        val result = XBoardApi.requireHttps("https://example.com/")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `https URL with multiple trailing slashes is trimmed`() {
        val result = XBoardApi.requireHttps("https://example.com///")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `https URL with path preserves path`() {
        val result = XBoardApi.requireHttps("https://example.com/api/v1/")
        assertEquals("https://example.com/api/v1", result)
    }

    @Test(expected = Exception::class)
    fun `http URL throws exception`() {
        XBoardApi.requireHttps("http://example.com")
    }

    @Test(expected = Exception::class)
    fun `HTTP uppercase URL throws exception`() {
        XBoardApi.requireHttps("HTTP://example.com")
    }

    @Test(expected = Exception::class)
    fun `Http mixed case URL throws exception`() {
        XBoardApi.requireHttps("Http://example.com")
    }

    @Test
    fun `http rejection message mentions HTTPS`() {
        try {
            XBoardApi.requireHttps("http://insecure.example.com")
            fail("Expected exception")
        } catch (e: Exception) {
            assertTrue(e.message!!.contains("HTTPS", ignoreCase = true))
        }
    }
}
