package com.bitchat.android.bitnow

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class BitNowAvailabilityWindow(val durationMs: Long, val title: String) {
    THIRTY_MINUTES(30L * 60_000L, "30 min"),
    ONE_HOUR(60L * 60_000L, "1 hour"),
    TWO_HOURS(2L * 60L * 60_000L, "2 hours"),
    FOUR_HOURS(4L * 60L * 60_000L, "4 hours")
}

object BitNowAvailabilityStore {
    private const val PREFS = "bitnow_availability_v1"
    private const val KEY_UNTIL = "available_until_ms"

    private val _availableUntilMs = MutableStateFlow<Long?>(null)
    val availableUntilMs: StateFlow<Long?> = _availableUntilMs.asStateFlow()

    fun initialize(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_UNTIL, 0L)
            .takeIf { it > nowMs }
        _availableUntilMs.value = stored
        if (stored == null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY_UNTIL).apply()
        }
    }

    fun start(
        context: Context,
        window: BitNowAvailabilityWindow,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val until = nowMs + window.durationMs
        _availableUntilMs.value = until
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_UNTIL, until).apply()
        return until
    }

    fun stop(context: Context) {
        _availableUntilMs.value = null
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_UNTIL).apply()
    }

    fun expireIfNeeded(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean {
        val until = _availableUntilMs.value ?: return false
        if (until > nowMs) return false
        stop(context)
        return true
    }

    fun isAvailable(profile: BitNowProfile?, nowMs: Long = System.currentTimeMillis()): Boolean {
        val until = _availableUntilMs.value ?: return false
        return profile?.visibleNearby == true && profile.isShareable && until > nowMs
    }

    /**
     * Stateless read for lower transport layers that can be created before the
     * Compose/ViewModel layer initializes this store's StateFlow.
     */
    fun isPersistedAvailable(
        context: Context,
        profile: BitNowProfile?,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val until = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_UNTIL, 0L)
        return profile?.visibleNearby == true && profile.isShareable && until > nowMs
    }
}

object BitNowDiscoveryFilterStore {
    private const val PREFS = "bitnow_discovery_filter_v1"
    private const val KEY_FILTER = "filter"

    private val _filter = MutableStateFlow(BitNowDiscoveryFilter())
    val filter: StateFlow<BitNowDiscoveryFilter> = _filter.asStateFlow()

    fun initialize(context: Context) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FILTER, null)
        _filter.value = raw?.let(::decode) ?: BitNowDiscoveryFilter()
    }

    fun save(context: Context, filter: BitNowDiscoveryFilter) {
        val normalized = filter.normalized()
        _filter.value = normalized
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FILTER, encode(normalized)).apply()
    }

    fun reset(context: Context) {
        save(context, BitNowDiscoveryFilter())
    }

    private fun encode(filter: BitNowDiscoveryFilter): String = JSONObject()
        .put("minimumAge", filter.minimumAge)
        .put("maximumAge", filter.maximumAge)
        .put("identities", JSONArray(filter.identities.map { it.wireValue }))
        .put("intents", JSONArray(filter.intents.map { it.wireValue }))
        .toString()

    private fun decode(raw: String): BitNowDiscoveryFilter? = runCatching {
        val json = JSONObject(raw)
        val identities = buildSet {
            val arr = json.optJSONArray("identities")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    BitNowIdentity.fromWire(arr.optString(i))?.let(::add)
                }
            }
        }
        val intents = buildSet {
            val arr = json.optJSONArray("intents")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    BitNowIntent.fromWire(arr.optString(i))?.let(::add)
                }
            }
        }
        BitNowDiscoveryFilter(
            minimumAge = json.optInt("minimumAge", 18),
            maximumAge = json.optInt("maximumAge", 99),
            identities = identities.ifEmpty { BitNowIdentity.entries.toSet() },
            intents = intents.ifEmpty { BitNowIntent.entries.toSet() }
        ).normalized()
    }.getOrNull()
}

object BitNowSafetyStore {
    private const val PREFS = "bitnow_safety_v1"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_REPORTS = "reports"

    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    fun initialize(context: Context) {
        _blocked.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_BLOCKED, emptySet())
            ?.mapTo(mutableSetOf()) { it.lowercase() }
            ?: emptySet()
    }

    fun isBlocked(peerId: String): Boolean = peerId.lowercase() in _blocked.value

    fun block(context: Context, peerId: String) {
        val id = peerId.lowercase()
        _blocked.value = _blocked.value + id
        persistBlocked(context)
        BitNowRegistry.remove(id)
        BitNowRelationshipStore.clearPeer(context, id)
    }

    fun reportAndBlock(
        context: Context,
        peerId: String,
        reason: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val reports = prefs.getStringSet(KEY_REPORTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        reports += JSONObject()
            .put("peerId", peerId.lowercase())
            .put("reason", reason.take(280))
            .put("reportedAtMs", nowMs)
            .toString()
        prefs.edit().putStringSet(KEY_REPORTS, reports.toList().takeLast(100).toSet()).apply()
        block(context, peerId)
    }

    fun clear(context: Context) {
        _blocked.value = emptySet()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun persistBlocked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_BLOCKED, _blocked.value).apply()
    }
}
