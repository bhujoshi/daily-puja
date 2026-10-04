package com.worship.nityamandir.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import com.worship.nityamandir.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.data.DevotionAccount
import com.worship.nityamandir.engine.BhajanPlayer
import kotlinx.coroutines.launch

enum class DevotionPage { PROFILE, PACKAGE, LOGIN }

@Composable
fun DevotionStreak(streak:Int, hindi:Boolean, unlocked:Boolean=false) {
    val completed=streak.coerceIn(0,7)
    Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.LocalFireDepartment,null,tint=RitualGold)
                Text(if(hindi) "$streak दिन की पूजा श्रृंखला" else "$streak day puja streak",fontWeight=FontWeight.SemiBold,fontSize=20.sp)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                repeat(7) {day ->
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.size(32.dp).background(if(day<completed) RitualGold else Color.White.copy(alpha=.7f),CircleShape),contentAlignment=Alignment.Center) {
                            if(day<completed) Icon(Icons.Outlined.Check,null,tint=Color.White,modifier=Modifier.size(18.dp))
                            else Text("${day+1}",color=RitualInk,fontSize=13.sp)
                        }
                        Text(if(hindi) "दिन ${day+1}" else "Day ${day+1}",fontSize=10.sp)
                    }
                }
            }
            Text(if(unlocked) {if(hindi) "आपका मंदिर पैकेज अनलॉक है" else "Your temple package is unlocked"}
                else if(hindi) "${7-completed} और लगातार पूजा के दिन — पैकेज अनलॉक करें" else "${7-completed} more consecutive puja days to unlock",fontSize=14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionSheet(account:DevotionAccount,hindi:Boolean,onDismiss:()->Unit,onCustomize:()->Unit,canCustomize:Boolean=true,page:DevotionPage=DevotionPage.PROFILE,
    player:BhajanPlayer,onMusic:()->Unit) {
    fun tr(hi:String,en:String)=if(hindi) hi else en
    val scope=rememberCoroutineScope();val context=LocalContext.current
    var destination by remember(page) {mutableStateOf(page)}
    var invite by remember {mutableStateOf("")}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")}
    var deleteConfirm by remember {mutableStateOf(false)}
    val profile=account.profile
    val unlocked=profile?.optBoolean("unlocked")==true
    fun run(action:suspend ()->Unit) {scope.launch {busy=true;message="";try {action()} catch(e:Exception) {message=e.message ?: tr("फिर कोशिश करें","Please try again")} finally {busy=false}}}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Color(0xFFFFF7EC)) {
        Column(Modifier.fillMaxWidth()) {
            if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,onMusic,Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Column(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(if(destination==DevotionPage.PACKAGE) Icons.Outlined.AutoAwesome else Icons.Outlined.AccountCircle,null,tint=RitualGold,modifier=Modifier.size(32.dp))
                Text(when(destination) {DevotionPage.PACKAGE -> tr("मंदिर पैकेज","Temple package");DevotionPage.LOGIN -> tr("लॉग इन","Log in");else -> tr("मेरी प्रोफ़ाइल","My profile")},fontSize=25.sp,fontWeight=FontWeight.SemiBold)
            }
            if(destination==DevotionPage.PACKAGE) {
                Surface(color=if(unlocked) Color(0xFFE4EFDF) else Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Icon(if(unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,null,tint=RitualGold)
                        Text(if(unlocked) tr("आपका अपना मंदिर, अनलॉक","Your personal temple, unlocked") else tr("अपनी पूजा से अनलॉक करें","Unlock through your daily puja"),fontSize=22.sp)
                        Text(tr("मंदिर, मूर्तियाँ, फूल, दीपक, शंख और प्रसाद के सभी रूप।","All shrine designs, idols, flowers, lamps, shankh and prasad."))
                        Text(tr("रोज़ की पूरी पूजा हमेशा निःशुल्क है।","Your complete daily puja is always free."),fontSize=14.sp)
                    }
                }
                DevotionStreak(account.streakDays,hindi,unlocked)
                if(!unlocked) {
                    Text(tr("या एक मित्र को आमंत्रित करें","Or invite one friend"),fontWeight=FontWeight.SemiBold)
                    Text(tr("मित्र की पहली पूरी पूजा पर पैकेज अनलॉक होगा।","The package unlocks when your friend completes their first puja."))
                }
                Button(onClick=onCustomize,enabled=!busy && canCustomize,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                    Icon(Icons.Outlined.TempleHindu,null);Spacer(Modifier.width(8.dp))
                    Text(if(unlocked) tr("मेरा मंदिर सजाएँ","Customize my temple") else tr("मंदिर के रूप देखें","Explore temple designs"))
                }
                if(account.signedIn && !unlocked) {
                    OutlinedButton(onClick={
                        val text=tr("पवित्र मंदिर पर मेरे साथ पूजा करें। निमंत्रण कोड: ","Join me for puja on Pavitra Mandir. Invitation code: ")+profile?.optString("invite_code")+"\nhttps://play.google.com/store/apps/details?id=com.pavitramandir.app"
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),tr("निमंत्रण भेजें","Share invitation")))
                    },modifier=Modifier.fillMaxWidth(),enabled=!busy) {Icon(Icons.Outlined.PersonAdd,null);Spacer(Modifier.width(8.dp));Text(tr("मित्र को आमंत्रित करें","Invite a friend"))}
                    if(profile?.optBoolean("mock_payments")==true) {
                        OutlinedButton(onClick={run {account.purchase()}},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text(tr("डेमो अनलॉक · ₹0","Demo unlock · ₹0"))}
                        Text(tr("परीक्षण भुगतान। कोई पैसा नहीं कटेगा।","Test payment. No money will be charged."),fontSize=12.sp)
                    }
                }
                if(!account.signedIn) OutlinedButton(onClick={destination=DevotionPage.LOGIN},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Login,null);Spacer(Modifier.width(8.dp));Text(tr("प्रगति सहेजने के लिए लॉग इन करें","Log in to save your progress"))}
            } else if(account.signedIn) {
                Text(profile?.optString("phone")?.takeIf {it.isNotEmpty()}?.let {"+91 $it"} ?: profile?.optString("email").orEmpty(),fontSize=18.sp)
                OutlinedButton(onClick={run {account.signOut();destination=DevotionPage.LOGIN}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Icon(Icons.Outlined.Logout,null);Spacer(Modifier.width(8.dp));Text(tr("लॉग आउट","Log out"))}
                DevotionStreak(account.streakDays,hindi,unlocked)
                Text(tr("एक दिन छूट जाए तो फिर शुरू करें। अनलॉक किया पैकेज आपका रहेगा।","Missed a day? Begin again. An unlocked package stays yours."),fontSize=14.sp)
                TextButton(onClick={destination=DevotionPage.PACKAGE}) {Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(tr("मंदिर पैकेज देखें","View temple package"))}
                Text(tr("हाल की पूजा","Recent pujas"),fontSize=20.sp)
                val days=profile?.optJSONArray("days")
                val dates=(0 until (days?.length() ?: 0)).map {days!!.getString(it)}.toSet()
                val today=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    (6 downTo 0).forEach {ago -> val day=today.minusDays(ago.toLong());val done=day.toString() in dates
                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Icon(if(done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,null,tint=if(done) RitualGold else Color.Gray)
                            Text("${day.dayOfMonth}/${day.monthValue}",fontSize=11.sp)
                        }
                    }
                }
                Text(tr("गतिविधि इतिहास","Activity history"),fontSize=20.sp)
                val history=profile?.optJSONArray("history")
                if(history==null || history.length()==0) Text(tr("पहली पूजा से शुरुआत करें","Begin with your first puja"))
                else for(i in history.length()-1 downTo maxOf(0,history.length()-8)) {
                    val event=history.getJSONObject(i)
                    val label=when(event.optString("kind")) {
                        "puja_completed" -> tr("पूजा पूरी हुई","Puja completed")
                        "shrine_saved" -> tr("मंदिर सहेजा","Temple design saved")
                        "streak_unlock","referral_unlock","mock_purchase" -> tr("पैकेज अनलॉक","Package unlocked")
                        else -> tr("खाता बनाया","Account created")
                    }
                    Text("$label · ${event.optString("at").take(10)}",fontSize=14.sp)
                }
                TextButton(onClick={deleteConfirm=true},enabled=!busy) {Text(tr("खाता और गतिविधि मिटाएँ","Delete account and activity"))}
            } else {
                Text(tr("अपनी पूजा श्रृंखला और मंदिर के रूप सहेजें।","Save your puja streak and your temple designs."))
                if(!account.configured) Text(tr("खाता सेवा अभी जुड़ी नहीं है। पूजा जारी रख सकते हैं।","Account service is not connected yet. You can continue your puja."))
                else {
                    if (!account.googleConfigured) {
                        Text(tr("Google लॉगिन अभी जुड़ा नहीं है। पूजा जारी रख सकते हैं।", "Google sign-in is not connected yet. You can continue your puja."))
                    } else {
                        OutlinedTextField(invite,{invite=it},label={Text(tr("निमंत्रण कोड (वैकल्पिक)","Invitation code (optional)"))},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())
                        OutlinedButton(onClick={run {
                            account.signInWithGoogle(context,invite)
                            if (account.signedIn) { destination=DevotionPage.PROFILE;account.sync() }
                        }},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),border=BorderStroke(1.dp,Color(0xFF747775)),colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.White,contentColor=Color(0xFF1F1F1F))) {
                            Image(painterResource(R.drawable.google_sign_in_logo),contentDescription=null,modifier=Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(tr("Google से साइन इन करें","Sign in with Google"))
                        }
                    }
                }
            }
            if(account.signedIn) {
                if(account.pending) Text(tr("आज की पूजा सहेजने के लिए ताज़ा करें।","Today's puja is waiting to sync."),color=RitualGold)
                OutlinedButton(onClick={run {account.sync()}},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(tr("प्रगति ताज़ा करें","Refresh progress"))}
            }
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(message.isNotEmpty()) Text(message,color=MaterialTheme.colorScheme.error)
            TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) {Text(tr("पूजा पर वापस जाएँ","Return to puja"))}
            }
        }
    }
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},title={Text(tr("खाता मिटाएँ?","Delete account?"))},text={Text(tr("पूजा का इतिहास और अनलॉक हटा दिए जाएँगे।","Your puja history and unlock will be removed."))},confirmButton={TextButton(onClick={deleteConfirm=false;run {account.delete()}}){Text(tr("मिटाएँ","Delete"))}},dismissButton={TextButton(onClick={deleteConfirm=false}){Text(tr("वापस","Cancel"))}})
}
