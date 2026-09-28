package com.bitchat.android.bitnow

import android.content.Intent
import android.net.Uri
import com.bitchat.android.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class BitNowReportReason(val title: String) {
    UNDERAGE("under 18 / age concern"),
    HARASSMENT("harassment or unwanted contact"),
    IMPERSONATION("impersonation or deception"),
    EXPLOITATION("sexual services, exploitation or trafficking"),
    NON_CONSENSUAL("non-consensual intimate content"),
    THREATS("threats or immediate safety concern"),
    OTHER("other")
}

object BitNowSupport {
    fun reportingEmail(raw: String = BuildConfig.BITNOW_REPORT_EMAIL): String? {
        val value = raw.trim()
        return value.takeIf {
            it.isNotEmpty() &&
                it.contains("@") &&
                !it.contains("\$(") &&
                !it.contains("\n") &&
                !it.contains("\r")
        }
    }

    fun reportIntent(
        peerId: String,
        displayName: String,
        reason: BitNowReportReason,
        details: String,
        nowMs: Long = System.currentTimeMillis(),
        configuredEmail: String = BuildConfig.BITNOW_REPORT_EMAIL
    ): Intent? {
        val email = reportingEmail(configuredEmail) ?: return null
        val cleanDetails = details.trim()
        if (cleanDetails.isEmpty()) return null

        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(nowMs))

        val peerPrefix = peerId.lowercase().take(24)
        val subject = "BitNow safety report — ${reason.title}"
        val body = """
            BitNow safety report

            Reported display name: ${displayName.take(80)}
            Peer identifier prefix: $peerPrefix
            Reason: ${reason.title}
            Reported at: $timestamp

            Details:
            ${cleanDetails.take(2_000)}

            Please do not include passwords, financial credentials, exact home addresses, or intimate media in this email.
        """.trimIndent()

        val uri = Uri.parse(
            "mailto:${Uri.encode(email)}" +
                "?subject=${Uri.encode(subject)}" +
                "&body=${Uri.encode(body)}"
        )
        return Intent(Intent.ACTION_SENDTO, uri)
    }
}
