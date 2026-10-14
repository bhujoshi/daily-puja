package com.worship.nityamandir.data

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhatsAppShareTest {
    @Test
    fun activityWrappersSendTheInvitationToWhatsAppWithoutANewTask() {
        lateinit var activity: Activity
        InstrumentationRegistry.getInstrumentation().runOnMainSync { activity = Activity() }
        val context = RecordingContext(ContextWrapper(activity))

        WhatsAppShare.open(context, "Daily puja\nhttps://example.com/invitation")

        val intent = context.intents.single()
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("com.whatsapp", intent.`package`)
        assertEquals("Daily puja\nhttps://example.com/invitation", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun applicationContextUsesANewTaskAndFallsBackToWhatsAppBusiness() {
        val context = RecordingContext(
            InstrumentationRegistry.getInstrumentation().targetContext,
            setOf("com.whatsapp")
        )

        WhatsAppShare.open(context, "Invitation")

        assertEquals(listOf("com.whatsapp", "com.whatsapp.w4b"), context.intents.map { it.`package` })
        context.intents.forEach {
            assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, it.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            assertEquals("Invitation", it.getStringExtra(Intent.EXTRA_TEXT))
        }
    }

    @Test
    fun unavailableWhatsAppVariantsProduceARecoverableTypedError() {
        val context = RecordingContext(
            InstrumentationRegistry.getInstrumentation().targetContext,
            setOf("com.whatsapp", "com.whatsapp.w4b")
        )

        try {
            WhatsAppShare.open(context, "Invitation")
            fail("Missing WhatsApp variants should report a typed error")
        } catch (error: MissingWhatsAppException) {
            assertTrue(error.message.orEmpty().contains("WhatsApp"))
        }

        assertEquals(2, context.intents.size)
    }

    @Test
    fun cyclicContextWrapperTerminatesAndUsesANewTask() {
        val context = object : RecordingContext(null) {
            override fun getBaseContext(): Context = this
        }

        WhatsAppShare.open(context, "Invitation")

        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, context.intents.single().flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private open class RecordingContext(
        base: Context?,
        private val unavailablePackages: Set<String> = emptySet()
    ) : ContextWrapper(base) {
        val intents = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            intents += Intent(intent)
            if (intent.`package` in unavailablePackages) throw ActivityNotFoundException()
        }
    }
}
