package com.bitchat.android.bitnow

import android.util.Base64

sealed interface BitNowControlMessage {
    data class Profile(val profile: BitNowProfile) : BitNowControlMessage
    data class Interest(val interested: Boolean) : BitNowControlMessage

    companion object {
        private const val PROFILE_PREFIX = "[BITNOW_PROFILE_V1]:"
        private const val INTEREST_PREFIX = "[BITNOW_INTEREST_V1]:"
        private const val MAX_PROFILE_BYTES = 4096

        fun encodeProfile(profile: BitNowProfile): String {
            val json = BitNowProfileCodec.encode(profile).toByteArray(Charsets.UTF_8)
            require(json.size <= MAX_PROFILE_BYTES) { "BitNow profile is too large" }
            val body = Base64.encodeToString(json, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            return PROFILE_PREFIX + body
        }

        fun encodeInterest(interested: Boolean): String =
            INTEREST_PREFIX + if (interested) "1" else "0"

        fun parse(content: String): BitNowControlMessage? {
            val trimmed = content.trim()
            return when {
                trimmed.startsWith(PROFILE_PREFIX) -> {
                    val encoded = trimmed.removePrefix(PROFILE_PREFIX)
                    runCatching {
                        val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                        if (bytes.size > MAX_PROFILE_BYTES) return null
                        BitNowProfileCodec.decode(bytes.toString(Charsets.UTF_8))?.let(::Profile)
                    }.getOrNull()
                }
                trimmed.startsWith(INTEREST_PREFIX) -> when (trimmed.removePrefix(INTEREST_PREFIX)) {
                    "1" -> Interest(true)
                    "0" -> Interest(false)
                    else -> null
                }
                else -> null
            }
        }
    }
}