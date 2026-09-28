package com.bitchat.android.bitnow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class BitNowControlMessageTest {
    @Test
    fun `signal round trips through the cross-platform envelope`() {
        val profile = BitNowProfile(
            displayName = "Alex",
            age = 29,
            headline = "Nearby",
            about = "Coffee first",
            primaryIntent = BitNowIntent.MEET_FIRST,
            identity = BitNowIdentity.NON_BINARY,
            interestedIn = setOf(BitNowIdentity.WOMAN)
        )

        val parsed = BitNowControlMessage.parse(
            BitNowControlMessage.encodeSignal(BitNowIntent.MEET_FIRST, profile)
        )

        assertTrue(parsed is BitNowControlMessage.Signal)
        parsed as BitNowControlMessage.Signal
        assertEquals(BitNowIntent.MEET_FIRST, parsed.intent)
        assertEquals(profile.toSharedProfile(), parsed.profile)
    }

    @Test
    fun `profile request accepts an iOS-shaped standard Base64 envelope`() {
        val json = """{"version":1,"kind":"profileRequest","sentAt":812345678.0}"""
        val payload = Base64.getEncoder().encodeToString(json.toByteArray())
        val parsed = BitNowControlMessage.parse("profile request\u2063$payload")

        assertEquals(BitNowControlMessage.ProfileRequest, parsed)
    }

    @Test
    fun `profile envelope never includes the local display name`() {
        val profile = BitNowProfile(
            displayName = "Local-only name",
            age = 32,
            headline = "Hello"
        )
        val wire = BitNowControlMessage.encodeProfile(profile)

        assertFalse(wire.contains("Local-only name"))
    }

    @Test
    fun `malformed envelopes fail closed`() {
        assertEquals(null, BitNowControlMessage.parse("hello"))
        assertEquals(null, BitNowControlMessage.parse("hello\u2063not-base64"))
    }
}
