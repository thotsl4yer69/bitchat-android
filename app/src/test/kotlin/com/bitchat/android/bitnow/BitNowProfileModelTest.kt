package com.bitchat.android.bitnow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitNowProfileModelTest {
    @Test
    fun `new profiles are adult-only shareable and hidden by default`() {
        val profile = BitNowProfile(
            displayName = "Alex",
            age = 29,
            headline = "Coffee nearby",
            about = "Chat first",
            identity = BitNowIdentity.NON_BINARY,
            interestedIn = setOf(BitNowIdentity.WOMAN, BitNowIdentity.NON_BINARY)
        )

        assertTrue(profile.isShareable)
        assertFalse(profile.visibleNearby)
    }

    @Test
    fun `content policy rejects explicit underage claims`() {
        val profile = BitNowProfile(
            displayName = "Alex",
            age = 29,
            about = "I am under 18"
        )

        assertFalse(profile.isShareable)
    }

    @Test
    fun `hidden age is excluded by a restrictive age filter`() {
        val hiddenAge = BitNowProfile(
            displayName = "Alex",
            age = 29,
            showAge = false
        ).toSharedProfile()

        assertFalse(BitNowDiscoveryFilter(minimumAge = 25, maximumAge = 35).matches(hiddenAge))
        assertTrue(BitNowDiscoveryFilter().matches(hiddenAge))
    }

    @Test
    fun `remote interests require the local identity when specified`() {
        val remote = BitNowProfile(
            displayName = "Sam",
            age = 31,
            interestedIn = setOf(BitNowIdentity.WOMAN)
        ).toSharedProfile()

        assertTrue(remote.appearsInterestedIn(BitNowIdentity.WOMAN))
        assertFalse(remote.appearsInterestedIn(BitNowIdentity.MAN))
        assertFalse(remote.appearsInterestedIn(null))
    }

    @Test
    fun `age filter normalization never leaves invalid bounds`() {
        val filter = BitNowDiscoveryFilter(minimumAge = 110, maximumAge = 12).normalized()

        assertTrue(filter.minimumAge == 99)
        assertTrue(filter.maximumAge == 99)
    }
}
