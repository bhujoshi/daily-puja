package com.worship.nityamandir.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.worship.nityamandir.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class GoogleSignInFailure(val reason: Reason, cause: Throwable? = null) : IllegalStateException(
    when (reason) {
        Reason.UNAVAILABLE -> "Google sign-in is unavailable. Please try again later."
        Reason.PROVIDER_ERROR -> "Google sign-in could not start. Please try again."
        Reason.INVALID_CREDENTIAL -> "Google sign-in could not be completed. Please try again."
    }, cause
) {
    enum class Reason { UNAVAILABLE, PROVIDER_ERROR, INVALID_CREDENTIAL }
}

class AccountConnectionFailure(val reason: Reason, cause: IOException) : IOException(
    when (reason) {
        Reason.TIMEOUT -> "The connection timed out. Please try again."
        Reason.CONNECTION -> "Could not connect. Check your internet connection and try again."
    }, cause
) {
    enum class Reason { TIMEOUT, CONNECTION }
}

/** Account identity with a Keystore-protected session and durable profile snapshot. */
class DevotionAccount(context: Context) {
    private val referral = ReferralInstall(context.applicationContext)
    private val credentialManager = androidx.credentials.CredentialManager.create(context)
    val googleConfigured get() = configured && BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()
    private val prefs = context.getSharedPreferences("devotion", Context.MODE_PRIVATE)
    private val vault = SessionVault()
    private var token = runCatching { vault.decrypt(prefs.getString("session", "") ?: "") }.getOrDefault("")
    var profile: JSONObject? by mutableStateOf(if (token.isNotEmpty()) runCatching { JSONObject(prefs.getString("profile", "") ?: "") }.getOrNull() else null)
        private set
    val configured get():Boolean {
        val uri=runCatching {java.net.URI(BuildConfig.ACCOUNT_API_URL)}.getOrNull() ?: return false
        return uri.scheme=="https" && !uri.host.isNullOrEmpty() ||
            BuildConfig.DEBUG && uri.scheme=="http" && uri.host in listOf("10.0.2.2","localhost","127.0.0.1")
    }
    val signedIn get() = token.isNotEmpty()
    val googleAccount get() = signedIn && profile?.optBoolean("google_account") == true
    var referredInstall by mutableStateOf(false)
        private set
    val coins get() = profile?.optInt("coins") ?: 0
    fun cost(category: String, item: String) = if (item == "original") 0 else when(category) {
        "shrine" -> 7
        "flowers" -> 2
        else -> 0
    }
    fun owns(category: String, item: String) = cost(category, item) == 0 ||
        profile?.optBoolean("unlocked") == true || profile?.optJSONObject("owned")?.optBoolean("$category:$item") == true
    fun owns(selection: ShrineSelection) = selection.values.all { (category, value) ->
        value.split(',').all { owns(category, it) }
    }
    suspend fun readReferral(): Boolean { referral.read(); referredInstall=referral.hasPending; return referredInstall }
    private fun today() = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString()
    val pending get() = prefs.getString("pending_day", "") == today() &&
        (prefs.getString("pending_owner", "") == "" || prefs.getString("pending_owner", "") == profile?.optString("id"))
    private fun cache(value: JSONObject) { profile=value; prefs.edit().putString("profile",value.toString()).apply() }
    private fun localKey() = "completed_days_" + (profile?.optString("id") ?: "guest")
    private var completionRevision by mutableStateOf(0)
    val streakDays: Int get() {
        completionRevision // Observe locally recorded completions before network sync finishes.
        val days = prefs.getStringSet(localKey(), emptySet()).orEmpty().toMutableSet()
        profile?.optJSONArray("days")?.let { values ->
            repeat(values.length()) { days.add(values.getString(it)) }
        }
        return DevotionStreakCounter.count(days, java.time.LocalDate.parse(today()))
    }
    fun recordCompletion() {
        val days = prefs.getStringSet(localKey(), emptySet()).orEmpty() + today()
        prefs.edit().putStringSet(localKey(), days).putString("pending_day", today())
            .putString("pending_owner",profile?.optString("id") ?: "").apply()
        completionRevision++
    }
    private suspend fun request(path: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        check(configured) { "Account service is not connected yet." }
        try {
            val connection = URL(BuildConfig.ACCOUNT_API_URL.trimEnd('/') + "/api/v2/" + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 10000; connection.readTimeout = 15000
                connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val text = (if(status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() } ?: "{}"
                val result = JSONObject(text)
                if(status==401 && signedIn && !path.startsWith("auth/")) withContext(Dispatchers.Main) { clearLocal() }
                check(status in 200..299) { result.optString("error", "Please try again") }
                result
            } finally { connection.disconnect() }
        } catch (e: SocketTimeoutException) {
            throw AccountConnectionFailure(AccountConnectionFailure.Reason.TIMEOUT, e)
        } catch (e: IOException) {
            throw AccountConnectionFailure(AccountConnectionFailure.Reason.CONNECTION, e)
        }
    }
    /** False means Google returned without completing sign-in, including cancellation. */
    suspend fun signInWithGoogle(activityContext: Context, invite: String): Boolean {
        if (!googleConfigured) throw GoogleSignInFailure(GoogleSignInFailure.Reason.UNAVAILABLE)
        val challenge = request("auth/google/challenge", "POST", JSONObject())
        val nonce = challenge.getString("nonce")
        val option = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_CLIENT_ID).setNonce(nonce).build()
        val credentialRequest = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(option).build()
        fun logProviderFailure(stage: String, e: androidx.credentials.exceptions.GetCredentialException) {
            // Keep the original failure classification without logging provider messages,
            // which may contain account information, or any credential data.
            android.util.Log.w("GoogleSignIn", "$stage: ${e.javaClass.simpleName}; type=${e.type}")
        }
        suspend fun getGoogleCredential(): androidx.credentials.Credential {
            try {
                return credentialManager.getCredential(android.content.MutableContextWrapper(activityContext), credentialRequest).credential
            } catch (e: androidx.credentials.exceptions.GetCredentialUnsupportedException) {
                // Only an explicit capability failure permits a compatibility fallback.
                // Cancellation, OAuth configuration and other provider errors are not retried.
                logProviderFailure("Google button unsupported; requesting account picker", e)
                val fallbackOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setNonce(nonce).build()
                val fallbackRequest = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption).build()
                try {
                    return credentialManager.getCredential(android.content.MutableContextWrapper(activityContext), fallbackRequest).credential
                } catch (fallback: androidx.credentials.exceptions.GetCredentialException) {
                    fallback.addSuppressed(e)
                    throw fallback
                }
            }
        }
        val credential = try {
            getGoogleCredential()
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // Providers can also report configuration failures as cancellation.
            // Let the caller show an incomplete sign-in instead of silently doing nothing.
            logProviderFailure("Credential request did not complete", e)
            return false
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            // This also happens when the installed signing certificate is not registered
            // with Google OAuth; it does not prove the device has no Google account.
            logProviderFailure("No Google credential returned", e)
            throw GoogleSignInFailure(GoogleSignInFailure.Reason.UNAVAILABLE, e)
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            logProviderFailure("Credential request failed", e)
            val unavailable = e is androidx.credentials.exceptions.GetCredentialUnsupportedException ||
                e is androidx.credentials.exceptions.GetCredentialProviderConfigurationException
            throw GoogleSignInFailure(
                if (unavailable) GoogleSignInFailure.Reason.UNAVAILABLE else GoogleSignInFailure.Reason.PROVIDER_ERROR,
                e
            )
        }
        if (credential !is androidx.credentials.CustomCredential ||
            credential.type != com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            android.util.Log.w("GoogleSignIn", "Credential response classification: unsupported credential")
            throw GoogleSignInFailure(GoogleSignInFailure.Reason.INVALID_CREDENTIAL)
        }
        val googleToken = try {
            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException) {
            android.util.Log.w("GoogleSignIn", "Credential response classification: ${e.javaClass.simpleName}")
            throw GoogleSignInFailure(GoogleSignInFailure.Reason.INVALID_CREDENTIAL, e)
        }
        val result = request("auth/google", "POST", JSONObject()
            .put("id_token", googleToken.idToken).put("nonce", nonce).put("invite_code", invite.trim())
            .put("install_id", referral.identity().orEmpty()))
        val session = result.getString("token")
        val encrypted = vault.encrypt(session)
        token = session
        prefs.edit().putString("session", encrypted).apply()
        cache(result.getJSONObject("profile"))
        return true
    }
    private suspend fun clearGoogleState() {
        try { credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest()) }
        catch (_: androidx.credentials.exceptions.ClearCredentialException) { /* Local/server logout has completed. */ }
    }
    suspend fun purchase() {
        val key="purchase_"+profile?.optString("id")
        val id=prefs.getString(key,null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString(key,it).apply() }
        cache(request("purchase","POST",JSONObject().put("request_id",id)))
    }
    suspend fun sync() {
        if (!signedIn) return
        if(pending) {
            cache(request("activity/puja", "POST", JSONObject().put("steps",org.json.JSONArray(listOf("LIGHT","BATH","TILAK","FLOWERS","BELL","CONCH","PRASAD","AARTI")))))
            prefs.edit().remove("pending_day").apply()
        } else cache(request("me"))
        if (googleAccount) referral.pending()?.let { claim ->
            val result = request("referrals/install", "POST", claim)
            if (result.optBoolean("processed")) { referral.processed(); referredInstall=false }
        }
        prefs.getStringSet("pending_shares_" + profile?.optString("id"), emptySet()).orEmpty().toList().forEach {
            recordShareOpened(it)
        }
    }
    fun selection(): ShrineSelection {
        val items=profile?.optJSONObject("selections") ?: runCatching {
            JSONObject(prefs.getString("guest_selection", "{}").orEmpty())
        }.getOrDefault(JSONObject())
        return ShrineSelection(items.keys().asSequence().associateWith {items.getString(it)})
    }
    suspend fun saveSelection(selection: ShrineSelection) {
        if (signedIn) cache(request("shrine","PUT",JSONObject().put("selections",JSONObject(selection.values))))
        else {
            check(owns(selection)) { "Sign in to unlock this design with coins." }
            prefs.edit().putString("guest_selection", JSONObject(selection.values).toString()).apply()
        }
    }
    suspend fun unlock(category: String, item: String) {
        cache(request("coins/unlock", "POST", JSONObject().put("category", category).put("item", item)))
    }
    suspend fun createShare(): JSONObject {
        check(googleAccount) { "Sign in with Google before sharing." }
        val key = "share_request_" + profile?.optString("id")
        val id = prefs.getString(key, null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString(key, it).apply() }
        return request("shares", "POST", JSONObject().put("request_id", id))
    }
    fun queueShareOpened(id: String) {
        prefs.edit().remove("share_request_" + profile?.optString("id")).apply()
        val key = "pending_shares_" + profile?.optString("id")
        prefs.edit().putStringSet(key, prefs.getStringSet(key, emptySet()).orEmpty() + id).apply()
    }
    suspend fun recordShareOpened(id: String) {
        val key = "pending_shares_" + profile?.optString("id")
        cache(request("shares/opened", "POST", JSONObject().put("share_id", id)))
        prefs.edit().putStringSet(key, prefs.getStringSet(key, emptySet()).orEmpty() - id).apply()
    }
    private fun clearLocal() { token="";profile=null;prefs.edit().remove("session").remove("profile").remove("pending_day").remove("pending_owner").apply() }
    suspend fun signOut() { request("logout","POST",JSONObject()); clearLocal(); clearGoogleState() }
    suspend fun delete() { request("me","DELETE");clearLocal();clearGoogleState() }
}


private class SessionVault {
    private fun key(): javax.crypto.SecretKey {
        val store=java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("devotion_session",null) as? javax.crypto.SecretKey)?.let { return it }
        return javax.crypto.KeyGenerator.getInstance("AES","AndroidKeyStore").apply {
            init(android.security.keystore.KeyGenParameterSpec.Builder("devotion_session",
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes("GCM").setEncryptionPaddings("NoPadding").build())
        }.generateKey()
    }
    fun encrypt(value:String):String {
        val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,key())
        return android.util.Base64.encodeToString(cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8)),android.util.Base64.NO_WRAP)
    }
    fun decrypt(value:String):String {
        if(value.isEmpty()) return ""
        val bytes=android.util.Base64.decode(value,android.util.Base64.NO_WRAP)
        val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE,key(),javax.crypto.spec.GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        return String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
    }
}
