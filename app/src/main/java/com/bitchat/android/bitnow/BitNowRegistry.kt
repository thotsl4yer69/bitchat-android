package com.bitchat.android.bitnow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BitNowRegistry {
    private const val PROFILE_TTL_MS = 2L * 60_000L

    private data class Entry(
        val profile: BitNowSharedProfile,
        val lastSeenMs: Long
    )

    private val entries = mutableMapOf<String, Entry>()
    private val _profiles = MutableStateFlow<Map<String, BitNowSharedProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, BitNowSharedProfile>> = _profiles.asStateFlow()

    @Synchronized
    fun update(
        peerId: String,
        profile: BitNowSharedProfile,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!profile.isValid || BitNowSafetyStore.isBlocked(peerId)) return
        entries[peerId.lowercase()] = Entry(profile, nowMs)
        publish()
    }

    @Synchronized
    fun remove(peerId: String) {
        entries.remove(peerId.lowercase())
        publish()
    }

    @Synchronized
    fun retain(peerIds: Collection<String>) {
        val allowed = peerIds.mapTo(mutableSetOf()) { it.lowercase() }
        entries.keys.retainAll(allowed)
        publish()
    }

    @Synchronized
    fun prune(nowMs: Long = System.currentTimeMillis()) {
        entries.entries.removeAll { nowMs - it.value.lastSeenMs > PROFILE_TTL_MS }
        publish()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        publish()
    }

    private fun publish() {
        _profiles.value = entries.mapValues { it.value.profile }
    }
}
