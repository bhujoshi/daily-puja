package com.worship.nityamandir.data

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.coroutines.resume

/** Read once per installation; retain failed network claims for the next sync. */
class ReferralInstall(private val context: Context) {
    private val prefs = context.getSharedPreferences("referral_install", Context.MODE_PRIVATE)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    val hasPending get() = !prefs.getBoolean("processed", false) && !prefs.getString("share_id", "").isNullOrEmpty()

    suspend fun read() {
        mutex.lock()
        try { readOnce() } finally { mutex.unlock() }
    }

    private suspend fun readOnce() {
        if (prefs.getBoolean("read", false)) return
        withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val client = InstallReferrerClient.newBuilder(context).build()
                continuation.invokeOnCancellation { client.endConnection() }
                fun finish() {
                    client.endConnection()
                    if (continuation.isActive) continuation.resume(Unit)
                }
                try {
                    client.startConnection(object : InstallReferrerStateListener {
                        override fun onInstallReferrerSetupFinished(responseCode: Int) {
                            if (!continuation.isActive) { client.endConnection(); return }
                            if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                                runCatching {
                                    val response = client.installReferrer
                                    val share = Uri.parse("https://referral.local/?" + response.installReferrer)
                                        .getQueryParameter("share_id").orEmpty()
                                    prefs.edit().putBoolean("read", true)
                                        .putString("share_id", share)
                                        .putLong("installed_at", response.installBeginTimestampSeconds).apply()
                                }
                            } else if (responseCode == InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED) {
                                prefs.edit().putBoolean("read", true).apply()
                            }
                            finish()
                        }
                        override fun onInstallReferrerServiceDisconnected() { finish() }
                    })
                } catch (_: Exception) { finish() }
            }
        }
    }

    suspend fun pending(): JSONObject? {
        read()
        if (prefs.getBoolean("processed", false)) return null
        val share = prefs.getString("share_id", "").orEmpty()
        if (!share.matches(Regex("[a-f0-9]{48}"))) return null
        val digest = identity() ?: return null
        return JSONObject().put("share_id", share).put("install_id", digest)
            .put("installed_at", prefs.getLong("installed_at", 0))
    }

    suspend fun identity(): String? {
        // ANDROID_ID is scoped to this app signing key, Android user and device.
        // Only its digest leaves the device. It is stable across reinstalls.
        val identity = withContext(Dispatchers.IO) {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeIf { it.isNotBlank() }
        } ?: return null
        return java.security.MessageDigest.getInstance("SHA-256")
            .digest((context.packageName + ":" + identity).toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun processed() { prefs.edit().putBoolean("processed", true).apply() }
}
