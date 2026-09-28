package com.bitchat.android.bitnow

import org.json.JSONArray
import org.json.JSONObject

object BitNowProfileCodec {
    private const val TYPE_V1 = "bitnow.profile.v1"
    private const val TYPE_V2 = "bitnow.profile.v2"

    fun encode(profile: BitNowProfile): String = JSONObject()
        .put("type", TYPE_V2)
        .put("displayName", profile.displayName)
        .put("age", profile.age)
        .put("headline", profile.headline)
        .put("about", profile.about)
        .put("primaryIntent", profile.primaryIntent.wireValue)
        .put("visibleNearby", profile.visibleNearby)
        .put("showAge", profile.showAge)
        .put("identity", profile.identity?.wireValue ?: JSONObject.NULL)
        .put("interestedIn", JSONArray(profile.interestedIn.map { it.wireValue }))
        .put("pronouns", profile.pronouns ?: JSONObject.NULL)
        .toString()

    fun decode(payload: String): BitNowProfile? = runCatching {
        val json = JSONObject(payload)
        when (json.optString("type")) {
            TYPE_V1 -> BitNowProfile(
                displayName = json.getString("displayName"),
                age = json.getInt("age"),
                bio = json.optString("bio"),
                intent = json.optString("intent", "Meet now"),
                visible = json.optBoolean("visible", false)
            )
            TYPE_V2 -> {
                val identities = buildSet {
                    val arr = json.optJSONArray("interestedIn")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            BitNowIdentity.fromWire(arr.optString(i))?.let(::add)
                        }
                    }
                }
                BitNowProfile(
                    displayName = json.getString("displayName"),
                    age = json.getInt("age"),
                    headline = json.optString("headline"),
                    about = json.optString("about"),
                    primaryIntent = BitNowIntent.fromWire(json.optString("primaryIntent"))
                        ?: BitNowIntent.MEET_NOW,
                    visibleNearby = json.optBoolean("visibleNearby", false),
                    showAge = json.optBoolean("showAge", true),
                    identity = if (json.isNull("identity")) null
                        else BitNowIdentity.fromWire(json.optString("identity")),
                    interestedIn = identities,
                    pronouns = if (json.isNull("pronouns")) null else json.optString("pronouns")
                )
            }
            else -> null
        }
    }.getOrNull()
}
