package com.bitchat.android.bitnow

enum class BitNowIntent(val wireValue: String, val title: String) {
    MEET_NOW("now", "Meet now"),
    TONIGHT("tonight", "Tonight"),
    MEET_FIRST("meetFirst", "Meet first"),
    CHAT_FIRST("chatFirst", "Chat first");

    companion object {
        fun fromWire(value: String?): BitNowIntent? = when (value?.trim()) {
            "now", "Meet now", "meet now" -> MEET_NOW
            "tonight", "Tonight" -> TONIGHT
            "meetFirst", "Meet first", "Open to plans", "open to plans" -> MEET_FIRST
            "chatFirst", "Chat first", "Chat now", "chat now" -> CHAT_FIRST
            else -> null
        }
    }
}

enum class BitNowIdentity(val wireValue: String, val title: String) {
    MAN("man", "Man"),
    WOMAN("woman", "Woman"),
    NON_BINARY("nonBinary", "Non-binary"),
    COUPLE("couple", "Couple"),
    OTHER("other", "Other / self-described");

    companion object {
        fun fromWire(value: String?): BitNowIdentity? =
            entries.firstOrNull { it.wireValue == value }
    }
}

object BitNowProfileContentPolicy {
    private val blockedFragments = listOf(
        "i am under 18",
        "i'm under 18",
        "underage",
        "13yo", "13 yo",
        "14yo", "14 yo",
        "15yo", "15 yo",
        "16yo", "16 yo",
        "17yo", "17 yo",
        "pay for sex",
        "cash for sex",
        "sex for cash",
        "buy sex",
        "selling sex",
        "escort service",
        "escort services",
        "prostitution",
        "send nudes",
        "nude pics",
        "explicit videos"
    )

    fun allows(headline: String, about: String, pronouns: String?): Boolean {
        val combined = listOf(headline, about, pronouns.orEmpty())
            .joinToString(" ")
            .lowercase()
            .replace("\n", " ")
        return blockedFragments.none(combined::contains)
    }
}

data class BitNowProfile(
    val displayName: String,
    val age: Int,
    val headline: String = "",
    val about: String = "",
    val primaryIntent: BitNowIntent = BitNowIntent.MEET_NOW,
    val visibleNearby: Boolean = false,
    val showAge: Boolean = true,
    val identity: BitNowIdentity? = null,
    val interestedIn: Set<BitNowIdentity> = emptySet(),
    val pronouns: String? = null
) {
    init {
        require(displayName.isNotBlank()) { "BitNow display name is required" }
        require(age in 18..99) { "BitNow profiles must be 18+" }
        require(headline.length <= BitNowSharedProfile.MAX_HEADLINE_LENGTH)
        require(about.length <= BitNowSharedProfile.MAX_ABOUT_LENGTH)
        require((pronouns?.length ?: 0) <= BitNowSharedProfile.MAX_PRONOUNS_LENGTH)
    }

    // Compatibility with the first Android BitNow prototype.
    constructor(
        displayName: String,
        age: Int,
        bio: String,
        intent: String,
        visible: Boolean
    ) : this(
        displayName = displayName,
        age = age,
        about = bio,
        primaryIntent = BitNowIntent.fromWire(intent) ?: BitNowIntent.MEET_NOW,
        visibleNearby = visible
    )

    val bio: String get() = about
    val intent: String get() = primaryIntent.title
    val visible: Boolean get() = visibleNearby

    val isShareable: Boolean
        get() = age in 18..99 &&
            BitNowProfileContentPolicy.allows(headline, about, pronouns)

    fun toSharedProfile(): BitNowSharedProfile = BitNowSharedProfile(
        age = age.takeIf { showAge },
        headline = headline.take(BitNowSharedProfile.MAX_HEADLINE_LENGTH),
        about = about.take(BitNowSharedProfile.MAX_ABOUT_LENGTH),
        primaryIntent = primaryIntent,
        identity = identity,
        interestedIn = interestedIn,
        pronouns = pronouns?.trim()?.takeIf { it.isNotEmpty() }
            ?.take(BitNowSharedProfile.MAX_PRONOUNS_LENGTH)
    )
}

data class BitNowSharedProfile(
    val age: Int?,
    val headline: String,
    val about: String,
    val primaryIntent: BitNowIntent,
    val identity: BitNowIdentity?,
    val interestedIn: Set<BitNowIdentity> = emptySet(),
    val pronouns: String?
) {
    companion object {
        const val MAX_HEADLINE_LENGTH = 80
        const val MAX_ABOUT_LENGTH = 280
        const val MAX_PRONOUNS_LENGTH = 32
    }

    val ageLabel: String get() = age?.toString() ?: "18+"

    val isValid: Boolean
        get() = (age == null || age in 18..99) &&
            headline.length <= MAX_HEADLINE_LENGTH &&
            about.length <= MAX_ABOUT_LENGTH &&
            (pronouns?.length ?: 0) <= MAX_PRONOUNS_LENGTH &&
            BitNowProfileContentPolicy.allows(headline, about, pronouns)

    fun appearsInterestedIn(localIdentity: BitNowIdentity?): Boolean {
        if (interestedIn.isEmpty()) return true
        return localIdentity != null && localIdentity in interestedIn
    }
}

data class BitNowDiscoveryFilter(
    val minimumAge: Int = 18,
    val maximumAge: Int = 99,
    val identities: Set<BitNowIdentity> = BitNowIdentity.entries.toSet(),
    val intents: Set<BitNowIntent> = BitNowIntent.entries.toSet()
) {
    fun normalized(): BitNowDiscoveryFilter {
        val min = minimumAge.coerceIn(18, 99)
        val max = maximumAge.coerceIn(min, 99)
        return copy(minimumAge = min, maximumAge = max)
    }

    fun matches(profile: BitNowSharedProfile): Boolean {
        val filter = normalized()
        val fullAgeRange = filter.minimumAge == 18 && filter.maximumAge == 99
        val ageMatches = profile.age?.let { it in filter.minimumAge..filter.maximumAge } ?: fullAgeRange
        val allIdentities = BitNowIdentity.entries.toSet()
        val identityMatches = profile.identity?.let {
            filter.identities.isEmpty() || it in filter.identities
        } ?: (filter.identities.isEmpty() || filter.identities == allIdentities)
        val intentMatches = filter.intents.isEmpty() || profile.primaryIntent in filter.intents
        return ageMatches && identityMatches && intentMatches
    }
}
