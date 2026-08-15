package com.bitchat.android

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/**
 * BitNow Android protocol-preview shell.
 *
 * Encounter transmission remains deliberately disabled in this legacy Android
 * codebase. The production Android client must move to the current encrypted
 * BitChat transport before proximity profiles or signals are allowed on-air.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 96, 56, 56)
        }

        container.addView(TextView(this).apply {
            text = "BitNow"
            textSize = 30f
        })

        container.addView(TextView(this).apply {
            text = "Android protocol preview"
            textSize = 20f
            setPadding(0, 24, 0, 24)
        })

        container.addView(TextView(this).apply {
            text = "Encrypted encounter transport is not enabled in this build. No nearby profile or signal data is transmitted."
            textSize = 16f
        })

        setContentView(container)
    }
}

// Retained while legacy view-model sources remain in this staging branch.
data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: String,
    val room: String? = null,
    val isPrivate: Boolean = false
)
