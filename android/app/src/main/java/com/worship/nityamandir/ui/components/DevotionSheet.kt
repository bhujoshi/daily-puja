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
import com.worship.nityamandir.data.ShrineCatalog
import com.worship.nityamandir.engine.BhajanPlayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

enum class DevotionPage { PROFILE, PACKAGE, LOGIN, SHARE }

@Composable
private fun MandirSajayeEntry(account:DevotionAccount,catalog:ShrineCatalog,hindi:Boolean,onCustomize:()->Unit,enabled:Boolean) {
    fun tr(hi:String,en:String)=if(hindi) hi else en
    val newUnlocks=account.newUnlocks(catalog)
    val availableUnlocks=account.availableUnlocks(catalog)
    fun names(items:List<com.worship.nityamandir.data.ShrineUnlock>)=items.take(2).joinToString(" · ") {if(hindi) it.option.hi else it.option.en}+
        if(items.size>2) tr(" और ${items.size-2}"," + ${items.size-2} more") else ""
    Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            when {
                newUnlocks.isNotEmpty() -> {
                    Text(tr("${newUnlocks.size} नए अनलॉक आपके लिए तैयार हैं",if(newUnlocks.size==1) "1 newly unlocked item is ready" else "${newUnlocks.size} newly unlocked items are ready"),fontWeight=FontWeight.SemiBold)
                    Text(names(newUnlocks),fontSize=14.sp)
                }
                availableUnlocks.isEmpty() && account.signedIn -> Text(tr("अपने मंदिर का रूप चुनें","Choose your mandir’s look"),fontWeight=FontWeight.SemiBold)
                !account.signedIn -> {
                    Text(tr("मुफ़्त वस्तुओं से मंदिर सजाएँ","Decorate with free items"),fontWeight=FontWeight.SemiBold)
                    Text(tr("मूर्तियाँ, दीपक, शंख, आरती और प्रसाद चुन सकते हैं।","Choose idols, oil lamps, shankh, aarti and prasad."),fontSize=14.sp)
                }
            }
            if(availableUnlocks.isNotEmpty()) {
                Text(tr("आपके सिक्कों से ${availableUnlocks.size} नई वस्तुएँ अनलॉक हो सकती हैं",if(availableUnlocks.size==1) "Your coins can unlock 1 new item" else "Your coins can unlock ${availableUnlocks.size} new items"),fontWeight=FontWeight.SemiBold)
                Text(names(availableUnlocks),fontSize=14.sp)
            } else if(account.signedIn && newUnlocks.isEmpty()) {
                val nextCost=catalog.categories.flatMap {category -> category.options.map {category.id to it}}
                    .filter {(category,option) -> !account.owns(category,option.id)}
                    .map {(category,option) -> account.cost(category,option.id)}.filter {it>0}.minOrNull()
                if(nextCost!=null) Text(tr(
                    "अगली वस्तु के लिए ${nextCost-account.coins} और सिक्के कमाएँ। रोज़ पूजा करें या मित्र को आमंत्रित करें।",
                    "Earn ${nextCost-account.coins} more coins for your next item. Complete daily puja or invite a friend."),fontSize=14.sp)
                else Text(tr("आपकी सभी वस्तुएँ सजाने के लिए तैयार हैं।","All your items are ready to use."),fontSize=14.sp)
            }
            RitualChoice(tr("मंदिर सजाएँ","Mandir Sajaye"),false,onCustomize,Modifier.fillMaxWidth(),enabled=enabled)
        }
    }
}

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
    player:BhajanPlayer,onMusic:()->Unit,onSignedIn:()->Unit={}) {
    fun tr(hi:String,en:String)=if(hindi) hi else en
    val scope=rememberCoroutineScope();val context=LocalContext.current
    val catalog=remember(context) {ShrineCatalog(context)}
    var destination by remember(page) {mutableStateOf(page)}
    var shareAfterLogin by remember(page) {mutableStateOf(page==DevotionPage.SHARE)}
    var busy by remember {mutableStateOf(false)}
    var refreshing by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf("")}
    var messageIsError by remember {mutableStateOf(false)}
    var busyLabel by remember {mutableStateOf("")}
    var deleteConfirm by remember {mutableStateOf(false)}
    val profile=account.profile
    fun errorMessage(error: Exception): String = when (error) {
        is com.worship.nityamandir.data.MissingWhatsAppException -> tr(
            "निमंत्रण साझा करने के लिए WhatsApp या WhatsApp Business इंस्टॉल करें।",
            "Install WhatsApp or WhatsApp Business to share your invitation.")
        is com.worship.nityamandir.data.GoogleSignInFailure -> when(error.reason) {
            com.worship.nityamandir.data.GoogleSignInFailure.Reason.UNAVAILABLE -> tr(
                "Google साइन इन अभी उपलब्ध नहीं है। थोड़ी देर बाद फिर कोशिश करें।",
                "Google sign-in is currently unavailable. Please try again later.")
            com.worship.nityamandir.data.GoogleSignInFailure.Reason.PROVIDER_ERROR -> tr(
                "Google साइन इन शुरू नहीं हो पाया। फिर कोशिश करें या Google Play सेवाएँ अपडेट करें।",
                "Google sign-in could not start. Try again or update Google Play services.")
            com.worship.nityamandir.data.GoogleSignInFailure.Reason.INVALID_CREDENTIAL -> tr(
                "Google साइन इन पूरा नहीं हो पाया। कृपया फिर कोशिश करें।",
                "Google sign-in could not be completed. Please try again.")
        }
        is com.worship.nityamandir.data.AccountConnectionFailure -> tr(
            "कनेक्शन नहीं हो पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
            "Could not connect. Check your internet connection and try again.")
        is java.io.IOException -> tr(
            "कनेक्शन नहीं हो पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
            "Could not connect. Check your internet connection and try again.")
        else -> error.message ?: tr("फिर कोशिश करें", "Please try again")
    }
    fun run(label: String = tr("कृपया प्रतीक्षा करें…", "Please wait…"), action:suspend ()->Unit) {
        if (busy) return
        busy=true; busyLabel=label; message=""; messageIsError=false
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message=errorMessage(error); messageIsError=true }
            finally { busy=false }
        }
    }
    suspend fun share() {
        val invitation=account.createShare()
        val text=tr("पवित्र मंदिर पर मेरे साथ पूजा करें। डाउनलोड करें और Google से साइन इन करें:\n", "Join me for daily puja on Pavitra Mandir. Install and sign in with Google:\n")+invitation.getString("url")
        WhatsAppShare.open(context,text)
        account.queueShareOpened(invitation.getString("share_id"))
        message=tr("आपका निमंत्रण WhatsApp में खुल गया।", "WhatsApp opened with your invitation.")
        messageIsError=false
        try { account.recordShareOpened(invitation.getString("share_id")) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message=tr("WhatsApp खुल गया। शेयर गिनती इंटरनेट मिलने पर सहेजेंगे।", "WhatsApp opened. Your share count will sync when connected.") }
    }
    fun signIn() = run(tr("Google से साइन इन हो रहा है…", "Signing in with Google…")) {
        if (!account.signInWithGoogle(context, "")) {
            message=tr("साइन इन पूरा नहीं हुआ। तैयार हों तो फिर कोशिश करें।", "Sign-in was not completed. Try again when you’re ready.")
        } else {
            val continueToShare=shareAfterLogin || destination==DevotionPage.SHARE
            shareAfterLogin=false
            destination=if(continueToShare) DevotionPage.SHARE else if(page==DevotionPage.PACKAGE) DevotionPage.PACKAGE else DevotionPage.PROFILE
            message=tr("साइन इन हो गया।", "You’re signed in.")
            // The requested WhatsApp handoff must not wait on optional progress sync.
            if(continueToShare) {
                busyLabel=tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")
                try { share() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { message=errorMessage(error); messageIsError=true }
            }
            try { account.sync() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if(!continueToShare) message=tr("साइन इन हो गया। इंटरनेट मिलने पर प्रगति सहेजेंगे।", "You’re signed in. Your progress will sync when connected.")
            }
            if(!continueToShare) onSignedIn()
        }
    }
    LaunchedEffect(page) {
        if(page==DevotionPage.SHARE && account.googleAccount) run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {shareAfterLogin=false;share()}
        else if(account.signedIn && page in listOf(DevotionPage.PROFILE,DevotionPage.PACKAGE)) {
            refreshing=true
            try {account.sync()}
            catch(cancelled:CancellationException) {throw cancelled}
            catch(error:Exception) {message=errorMessage(error);messageIsError=true}
            finally {refreshing=false}
        }
    }
    val sharing=destination==DevotionPage.SHARE || shareAfterLogin
    val signingIn=destination==DevotionPage.LOGIN || (!account.signedIn && destination==DevotionPage.PROFILE)
    val focused=sharing || signingIn

    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Color(0xFFFFF7EC)) {
        Column(Modifier.fillMaxWidth()) {
            if(player.current!=null || player.loading) BhajanMiniPlayer(player,hindi,onMusic,Modifier.padding(horizontal=24.dp,vertical=8.dp))
            Column(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(if(sharing) Icons.Outlined.Share else if(destination==DevotionPage.PACKAGE) Icons.Outlined.AutoAwesome else Icons.Outlined.AccountCircle,null,tint=RitualGold,modifier=Modifier.size(32.dp))
                Text(if(sharing) tr("मित्र को आमंत्रित करें","Invite a friend") else if(signingIn) tr("साइन इन करें","Sign in") else if(destination==DevotionPage.PACKAGE) tr("सिक्के कमाएँ","Earn coins") else tr("मेरी प्रोफ़ाइल","My profile"),fontSize=25.sp,fontWeight=FontWeight.SemiBold)
            }
            if(sharing) {
                Text(tr("मित्र को अपने साथ रोज़ की पूजा के लिए आमंत्रित करें।", "Invite a friend to join your daily puja."))
                Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(!account.googleAccount) Text(tr("1. Google से साइन इन करें\n2. WhatsApp में मित्र चुनकर निमंत्रण भेजें", "1. Sign in with Google\n2. Choose a friend in WhatsApp and send your invitation"))
                        else Text(tr("WhatsApp में मित्र चुनें और भेजें पर टैप करें।", "Choose a friend in WhatsApp and tap Send."))
                        Text(tr("मित्र पहली बार इंस्टॉल करके Google से साइन इन करे तो आपको 1 सिक्का मिलेगा।", "Earn 1 coin when a new friend installs and signs in with Google."),fontSize=14.sp)
                    }
                }
                if(account.googleAccount) {
                    RitualChoice(tr("WhatsApp पर साझा करें", "Share on WhatsApp"),false,{run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {share()}},Modifier.fillMaxWidth(),enabled=!busy)
                    TextButton(onClick={destination=DevotionPage.PROFILE;message=""},enabled=!busy) {Text(tr("मेरी प्रोफ़ाइल देखें", "View my profile"))}
                }
            }
            if(!focused) {
            Surface(color=Color(0xFFF2E5D2),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(tr("${account.coins} सिक्के", "${account.coins} coins"),fontSize=28.sp,fontWeight=FontWeight.SemiBold)
                    Text(tr("पूरी पूजा · दिन में 1 सिक्का\nमित्र का नया इंस्टॉल और Google लॉगिन · 1 सिक्का", "Daily puja · 1 coin per day\nFriend installs and signs in with Google · 1 coin each"))
                    val shrineCost=account.cost("shrine","marble")
                    val flowerCost=account.cost("flowers","rose")
                    Text(tr("मंदिर · $shrineCost सिक्के प्रति रूप   फूल · $flowerCost सिक्के प्रति किस्म", "Shrine · $shrineCost coins each   Flower · $flowerCost coins each"),fontSize=14.sp)
                    Text(tr("शंख, आरती, प्रसाद, मूर्तियाँ और दीपक मुफ़्त हैं।", "Shankh, aarti, prasad, idols and oil lamps are free."),fontSize=14.sp)
                    if(account.signedIn) Text(tr("WhatsApp खुले: ${profile?.optInt("share_count") ?: 0} · नए मित्र: ${profile?.optInt("referrals") ?: 0}", "WhatsApp opens: ${profile?.optInt("share_count") ?: 0} · New friends: ${profile?.optInt("referrals") ?: 0}"),fontSize=14.sp)
                }
            }
            MandirSajayeEntry(account,catalog,hindi,onCustomize,enabled=!busy && canCustomize)
            RitualChoice(tr("WhatsApp पर साझा करें", "Share on WhatsApp"), false, {
                destination=DevotionPage.SHARE
                if(account.googleAccount) run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {share()}
                else {shareAfterLogin=true;message=""}
            },Modifier.fillMaxWidth(),enabled=!busy)
            }
            if(destination==DevotionPage.PACKAGE && !focused) {
                DevotionStreak(account.streakDays,hindi)
                if(!account.signedIn) OutlinedButton(onClick={destination=DevotionPage.LOGIN},modifier=Modifier.fillMaxWidth()) {Text(tr("सिक्के सहेजने के लिए लॉग इन करें","Log in to keep your coins"))}
            } else if(account.signedIn && !focused) {
                Text(profile?.optString("phone")?.takeIf {it.isNotEmpty()}?.let {"+91 $it"} ?: profile?.optString("email").orEmpty(),fontSize=18.sp)
                OutlinedButton(onClick={run {account.signOut();destination=DevotionPage.LOGIN}},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Icon(Icons.Outlined.Logout,null);Spacer(Modifier.width(8.dp));Text(tr("लॉग आउट","Log out"))}
                DevotionStreak(account.streakDays,hindi)
                Text(tr("एक दिन छूटने पर भी आपके सिक्के और अनलॉक बने रहते हैं।","Your coins and unlocked items stay yours, even if you miss a day."),fontSize=14.sp)
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
            }
            if((signingIn || sharing) && (!account.googleAccount || signingIn)) {
                if(account.referredInstall) Text(tr("Google से साइन इन करके अपने मित्र का निमंत्रण पूरा करें।", "Sign in with Google to credit your friend's invitation."))
                if(!sharing) Text(tr("अपनी पूजा श्रृंखला, सिक्के और मंदिर के रूप अपने Google खाते में सहेजें।", "Keep your puja streak, coins and temple designs with your Google account."))
                if(!account.googleConfigured) Text(tr("साइन इन अभी उपलब्ध नहीं है। पूजा जारी रख सकते हैं।", "Sign-in is currently unavailable. You can continue your puja."))
                else {
                    OutlinedButton(onClick={signIn()},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,Color(0xFF747775)),colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.White,contentColor=Color(0xFF1F1F1F))) {
                        Image(painterResource(R.drawable.google_sign_in_logo),contentDescription=null,modifier=Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Google से साइन इन करें","Sign in with Google"))
                    }
                }
                Text(tr("बिना साइन इन किए भी पूजा मुफ़्त है।", "Puja is free, even without signing in."),fontSize=14.sp)
                if(!sharing) MandirSajayeEntry(account,catalog,hindi,onCustomize,enabled=!busy && canCustomize)
            }
            if(account.signedIn && !focused) {
                if(account.pending) Text(tr("आज की पूजा सहेजने के लिए ताज़ा करें।","Today's puja is waiting to sync."),color=RitualGold)
                OutlinedButton(onClick={run {account.sync()}},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Icon(Icons.Outlined.Refresh,null);Spacer(Modifier.width(8.dp));Text(tr("प्रगति ताज़ा करें","Refresh progress"))}
            }
            if(busy || refreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if(busy) busyLabel else tr("प्रगति ताज़ा हो रही है…","Refreshing your progress…"),fontSize=14.sp,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})
            }
            if(message.isNotEmpty()) Surface(color=if(messageIsError) MaterialTheme.colorScheme.errorContainer else Color(0xFFF2E5D2),shape=RoundedCornerShape(16.dp)) {
                Text(message,color=if(messageIsError) MaterialTheme.colorScheme.error else RitualInk,modifier=Modifier.fillMaxWidth().padding(14.dp).semantics {liveRegion=LiveRegionMode.Polite})
            }
            TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()) {Text(tr("पूजा पर वापस जाएँ","Return to puja"))}
            }
        }
    }
    if(deleteConfirm) AlertDialog(onDismissRequest={deleteConfirm=false},title={Text(tr("खाता मिटाएँ?","Delete account?"))},text={Text(tr("पूजा का इतिहास और अनलॉक हटा दिए जाएँगे।","Your puja history and unlock will be removed."))},confirmButton={TextButton(onClick={deleteConfirm=false;run {account.delete()}}){Text(tr("मिटाएँ","Delete"))}},dismissButton={TextButton(onClick={deleteConfirm=false}){Text(tr("वापस","Cancel"))}})
}
