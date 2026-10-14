package com.worship.nityamandir.data

import android.content.ActivityNotFoundException
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import java.util.Collections
import java.util.IdentityHashMap

class MissingWhatsAppException : IllegalStateException(
    "Install WhatsApp or WhatsApp Business to share your invitation."
)

object WhatsAppShare {
    /** Returns only after Android accepts the handoff; the user chooses a recipient. */
    fun open(context: Context, text: String) {
        val flags = if (context.hasActivity()) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        for (app in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                context.startActivity(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage(app).putExtra(Intent.EXTRA_TEXT, text).addFlags(flags))
                return
            } catch (_: ActivityNotFoundException) { /* Try WhatsApp Business next. */ }
        }
        throw MissingWhatsAppException()
    }

    private fun Context.hasActivity(): Boolean {
        var context = this
        val visited = Collections.newSetFromMap(IdentityHashMap<Context, Boolean>())
        while (visited.add(context)) {
            if (context is Activity) return true
            context = (context as? ContextWrapper)?.baseContext ?: return false
        }
        return false
    }
}
