package com.worship.nityamandir.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.worship.nityamandir.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Account identity with a Keystore-protected session and durable profile snapshot. */
class DevotionAccount(context: Context) {
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
    }
    suspend fun signInWithGoogle(activityContext: Context, invite: String) {
        check(googleConfigured) { "Google sign-in is not configured yet." }
        val challenge = request("auth/google/challenge", "POST", JSONObject())
        val nonce = challenge.getString("nonce")
        val option = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_CLIENT_ID).setNonce(nonce).build()
        val credentialRequest = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(option).build()
        suspend fun getGoogleCredential(): androidx.credentials.Credential {
            try {
                return credentialManager.getCredential(android.content.MutableContextWrapper(activityContext), credentialRequest).credential
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                throw e
            } catch (_: androidx.credentials.exceptions.GetCredentialException) {
                // Older Play services cannot parse the dedicated button option.
                // The account-picker request supports new and returning Google accounts.
                val fallbackOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setNonce(nonce).build()
                val fallbackRequest = androidx.credentials.GetCredentialRequest.Builder()
                    .addCredentialOption(fallbackOption).build()
                return credentialManager.getCredential(android.content.MutableContextWrapper(activityContext), fallbackRequest).credential
            }
        }
        val credential = try {
            getGoogleCredential()
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            return
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            throw IllegalStateException("No Google account is available. Add an account on this device and update Google Play services.")
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            // Record only the exception type; never log credentials or account data.
            android.util.Log.w("GoogleSignIn", "Credential request failed: ${e.type}")
            throw IllegalStateException("Google sign-in could not start. Update Google Play services and check this app's Google OAuth configuration.")
        }
        check(credential is androidx.credentials.CustomCredential &&
            credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google sign-in returned an unsupported credential."
        }
        val googleToken = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
        val result = request("auth/google", "POST", JSONObject()
            .put("id_token", googleToken.idToken).put("nonce", nonce).put("invite_code", invite.trim()))
        val session = result.getString("token")
        val encrypted = vault.encrypt(session)
        token = session
        prefs.edit().putString("session", encrypted).apply()
        cache(result.getJSONObject("profile"))
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
    }
    fun selection(): ShrineSelection {
        val items=profile?.optJSONObject("selections") ?: return ShrineSelection()
        return ShrineSelection(items.keys().asSequence().associateWith {items.getString(it)})
    }
    suspend fun saveSelection(selection: ShrineSelection) {
        cache(request("shrine","PUT",JSONObject().put("selections",JSONObject(selection.values))))
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
