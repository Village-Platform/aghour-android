package com.aghour.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle it (e.g. no maps app installed).
    }
}

fun Context.openDialer(phone: String) =
    startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

/** Egyptian local numbers (01xxxxxxxxx) become international (201xxxxxxxxx) for wa.me links. */
fun Context.openWhatsApp(number: String) {
    val digits = number.filter { it.isDigit() }
    val international = if (digits.startsWith("0")) "2$digits" else digits
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$international")))
}

fun Context.openMap(lat: Double, lng: Double, label: String) =
    startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")))

fun Context.shareText(text: String) =
    startSafely(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
