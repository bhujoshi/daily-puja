package com.worship.nityamandir.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

object WhatsAppShare {
    /** Returns only after Android accepts the handoff; the user chooses a recipient. */
    fun open(context: Context, text: String) {
        for (app in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage(app).putExtra(Intent.EXTRA_TEXT, text))
                return
            } catch (_: ActivityNotFoundException) { /* Try WhatsApp Business next. */ }
        }
        throw IllegalStateException("Install WhatsApp or WhatsApp Business to share your invitation.")
    }
}
