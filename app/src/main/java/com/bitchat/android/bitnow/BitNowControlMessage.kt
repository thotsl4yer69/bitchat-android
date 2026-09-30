package com.bitchat.android.bitnow

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

sealed interface BitNowControlMessage {
    data class Profile(val profile: BitNowSharedProfile) : BitNowControlMessage
    data class Signal(
        val intent: BitNowIntent,
        val profile: BitNowSharedProfile? = null
    ) : BitNowControlMessage
    data object ProfileRequest : BitNowControlMessage

    companion object {
        private const val SEPARATOR = "\u2063"
        private const val PROFILE_PREFIX_V1 = "[BITNOW_PROFILE_V1]:"
        private const val INTEREST_PREFIX_V1 = "[BITNOW_INTEREST_V1]:"
        private const val MAX_ENCODED_PAYLOAD = 8192
        private const val APPLE_REFERENCE_UNIX_SECONDS = 978307200.0

        fun encodeProfile(profile: BitNowProfile): String =
            encodeEnvelope(
                kind = "profile",
                profile = profile.toSharedProfile(),
                visibleText = "⚡ BitNow profile shared"
            )

        fun encodeProfileRequest(): String =
            encodeEnvelope(
                kind = "profileRequest",
                visibleText = "⚡ BitNow profile request"
            )

        fun encodeSignal(intent: BitNowIntent, profile: BitNowProfile? = null): String =
            encodeEnvelope(
                kind = "signal",
                intent = intent,
                profile = profile?.toSharedProfile(),
                visibleText = "⚡ BitNow signal — ${intent.title.lowercase()}"
            )

        fun parse(content: String): BitNowControlMessage? {
            val separatorIndex = content.indexOf(SEPARATOR)
            if (separatorIndex >= 0) {
                val payload = content.substring(separatorIndex + SEPARATOR.length)
                if (payload.isBlank() || payload.length > MAX_ENCODED_PAYLOAD) return null
                return runCatching {
                    val decoded = Base64.getDecoder().decode(payload)
                    parseEnvelope(JSONObject(decoded.toString(Charsets.UTF_8)))
                }.getOrNull()
            }

            // Backward compatibility with the first Android BitNow prototype.
            val trimmed = content.trim()
            return when {
                trimmed.startsWith(PROFILE_PREFIX_V1) -> runCatching {
                    val encoded = trimmed.removePrefix(PROFILE_PREFIX_V1)
                    val decoded = Base64.getUrlDecoder().decode(encoded)
                    BitNowProfileCodec.decode(decoded.toString(Charsets.UTF_8))
                        ?.takeIf { it.isShareable }
                        ?.toSharedProfile()
                        ?.let(::Profile)
                }.getOrNull()
                trimmed.startsWith(INTEREST_PREFIX_V1) ->
                    if (trimmed.removePrefix(INTEREST_PREFIX_V1) == "1") {
                        Signal(BitNowIntent.MEET_NOW)
                    } else {
                        null
                    }
                else -> null
            }
        }

        private fun encodeEnvelope(
            kind: String,
            intent: BitNowIntent? = null,
            profile: BitNowSharedProfile? = null,
            visibleText: String
        ): String {
            val json = JSONObject()
                .put("version", 1)
                .put("kind", kind)
                .put("sentAt", System.currentTimeMillis() / 1000.0 - APPLE_REFERENCE_UNIX_SECONDS)

            intent?.let { json.put("intent", it.wireValue) }
            profile?.let { json.put("profile", profileJson(it)) }

            val body = Base64.getEncoder()
                .encodeToString(json.toString().toByteArray(Charsets.UTF_8))
            require(body.length <= MAX_ENCODED_PAYLOAD) { "BitNow payload is too large" }
            return visibleText + SEPARATOR + body
        }

        private fun parseEnvelope(json: JSONObject): BitNowControlMessage? {
            if (json.optInt("version", -1) != 1) return null
            val profile = json.optJSONObject("profile")?.let(::sharedProfileFromJson)
            if (profile?.isValid == false) return null

            return when (json.optString("kind")) {
                "profile" -> profile?.let(::Profile)
                "profileRequest" -> {
                    if (json.has("intent") || profile != null) null else ProfileRequest
                }
                "signal" -> {
                    val intent = BitNowIntent.fromWire(json.optString("intent")) ?: return null
                    Signal(intent, profile)
                }
                else -> null
            }
        }

        private fun profileJson(profile: BitNowSharedProfile): JSONObject {
            val json = JSONObject()
                .put("headline", profile.headline)
                .put("about", profile.about)
                .put("primaryIntent", profile.primaryIntent.wireValue)
                .put("interestedIn", JSONArray(profile.interestedIn.map { it.wireValue }))

            profile.age?.let { json.put("age", it) }
            profile.identity?.let { json.put("identity", it.wireValue) }
            profile.pronouns?.let { json.put("pronouns", it) }
            return json
        }

        private fun sharedProfileFromJson(json: JSONObject): BitNowSharedProfile? = runCatching {
            val interests = buildSet {
                val arr = json.optJSONArray("interestedIn")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        BitNowIdentity.fromWire(arr.optString(i))?.let(::add)
                    }
                }
            }
            BitNowSharedProfile(
                age = if (json.has("age") && !json.isNull("age")) json.getInt("age") else null,
                headline = json.optString("headline"),
                about = json.optString("about"),
                primaryIntent = BitNowIntent.fromWire(json.optString("primaryIntent"))
                    ?: return null,
                identity = BitNowIdentity.fromWire(json.optString("identity")),
                interestedIn = interests,
                pronouns = json.optString("pronouns").takeIf { it.isNotBlank() }
            ).takeIf { it.isValid }
        }.getOrNull()
    }
}
