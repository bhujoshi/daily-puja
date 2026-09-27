package com.worship.nityamandir.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.data.DevotionAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionSheet(account: DevotionAccount, hindi: Boolean, onDismiss: () -> Unit, onCustomize: () -> Unit, canCustomize:Boolean=true) {
    fun tr(hi: String,en: String)=if(hindi) hi else en
    val scope=rememberCoroutineScope();val context=LocalContext.current
    var phone by remember { mutableStateOf("") };var otp by remember {mutableStateOf("")}
    var invite by remember {mutableStateOf("")};var otpRequested by remember {mutableStateOf(false)}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")}
    var profile by remember {mutableStateOf(account.profile)};var deleteConfirm by remember {mutableStateOf(false)}
    fun run(action: suspend () -> Unit) {scope.launch {busy=true;message="";try {action();profile=account.profile} catch(e:Exception) {message=e.message ?: tr("फिर कोशिश करें","Please try again")} finally {profile=account.profile;busy=false}}}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(tr("मेरी पूजा और मेरा मंदिर","My devotion & my temple"),fontSize=24.sp)
            Text(tr("रोज़ की पूरी पूजा हमेशा निःशुल्क है।","Your complete daily puja is always free."),fontSize=17.sp)
            Text(tr("मंदिर, मूर्तियाँ, फूल, शंख, आरती और प्रसाद — एक पैकेज में। अपना पसंदीदा रूप चुनें।","Shrine, idols, flowers, shankh, aarti and prasad — one package. Choose your favourite look."))
            OutlinedButton(onClick=onCustomize,enabled=!busy && canCustomize,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("मंदिर के रूप देखें","Browse temple designs"))}
            if(!account.configured) {
                Text(tr("खाता सेवा अभी जुड़ी नहीं है। आप पूजा जारी रख सकते हैं।","Account service is not connected yet. You can continue your puja."))
            } else if(!account.signedIn) {
                Text(tr("डेमो लॉगिन: OTP 1234 है। कोई SMS नहीं भेजा जाएगा। आपका मोबाइल नंबर और पूजा की प्रगति सहेजी जाएगी।","Demo login: use OTP 1234. No SMS is sent. Your mobile number and puja progress will be saved."))
                OutlinedTextField(phone,{phone=it.filter { c -> c.isDigit() }.take(10);otpRequested=false},label={Text(tr("मोबाइल नंबर","Mobile number"))},prefix={Text("+91 ")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
                if(otpRequested) {
                    OutlinedTextField(otp,{otp=it.filter { c -> c.isDigit() }.take(4)},label={Text(tr("डेमो OTP: 1234","Demo OTP: 1234"))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),singleLine=true,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(invite,{invite=it},label={Text(tr("निमंत्रण कोड (वैकल्पिक)","Invitation code (optional)"))},singleLine=true,modifier=Modifier.fillMaxWidth())
                }
                Button(onClick={run {
                    if(otpRequested) {account.authenticate(phone,otp,invite);otp="";account.sync()}
                    else {account.requestOtp(phone);otpRequested=true}
                }},enabled=!busy && phone.length==10 && (!otpRequested || otp.length==4),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                    Text(if(otpRequested) tr("साइन इन करें","Sign in") else tr("आगे बढ़ें","Continue with mobile"))
                }
            } else {
                Text(profile?.optString("phone")?.takeIf {it.isNotEmpty()}?.let {"+91 $it"} ?: profile?.optString("email").orEmpty())
                Text(tr("लगातार ${profile?.optInt("streak") ?: 0} दिन · लक्ष्य 7 दिन","${profile?.optInt("streak") ?: 0} consecutive days · goal 7 days"),fontSize=20.sp)
                Text(if(profile?.optBoolean("unlocked")==true) tr("पैकेज अनलॉक है। अपना मंदिर सजाएँ।","Package unlocked. Customize your temple.") else tr("7 दिन पूजा करें या एक मित्र को बुलाएँ जो अपनी पहली पूजा पूरी करे।","Complete puja for 7 consecutive days, or invite one friend who completes their first puja."))
                Text(tr("एक दिन छूट जाए तो फिर शुरू करें। अनलॉक किया पैकेज आपका रहेगा।","Missed a day? Begin again. An unlocked package stays yours."))
                Button(onClick={run {account.sync()}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("प्रगति सहेजें / ताज़ा करें","Save / refresh progress"))}
                OutlinedButton(onClick={
                    val text=tr("पवित्र मंदिर पर मेरे साथ पूजा करें। निमंत्रण कोड: ","Join me for puja on Pavitra Mandir. Invitation code: ")+profile?.optString("invite_code")+"\nhttps://play.google.com/store/apps/details?id=com.pavitramandir.app"
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),tr("निमंत्रण भेजें","Share invitation")))
                },enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("मित्र को आमंत्रित करें","Invite a friend"))}
                if(profile?.optBoolean("mock_payments")==true && profile?.optBoolean("unlocked")!=true) {
                    Button(onClick={run {account.purchase()}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {Text(tr("डेमो भुगतान से अनलॉक करें · ₹0","Unlock with mock payment · ₹0"))}
                    Text(tr("यह परीक्षण भुगतान है। कोई पैसा नहीं कटेगा।","Test payment only. No money will be charged."))
                }
                Text(tr("पूजा के दिन:","Puja days:")+" "+(profile?.optJSONArray("days")?.let { days -> (0 until days.length()).map { days.getString(it) }.takeLast(14).joinToString(", ") }?.ifEmpty {"—"} ?: "—"))
                Text(tr("गतिविधि और भुगतान इतिहास","Activity & payment history"),fontSize=20.sp)
                val history=profile?.optJSONArray("history")
                if(history==null || history.length()==0) Text(tr("अभी कोई गतिविधि नहीं","No activity yet"))
                else for(i in history.length()-1 downTo maxOf(0,history.length()-20)) {
                    val event=history.getJSONObject(i)
                    val label=when(event.optString("kind")) {
                        "mock_purchase" -> tr("डेमो भुगतान सफल · ₹0","Mock payment successful · ₹0")
                        "puja_completed" -> tr("पूजा पूरी हुई","Puja completed")
                        "shrine_saved" -> tr("मंदिर का रूप सहेजा","Temple design saved")
                        "streak_unlock" -> tr("7 दिन से पैकेज अनलॉक","Package unlocked by 7-day streak")
                        "referral_unlock" -> tr("निमंत्रण से पैकेज अनलॉक","Package unlocked by invitation")
                        else -> tr("खाता बनाया","Account created")
                    }
                    Text(label+" · "+event.optString("at").take(10))
                }
                if(account.pending) Text(tr("आज की पूजा सहेजने के लिए इंटरनेट से ताज़ा करें।","Today’s puja is waiting to sync. Refresh before midnight India time to credit today."))
                Text(tr("सहेजी हुई प्रगति दिखाई जा रही है। इंटरनेट से ताज़ा करें।","Showing saved progress. Refresh with internet to sync."))

                TextButton(onClick={run {account.signOut()}},enabled=!busy) {Text(tr("साइन आउट","Sign out"))}
                TextButton(onClick={deleteConfirm=true},enabled=!busy) {Text(tr("खाता और गतिविधि मिटाएँ","Delete account and activity"))}
            }
            if(busy) CircularProgressIndicator()
            if(message.isNotEmpty()) Text(message,color=MaterialTheme.colorScheme.error)
            TextButton(onClick=onDismiss) {Text(tr("पूजा पर वापस जाएँ","Return to puja"))}
        }
    }
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},title={Text(tr("खाता मिटाएँ?","Delete account?"))},text={Text(tr("पूजा का इतिहास और अनलॉक हटा दिए जाएँगे।","Your puja history and unlock will be removed."))},confirmButton={TextButton(onClick={deleteConfirm=false;run {account.delete()}}){Text(tr("मिटाएँ","Delete"))}},dismissButton={TextButton(onClick={deleteConfirm=false}){Text(tr("वापस","Cancel"))}})
}
