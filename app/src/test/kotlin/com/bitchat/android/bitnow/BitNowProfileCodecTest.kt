package com.bitchat.android.bitnow

import org.junit.Assert.*
import org.junit.Test

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
