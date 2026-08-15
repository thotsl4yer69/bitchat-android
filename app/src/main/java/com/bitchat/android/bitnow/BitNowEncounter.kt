package com.bitchat.android.bitnow

import java.time.Instant

data class BitNowEncounter(
    val peerId: String,
    val profile: BitNowProfile,
    val firstSeen: Instant,
    val lastSeen: Instant,
    val transport: String
)
