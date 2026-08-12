package com.bitchat.android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "BitNow preview\n\nEncrypted encounter transport is not enabled in this build."
        textView.textSize = 20f
        textView.setPadding(50, 100, 50, 50)
        setContentView(textView)
    }
}
