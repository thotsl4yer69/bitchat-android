package com.bitchat.android.bitnow

data class BitNowProfile(
    val displayName: String,
    val age: Int,
    val bio: String = "",
    val intent: String = "Meet now",
    val visible: Boolean = true
) {
    init {
        require(displayName.isNotBlank())
        require(age >= 18) { "BitNow profiles must be 18+" }
    }
}
