package com.bitchat.android.bitnow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BitNowRegistry {
    private val _profiles = MutableStateFlow<Map<String, BitNowProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, BitNowProfile>> = _profiles.asStateFlow()

    fun update(peerId: String, profile: BitNowProfile) {
        _profiles.update { it + (peerId.lowercase() to profile) }
    }

    fun remove(peerId: String) {
        _profiles.update { it - peerId.lowercase() }
    }

    fun retain(peerIds: Collection<String>) {
        val allowed = peerIds.mapTo(mutableSetOf()) { it.lowercase() }
        _profiles.update { current -> current.filterKeys { it in allowed } }
    }

    fun clear() {
        _profiles.value = emptyMap()
    }
}