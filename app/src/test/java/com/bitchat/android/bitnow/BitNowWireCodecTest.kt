package com.bitchat.android.bitnow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitNowWireCodecTest {
    @Test
    fun signalsRoundTrip() {
        BitNowIntent.entries.forEach { intent ->
            val decoded = BitNowWireCodec.decode(BitNowWireCodec.encodeSignal(intent))
            assertEquals(BitNowWireKind.SIGNAL, decoded?.kind)
            assertEquals(intent, decoded?.intent)
        }
    }

    @Test
    fun sharedProfilePreservesExplicitAdultFields() {
        val profile = BitNowProfile(
            age = 28,
            headline = "nearby tonight",
            about = "chat first",
            primaryIntent = BitNowIntent.TONIGHT,
            visibleNearby = true,
            showAge = true,
            identity = BitNowIdentity.WOMAN,
            interestedIn = setOf(BitNowIdentity.MAN, BitNowIdentity.WOMAN),
            pronouns = "she/her"
        )

        val decoded = BitNowWireCodec.decode(BitNowWireCodec.encodeSignal(BitNowIntent.NOW, profile))
        assertEquals(28, decoded?.profile?.age)
        assertEquals("nearby tonight", decoded?.profile?.headline)
        assertEquals(BitNowIdentity.WOMAN, decoded?.profile?.identity)
        assertEquals(setOf(BitNowIdentity.MAN, BitNowIdentity.WOMAN), decoded?.profile?.interestedIn)
        assertEquals("she/her", decoded?.profile?.pronouns)
    }

    @Test
    fun hiddenAgeRemainsHidden() {
        val profile = BitNowProfile(age = 31, showAge = false)
        val decoded = BitNowWireCodec.decode(BitNowWireCodec.encodeProfile(profile))
        assertNull(decoded?.profile?.age)
        assertEquals("18+", decoded?.profile?.ageLabel)
    }

    @Test
    fun requestRoundTripsAndOrdinaryChatIsIgnored() {
        val request = BitNowWireCodec.decode(BitNowWireCodec.encodeProfileRequest())
        assertEquals(BitNowWireKind.PROFILE_REQUEST, request?.kind)
        assertNull(BitNowWireCodec.decode("hey, are you around?"))
    }

    @Test
    fun profileValidationRejectsBadAdultAge() {
        val invalid = BitNowSharedProfile(
            age = 17,
            headline = "",
            about = "",
            primaryIntent = BitNowIntent.NOW
        )
        assertFalse(invalid.isValid())

        val valid = invalid.copy(age = 18)
        assertTrue(valid.isValid())
    }
}
