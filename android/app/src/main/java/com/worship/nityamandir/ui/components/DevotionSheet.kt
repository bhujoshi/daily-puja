package com.worship.nityamandir.ui.components

import com.worship.nityamandir.data.WhatsAppShare
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

enum class DevotionPage { PROFILE, PACKAGE, LOGIN, SHARE }

@Composable
fun DevotionStreak(streak:Int, hindi:Boolean) {
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
            Text(if(hindi) "हर दिन पूरी पूजा पर 1 सिक्का। कमाए सिक्के बने रहते हैं।" else "Earn 1 coin for each day you complete puja. Your coins stay with you.",fontSize=14.sp)
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
    var shareAfterLogin by remember {mutableStateOf(page==DevotionPage.SHARE)}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")}
    var deleteConfirm by remember {mutableStateOf(false)}
    val profile=account.profile
    fun run(action:suspend ()->Unit) {scope.launch {busy=true;message="";try {action()} catch(e:Exception) {message=e.message ?: tr("फिर कोशिश करें","Please try again")} finally {busy=false}}}
    suspend fun share() {
        val invitation=account.createShare()
        val text=tr("पवित्र मंदिर पर मेरे साथ पूजा करें। डाउनलोड करें और Google से साइन इन करें:\n", "Join me for daily puja on Pavitra Mandir. Install and sign in with Google:\n")+invitation.getString("url")
        WhatsAppShare.open(context,text)
        account.queueShareOpened(invitation.getString("share_id"))
        try { account.recordShareOpened(invitation.getString("share_id")) }
        catch (_: Exception) { message=tr("WhatsApp खुल गया। शेयर गिनती अगली बार सहेजेंगे।", "WhatsApp opened. Your share count will sync when connected.") }
    }
    LaunchedEffect(page) {
        if(page==DevotionPage.SHARE && account.googleAccount) run {shareAfterLogin=false;share()}
    }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Color(0xFFFFF7EC)) {
        Column(Modifier.fillMaxWidth()) {
            if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,onMusic,Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Column(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(if(destination==DevotionPage.PACKAGE) Icons.Outlined.AutoAwesome else Icons.Outlined.AccountCircle,null,tint=RitualGold,modifier=Modifier.size(32.dp))
                Text(when(destination) {DevotionPage.PACKAGE -> tr("सिक्के और अनलॉक","Coins & unlocks");DevotionPage.SHARE -> tr("मित्र को आमंत्रित करें","Invite a friend");DevotionPage.LOGIN -> tr("लॉग इन","Log in");else -> tr("मेरी प्रोफ़ाइल","My profile")},fontSize=25.sp,fontWeight=FontWeight.SemiBold)
            }
            Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(tr("${account.coins} सिक्के", "${account.coins} coins"),fontSize=28.sp,fontWeight=FontWeight.SemiBold)
                    Text(tr("पूरी पूजा · दिन में 1 सिक्का\nमित्र का नया इंस्टॉल और Google लॉगिन · 1 सिक्का", "Daily puja · 1 coin per day\nFriend installs and signs in with Google · 1 coin each"))
                    Text(tr("मंदिर · 7 सिक्के प्रति रूप   फूल · 2 सिक्के प्रति किस्म", "Shrine · 7 coins each   Flower · 2 coins each"),fontSize=14.sp)
                    Text(tr("शंख, आरती, प्रसाद, मूर्तियाँ और दीपक मुफ़्त हैं।", "Shankh, aarti, prasad, idols and oil lamps are free."),fontSize=14.sp)
                    if(account.signedIn) Text(tr("WhatsApp खुले: ${profile?.optInt("share_count") ?: 0} · नए मित्र: ${profile?.optInt("referrals") ?: 0}", "WhatsApp opens: ${profile?.optInt("share_count") ?: 0} · New friends: ${profile?.optInt("referrals") ?: 0}"),fontSize=14.sp)
                }
            }
            RitualChoice(tr("WhatsApp पर साझा करें", "Share on WhatsApp"), false, {
                if(account.googleAccount) run {share()}
                else {shareAfterLogin=true;destination=DevotionPage.LOGIN}
            },Modifier.fillMaxWidth(),enabled=!busy)
            if(shareAfterLogin && !account.googleAccount) Text(tr("साझा करने और सिक्के सहेजने के लिए पहले Google से साइन इन करें।", "Sign in with Google first to share and keep your coins."))
            if(destination==DevotionPage.PACKAGE) {
                DevotionStreak(account.streakDays,hindi)
                RitualChoice(tr("मेरा मंदिर सजाएँ","Customize my temple"),false,onCustomize,Modifier.fillMaxWidth(),enabled=!busy && canCustomize)
                if(!account.signedIn) OutlinedButton(onClick={destination=DevotionPage.LOGIN},modifier=Modifier.fillMaxWidth()) {Text(tr("सिक्के सहेजने के लिए लॉग इन करें","Log in to keep your coins"))}
            } else if(account.signedIn && destination!=DevotionPage.LOGIN) {
                Text(profile?.optString("phone")?.takeIf {it.isNotEmpty()}?.let {"+91 $it"} ?: profile?.optString("email").orEmpty(),fontSize=18.sp)
                OutlinedButton(onClick={run {account.signOut();destination=DevotionPage.LOGIN}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Icon(Icons.Outlined.Logout,null);Spacer(Modifier.width(8.dp));Text(tr("लॉग आउट","Log out"))}
                DevotionStreak(account.streakDays,hindi)
                Text(tr("एक दिन छूटने पर भी आपके सिक्के और अनलॉक बने रहते हैं।","Your coins and unlocked items stay yours, even if you miss a day."),fontSize=14.sp)
                TextButton(onClick={destination=DevotionPage.PACKAGE}) {Icon(Icons.Outlined.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(tr("सिक्के और अनलॉक देखें","View coins & unlocks"))}
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
                        "puja_completed" -> tr("पूजा पूरी हुई · +1 सिक्का","Puja completed · +1 coin")
                        "shrine_saved" -> tr("मंदिर सहेजा","Temple design saved")
                        "streak_unlock","referral_unlock","mock_purchase","package_granted" -> tr("पैकेज अनलॉक","Package unlocked")
                        "referral_coin" -> tr("मित्र का इंस्टॉल · +1 सिक्का","Friend installed · +1 coin")
                        "coin_unlock" -> tr("सिक्कों से अनलॉक","Unlocked with coins")
                        "whatsapp_opened" -> tr("WhatsApp निमंत्रण खुला","WhatsApp invitation opened")
                        else -> tr("खाता बनाया","Account created")
                    }
                    Text("$label · ${event.optString("at").take(10)}",fontSize=14.sp)
                }
                TextButton(onClick={deleteConfirm=true},enabled=!busy) {Text(tr("खाता और गतिविधि मिटाएँ","Delete account and activity"))}
            } else {
                if(account.referredInstall) Text(tr("Google से साइन इन करके अपने मित्र का निमंत्रण पूरा करें। पूजा बिना लॉगिन के भी मुफ़्त है।", "Sign in with Google to credit your friend's invitation. Puja is free even without signing in."))
                Text(tr("अपनी पूजा श्रृंखला और मंदिर के रूप सहेजें।","Save your puja streak and your temple designs."))
                if(!account.configured) Text(tr("खाता सेवा अभी जुड़ी नहीं है। पूजा जारी रख सकते हैं।","Account service is not connected yet. You can continue your puja."))
                else {
                    if (!account.googleConfigured) {
                        Text(tr("Google लॉगिन अभी जुड़ा नहीं है। पूजा जारी रख सकते हैं।", "Google sign-in is not connected yet. You can continue your puja."))
                    } else {
                        OutlinedButton(onClick={run {
                            account.signInWithGoogle(context, "")
                            if (account.signedIn) {
                                destination=DevotionPage.PROFILE
                                account.sync()
                                if(shareAfterLogin) {shareAfterLogin=false;share()}
                            }
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
