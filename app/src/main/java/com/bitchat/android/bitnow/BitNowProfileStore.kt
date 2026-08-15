package com.bitchat.android.bitnow

import android.content.Context

object BitNowProfileStore {
    private const val PREFS = "bitnow_profile_v1"
    private const val KEY_PROFILE = "profile"

    fun load(context: Context): BitNowProfile? = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_PROFILE, null)
        ?.let(BitNowProfileCodec::decode)

    fun save(context: Context, profile: BitNowProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE, BitNowProfileCodec.encode(profile))
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PROFILE)
            .apply()
    }
}