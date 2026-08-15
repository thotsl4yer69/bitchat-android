package com.bitchat.android.bitnow

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object BitNowRelationshipStore {
    private const val PREFS = "bitnow_relationships_v1"
    private const val KEY_MY_INTERESTS = "my_interests"

    private val _myInterests = MutableStateFlow<Set<String>>(emptySet())
    val myInterests: StateFlow<Set<String>> = _myInterests.asStateFlow()

    private val _theirInterests = MutableStateFlow<Set<String>>(emptySet())
    val theirInterests: StateFlow<Set<String>> = _theirInterests.asStateFlow()

    fun initialize(context: Context) {
        _myInterests.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_MY_INTERESTS, emptySet())
            ?.mapTo(mutableSetOf()) { it.lowercase() }
            ?: emptySet()
    }

    fun setMine(context: Context, peerId: String, interested: Boolean) {
        val id = peerId.lowercase()
        _myInterests.update { current -> if (interested) current + id else current - id }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_MY_INTERESTS, _myInterests.value)
            .apply()
    }

    fun setTheirs(peerId: String, interested: Boolean) {
        val id = peerId.lowercase()
        _theirInterests.update { current -> if (interested) current + id else current - id }
    }

    fun isMatch(peerId: String): Boolean {
        val id = peerId.lowercase()
        return id in _myInterests.value && id in _theirInterests.value
    }

    fun matches(): Set<String> = _myInterests.value intersect _theirInterests.value

    fun clearTransient() {
        _theirInterests.value = emptySet()
    }
}