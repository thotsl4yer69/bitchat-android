package com.bitchat.android.bitnow

import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class BitNowIntent(val title: String) {
    @SerialName("now") NOW("right now"),
    @SerialName("tonight") TONIGHT("tonight"),
    @SerialName("meetFirst") MEET_FIRST("meet first"),
    @SerialName("chatFirst") CHAT_FIRST("chat first")
}

@Serializable
enum class BitNowIdentity(val title: String) {
    @SerialName("man") MAN("man"),
    @SerialName("woman") WOMAN("woman"),
    @SerialName("nonBinary") NON_BINARY("non-binary"),
    @SerialName("couple") COUPLE("couple"),
    @SerialName("other") OTHER("other / self-described")
}

@Serializable
data class BitNowProfile(
    val age: Int = 18,
    val headline: String = "",
    val about: String = "",
    val primaryIntent: BitNowIntent = BitNowIntent.NOW,
    val visibleNearby: Boolean = false,
    val showAge: Boolean = true,
    val identity: BitNowIdentity? = null,
    val interestedIn: Set<BitNowIdentity>? = null,
    val pronouns: String? = null
) {
    val isAdult: Boolean get() = age in 18..99

    fun shared(): BitNowSharedProfile = BitNowSharedProfile(
        age = if (showAge && isAdult) age else null,
        headline = headline.take(BitNowSharedProfile.MAX_HEADLINE_LENGTH),
        about = about.take(BitNowSharedProfile.MAX_ABOUT_LENGTH),
        primaryIntent = primaryIntent,
        identity = identity,
        interestedIn = interestedIn,
        pronouns = pronouns
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.take(BitNowSharedProfile.MAX_PRONOUNS_LENGTH)
    )
}

@Serializable
data class BitNowSharedProfile(
    val age: Int? = null,
    val headline: String,
    val about: String,
    val primaryIntent: BitNowIntent,
    val identity: BitNowIdentity? = null,
    val interestedIn: Set<BitNowIdentity>? = null,
    val pronouns: String? = null
) {
    companion object {
        const val MAX_HEADLINE_LENGTH = 80
        const val MAX_ABOUT_LENGTH = 280
        const val MAX_PRONOUNS_LENGTH = 32
    }

    val ageLabel: String get() = age?.toString() ?: "18+"

    fun isValid(): Boolean =
        (age == null || age in 18..99) &&
            headline.length <= MAX_HEADLINE_LENGTH &&
            about.length <= MAX_ABOUT_LENGTH &&
            (pronouns?.length ?: 0) <= MAX_PRONOUNS_LENGTH &&
            (interestedIn?.size ?: 0) <= BitNowIdentity.entries.size
}

@Serializable
enum class BitNowWireKind {
    @SerialName("signal") SIGNAL,
    @SerialName("profileRequest") PROFILE_REQUEST,
    @SerialName("profile") PROFILE
}

@Serializable
data class BitNowWireEnvelope(
    val version: Int = CURRENT_VERSION,
    val kind: BitNowWireKind,
    val intent: BitNowIntent? = null,
    val profile: BitNowSharedProfile? = null,
    /** Seconds since Apple's 2001-01-01 reference date, matching Swift JSONEncoder(Date). */
    val sentAt: Double = appleReferenceSeconds()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }

    fun isValid(): Boolean {
        if (version != CURRENT_VERSION || profile?.isValid() == false) return false
        return when (kind) {
            BitNowWireKind.SIGNAL -> intent != null
            BitNowWireKind.PROFILE_REQUEST -> intent == null && profile == null
            BitNowWireKind.PROFILE -> intent == null && profile != null
        }
    }
}

object BitNowWireCodec {
    private const val SEPARATOR = '\u2063'
    private const val LEGACY_SIGNAL_PREFIX = "⚡ BitNow • signal • "
    private const val MAX_ENCODED_PAYLOAD_LENGTH = 8_192

    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    fun encodeSignal(intent: BitNowIntent, profile: BitNowProfile? = null): String = encode(
        BitNowWireEnvelope(
            kind = BitNowWireKind.SIGNAL,
            intent = intent,
            profile = profile?.shared()
        ),
        visibleText = "⚡ BitNow signal — ${intent.title}"
    )

    fun encodeProfileRequest(): String = encode(
        BitNowWireEnvelope(kind = BitNowWireKind.PROFILE_REQUEST),
        visibleText = "⚡ BitNow profile request"
    )

    fun encodeProfile(profile: BitNowProfile): String = encode(
        BitNowWireEnvelope(
            kind = BitNowWireKind.PROFILE,
            profile = profile.shared()
        ),
        visibleText = "⚡ BitNow profile shared"
    )

    fun decode(content: String): BitNowWireEnvelope? {
        val separatorIndex = content.indexOf(SEPARATOR)
        if (separatorIndex >= 0) {
            val payload = content.substring(separatorIndex + 1)
            if (payload.isEmpty() || payload.length > MAX_ENCODED_PAYLOAD_LENGTH) return null
            return runCatching {
                val bytes = Base64.getDecoder().decode(payload)
                json.decodeFromString<BitNowWireEnvelope>(bytes.toString(Charsets.UTF_8))
            }.getOrNull()?.takeIf { it.isValid() }
        }

        if (content.startsWith(LEGACY_SIGNAL_PREFIX)) {
            val raw = content.removePrefix(LEGACY_SIGNAL_PREFIX)
            val intent = BitNowIntent.entries.firstOrNull { serializedIntent(it) == raw } ?: return null
            return BitNowWireEnvelope(kind = BitNowWireKind.SIGNAL, intent = intent)
        }

        return null
    }

    private fun encode(envelope: BitNowWireEnvelope, visibleText: String): String {
        if (!envelope.isValid()) return visibleText
        val bytes = json.encodeToString(envelope).toByteArray(Charsets.UTF_8)
        return visibleText + SEPARATOR + Base64.getEncoder().encodeToString(bytes)
    }

    private fun serializedIntent(intent: BitNowIntent): String = when (intent) {
        BitNowIntent.NOW -> "now"
        BitNowIntent.TONIGHT -> "tonight"
        BitNowIntent.MEET_FIRST -> "meetFirst"
        BitNowIntent.CHAT_FIRST -> "chatFirst"
    }
}

private const val APPLE_REFERENCE_UNIX_SECONDS = 978_307_200.0

private fun appleReferenceSeconds(epochMillis: Long = System.currentTimeMillis()): Double =
    (epochMillis / 1_000.0) - APPLE_REFERENCE_UNIX_SECONDS
