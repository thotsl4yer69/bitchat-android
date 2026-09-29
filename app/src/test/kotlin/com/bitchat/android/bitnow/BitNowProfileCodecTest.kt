package com.bitchat.android.bitnow

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BitNowProfileCodecTest {
    @Test fun profileJsonRoundTrips() {
        val input = BitNowProfile("Alex", 29, "Coffee?", "Meet now", true)
        assertEquals(input, BitNowProfileCodec.decode(BitNowProfileCodec.encode(input)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnder18Profiles() {
        BitNowProfile("Nope", 17)
    }
}
