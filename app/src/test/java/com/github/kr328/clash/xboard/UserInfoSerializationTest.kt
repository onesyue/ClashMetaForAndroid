package com.github.kr328.clash.xboard

import org.junit.Assert.*
import org.junit.Test

class UserInfoSerializationTest {

    @Test
    fun `roundtrip with all fields populated`() {
        val original = XBoardApi.UserInfo(
            email = "user@example.com",
            balance = 10000L,
            commissionBalance = 500L,
            expiredAt = 1700000000L,
            transferEnable = 107374182400L,
            usedDownload = 5368709120L,
            usedUpload = 1073741824L,
            uuid = "abc-123-def",
            planName = "Premium Plan"
        )

        val json = original.toJson()
        val restored = XBoardApi.UserInfo.fromJson(json)

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun `roundtrip with null expiredAt`() {
        val original = XBoardApi.UserInfo(
            email = "user@example.com",
            balance = 0L,
            commissionBalance = 0L,
            expiredAt = null,
            transferEnable = 0L,
            usedDownload = 0L,
            usedUpload = 0L,
            uuid = "uuid-1",
            planName = "Basic"
        )

        val json = original.toJson()
        val restored = XBoardApi.UserInfo.fromJson(json)

        assertNotNull(restored)
        assertEquals(original, restored)
        assertNull(restored!!.expiredAt)
    }

    @Test
    fun `roundtrip with null planName`() {
        val original = XBoardApi.UserInfo(
            email = "test@test.com",
            balance = 100L,
            commissionBalance = 0L,
            expiredAt = 1700000000L,
            transferEnable = 1024L,
            usedDownload = 512L,
            usedUpload = 256L,
            uuid = "uuid-2",
            planName = null
        )

        val json = original.toJson()
        val restored = XBoardApi.UserInfo.fromJson(json)

        assertNotNull(restored)
        assertEquals(original, restored)
        assertNull(restored!!.planName)
    }

    @Test
    fun `roundtrip with both expiredAt and planName null`() {
        val original = XBoardApi.UserInfo(
            email = "null@test.com",
            balance = 0L,
            commissionBalance = 0L,
            expiredAt = null,
            transferEnable = 0L,
            usedDownload = 0L,
            usedUpload = 0L,
            uuid = "",
            planName = null
        )

        val json = original.toJson()
        val restored = XBoardApi.UserInfo.fromJson(json)

        assertNotNull(restored)
        assertEquals(original, restored)
    }

    @Test
    fun `fromJson with invalid JSON returns null`() {
        assertNull(XBoardApi.UserInfo.fromJson("not valid json"))
        assertNull(XBoardApi.UserInfo.fromJson("{broken"))
    }

    @Test
    fun `fromJson with empty string returns null`() {
        assertNull(XBoardApi.UserInfo.fromJson(""))
    }

    @Test
    fun `fromJson with missing fields uses defaults`() {
        val json = """{"email":"a@b.com"}"""
        val info = XBoardApi.UserInfo.fromJson(json)

        assertNotNull(info)
        assertEquals("a@b.com", info!!.email)
        assertEquals(0L, info.balance)
        assertEquals(0L, info.commissionBalance)
        assertEquals(0L, info.transferEnable)
        assertEquals(0L, info.usedDownload)
        assertEquals(0L, info.usedUpload)
        assertEquals("", info.uuid)
    }
}
