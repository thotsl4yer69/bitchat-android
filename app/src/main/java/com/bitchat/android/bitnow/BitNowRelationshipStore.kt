package com.bitchat.android.bitnow

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object BitNowRelationshipStore {
    const val SIGNAL_LIFETIME_MS = 45L * 60_000L
    private const val PREFS = "bitnow_relationships_v2"
    private const val KEY_OUTGOING = "outgoing"

    data class Signal(
        val intent: BitNowIntent,
        val atMs: Long
    )

    private val outgoing = mutableMapOf<String, Signal>()
    private val incoming = mutableMapOf<String, Signal>()

    private val _myInterests = MutableStateFlow<Set<String>>(emptySet())
    val myInterests: StateFlow<Set<String>> = _myInterests.asStateFlow()

    private val _theirInterests = MutableStateFlow<Set<String>>(emptySet())
    val theirInterests: StateFlow<Set<String>> = _theirInterests.asStateFlow()

    fun initialize(context: Context, nowMs: Long = System.currentTimeMillis()) {
        outgoing.clear()
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_OUTGOING, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val json = JSONObject(raw)
                json.keys().forEach { peer ->
                    val item = json.getJSONObject(peer)
                    val intent = BitNowIntent.fromWire(item.optString("intent"))
                        ?: BitNowIntent.MEET_NOW
                    val atMs = item.optLong("atMs")
                    if (nowMs - atMs <= SIGNAL_LIFETIME_MS) {
                        outgoing[peer.lowercase()] = Signal(intent, atMs)
                    }
                }
            }
        }
        incoming.clear()
        publish()
        persist(context)
    }

    fun setMine(
        context: Context,
        peerId: String,
        intent: BitNowIntent,
        interested: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val id = peerId.lowercase()
        if (interested) outgoing[id] = Signal(intent, nowMs) else outgoing.remove(id)
        pruneExpired(context, nowMs)
        persist(context)
        publish()
    }

    fun setTheirs(
        peerId: String,
        intent: BitNowIntent,
        nowMs: Long = System.currentTimeMillis()
    ) {
        incoming[peerId.lowercase()] = Signal(intent, nowMs)
        pruneExpired(null, nowMs)
        publish()
    }

    fun isMatch(peerId: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val id = peerId.lowercase()
        val mine = outgoing[id]
        val theirs = incoming[id]
        return mine != null && theirs != null &&
            nowMs - mine.atMs <= SIGNAL_LIFETIME_MS &&
            nowMs - theirs.atMs <= SIGNAL_LIFETIME_MS
    }

    fun matches(nowMs: Long = System.currentTimeMillis()): Set<String> {
        pruneExpired(null, nowMs)
        return outgoing.keys intersect incoming.keys
    }

    fun clearPeer(context: Context, peerId: String) {
        val id = peerId.lowercase()
        outgoing.remove(id)
        incoming.remove(id)
        persist(context)
        publish()
    }

    fun clearAll(context: Context) {
        outgoing.clear()
        incoming.clear()
        persist(context)
        publish()
    }

    fun pruneExpired(context: Context?, nowMs: Long = System.currentTimeMillis()) {
        outgoing.entries.removeAll { nowMs - it.value.atMs > SIGNAL_LIFETIME_MS }
        incoming.entries.removeAll { nowMs - it.value.atMs > SIGNAL_LIFETIME_MS }
        if (context != null) persist(context)
        publish()
    }

    fun clearTransient() {
        incoming.clear()
        publish()
    }

    private fun publish() {
        _myInterests.value = outgoing.keys.toSet()
        _theirInterests.value = incoming.keys.toSet()
    }

    private fun persist(context: Context) {
        val json = JSONObject()
        outgoing.forEach { (peer, signal) ->
            json.put(peer, JSONObject()
                .put("intent", signal.intent.wireValue)
                .put("atMs", signal.atMs))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_OUTGOING, json.toString()).apply()
    }
}
