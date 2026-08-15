package com.bitchat.android.bitnow

import org.json.JSONObject

object BitNowProfileCodec {
    const val TYPE = "bitnow.profile.v1"

    fun encode(profile: BitNowProfile): String = JSONObject()
        .put("type", TYPE)
        .put("displayName", profile.displayName)
        .put("age", profile.age)
        .put("bio", profile.bio)
        .put("intent", profile.intent)
        .put("visible", profile.visible)
        .toString()

    fun decode(payload: String): BitNowProfile? = runCatching {
        val json = JSONObject(payload)
        if (json.optString("type") != TYPE) return null
        BitNowProfile(
            displayName = json.getString("displayName"),
            age = json.getInt("age"),
            bio = json.optString("bio"),
            intent = json.optString("intent", "Meet now"),
            visible = json.optBoolean("visible", true)
        )
    }.getOrNull()
}
