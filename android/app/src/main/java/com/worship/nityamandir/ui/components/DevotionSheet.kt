package com.worship.nityamandir.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.R
import com.worship.nityamandir.data.*
import com.worship.nityamandir.engine.BhajanPlayer
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class DevotionPage { PROFILE, PACKAGE, LOGIN, SHARE }

private val CardSurface = Color(0xFFFFFDF9)
private val CardSurfaceWarm = Color(0xFFF6ECDC)
private val CardBorder = Color(0x55B3833B)
private val MutedInk = Color(0xFF6E5D4F)
private val CreditGreen = Color(0xFF1E6B3A)
private val CreditGreenBg = Color(0xFFE6F3EA)
private val DebitTerracotta = Color(0xFF9E3B18)
private val DebitTerracottaBg = Color(0xFFF9E6DD)

@Composable
private fun MandirSajayeEntry(
    account: DevotionAccount,
    catalog: ShrineCatalog,
    hindi: Boolean,
    onCustomize: () -> Unit,
    enabled: Boolean
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val newUnlocks = account.newUnlocks(catalog)
    val availableUnlocks = account.availableUnlocks(catalog)
    fun names(items: List<ShrineUnlock>) =
        items.take(2).joinToString(" · ") { if (hindi) it.option.hi else it.option.en } +
            if (items.size > 2) tr(" और ${items.size - 2}", " + ${items.size - 2} more") else ""

    val nextCost = if (account.signedIn && newUnlocks.isEmpty() && availableUnlocks.isEmpty()) {
        catalog.categories.flatMap { category -> category.options.map { category.id to it } }
            .filter { (category, option) -> !account.owns(category, option.id) }
            .map { (category, option) -> account.cost(category, option.id) }
            .filter { it > 0 }
            .minOrNull()
    } else null

    Surface(
        color = CardSurfaceWarm,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when {
                        newUnlocks.isNotEmpty() -> {
                            Text(
                                tr(
                                    "${newUnlocks.size} नए अनलॉक आपके लिए तैयार हैं",
                                    if (newUnlocks.size == 1) "1 newly unlocked item is ready"
                                    else "${newUnlocks.size} newly unlocked items are ready"
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = RitualInk,
                                fontSize = 15.sp
                            )
                            Text(names(newUnlocks), fontSize = 13.sp, color = MutedInk)
                        }
                        availableUnlocks.isNotEmpty() -> {
                            Text(
                                tr(
                                    "आपके सिक्कों से ${availableUnlocks.size} नई वस्तुएँ अनलॉक हो सकती हैं",
                                    if (availableUnlocks.size == 1) "Your coins can unlock 1 new item"
                                    else "Your coins can unlock ${availableUnlocks.size} new items"
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = RitualInk,
                                fontSize = 15.sp
                            )
                            Text(names(availableUnlocks), fontSize = 13.sp, color = MutedInk)
                        }
                        account.signedIn -> {
                            Text(
                                tr("अपने मंदिर का रूप चुनें", "Choose your mandir’s look"),
                                fontWeight = FontWeight.SemiBold,
                                color = RitualInk,
                                fontSize = 15.sp
                            )
                            if (nextCost != null && nextCost > account.coins) {
                                Text(
                                    tr(
                                        "अगले अनलॉक के लिए ${nextCost - account.coins} और सिक्के",
                                        "${nextCost - account.coins} more coins for next unlock"
                                    ),
                                    fontSize = 13.sp,
                                    color = MutedInk
                                )
                            }
                        }
                        else -> {
                            Text(
                                tr("मुफ़्त वस्तुओं से मंदिर सजाएँ", "Decorate with free items"),
                                fontWeight = FontWeight.SemiBold,
                                color = RitualInk,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
            RitualChoice(
                tr("मंदिर सजाएँ", "Mandir Sajaye"),
                false,
                onCustomize,
                Modifier.fillMaxWidth(),
                enabled = enabled
            )
        }
    }
}

private val monthNamesHi = arrayOf(
    "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
    "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
)
private val monthNamesEn = arrayOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val weekDaysHi = listOf("रवि", "सोम", "मंगल", "बुध", "गुरु", "शुक्र", "शनि")
private val weekDaysEn = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun DevotionStreak(
    streak: Int,
    hindi: Boolean,
    completedDays: Set<String> = emptySet(),
    today: LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val effectiveDays = remember(completedDays, streak, today) {
        if (completedDays.isNotEmpty() || streak <= 0) completedDays
        else (0 until streak).map { today.minusDays(it.toLong()).toString() }.toSet()
    }
    val currentMonth = remember(today) { YearMonth.from(today) }
    var displayedMonth by remember(currentMonth) { mutableStateOf(currentMonth) }
    val cells = remember(displayedMonth, effectiveDays, today) {
        DevotionStreakCounter.monthCells(displayedMonth, effectiveDays, today)
    }
    val monthCompleted = remember(displayedMonth, effectiveDays) {
        DevotionStreakCounter.completedInMonth(displayedMonth, effectiveDays)
    }
    val monthTitle = remember(displayedMonth, hindi) {
        val name = (if (hindi) monthNamesHi else monthNamesEn)[displayedMonth.monthValue - 1]
        "$name ${displayedMonth.year}"
    }

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("puja-streak-calendar")
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Streak Header + Monthly Badge
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFCECD2),
                        border = BorderStroke(1.dp, Color(0x55D8AF52))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFD85816),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            if (hindi) "$streak दिन की पूजा श्रृंखला" else "$streak day puja streak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = RitualInk
                        )
                        Text(
                            tr("पूजा कैलेंडर", "Puja calendar"),
                            fontSize = 12.sp,
                            color = MutedInk
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CardSurfaceWarm,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        tr("$monthCompleted दिन", "$monthCompleted days"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RitualInk
                    )
                }
            }

            // Month Navigation Bar
            Surface(
                color = CardSurfaceWarm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = tr("पिछला महीना", "Previous month"),
                            tint = RitualInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        monthTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = RitualInk
                    )
                    val canGoNext = displayedMonth < currentMonth
                    IconButton(
                        onClick = { if (canGoNext) displayedMonth = displayedMonth.plusMonths(1) },
                        enabled = canGoNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = tr("अगला महीना", "Next month"),
                            tint = if (canGoNext) RitualInk else Color(0xFFBFA993),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Weekday Header Row
            val weekDays = if (hindi) weekDaysHi else weekDaysEn
            Row(Modifier.fillMaxWidth()) {
                weekDays.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedInk
                    )
                }
            }

            // Calendar Grid Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        week.forEach { day ->
                            Box(
                                Modifier.weight(1f).height(34.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    // Streak connector ribbon between consecutive completed days
                                    if (day.connectsPrevious || day.connectsNext) {
                                        Row(Modifier.fillMaxWidth().height(18.dp)) {
                                            Box(
                                                Modifier.weight(1f).fillMaxHeight().background(
                                                    if (day.connectsPrevious) Color(0x40D8AF52) else Color.Transparent
                                                )
                                            )
                                            Box(
                                                Modifier.weight(1f).fillMaxHeight().background(
                                                    if (day.connectsNext) Color(0x40D8AF52) else Color.Transparent
                                                )
                                            )
                                        }
                                    }
                                    val circleMod = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .then(
                                            when {
                                                day.completed -> Modifier.background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFFD8AF52), Color(0xFFB3833B))
                                                    )
                                                )
                                                day.isToday -> Modifier.background(CardSurfaceWarm)
                                                else -> Modifier
                                            }
                                        )
                                        .then(
                                            when {
                                                day.isToday && day.completed -> Modifier.border(1.5.dp, RitualInk, CircleShape)
                                                day.isToday -> Modifier.border(1.5.dp, RitualGold, CircleShape)
                                                else -> Modifier
                                            }
                                        )
                                    Box(circleMod, contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${day.date.dayOfMonth}",
                                            fontSize = 12.sp,
                                            fontWeight = if (day.completed || day.isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                day.completed -> Color(0xFFFFFDF8)
                                                day.isFuture -> Color(0xFFB8A99A)
                                                else -> RitualInk
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Compact Legend
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).background(RitualGold)
                    )
                    Text(tr("पूजा संपन्न", "Puja done"), fontSize = 12.sp, color = MutedInk)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(10.dp).clip(CircleShape).border(1.5.dp, RitualGold, CircleShape)
                    )
                    Text(tr("आज", "Today"), fontSize = 12.sp, color = MutedInk)
                }
            }
        }
    }
}

@Composable
private fun CoinPassbookCard(
    ledger: PassbookLedger,
    referrals: Int,
    hindi: Boolean,
    busy: Boolean,
    onShare: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    var filter by remember { mutableStateOf(PassbookFilter.ALL) }
    var expanded by remember { mutableStateOf(false) }
    val filteredEntries = remember(ledger, filter) { ledger.filtered(filter) }
    val visibleEntries = if (expanded) filteredEntries else filteredEntries.take(5)

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("coin-passbook-card")
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Balance & Summary Row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        Modifier.size(42.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFFF5E6C8),
                        border = BorderStroke(1.dp, Color(0x66B3833B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Toll,
                                contentDescription = null,
                                tint = RitualGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            tr("${ledger.balance} सिक्के", "${ledger.balance} coins"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = RitualInk
                        )
                        Text(
                            tr("सिक्का पासबुक", "Coin passbook"),
                            fontSize = 12.sp,
                            color = MutedInk
                        )
                    }
                }
                // Earned / Spent Summary Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CreditGreenBg
                    ) {
                        Column(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "+${ledger.totalEarned}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditGreen
                            )
                            Text(
                                tr("प्राप्त", "Earned"),
                                fontSize = 10.sp,
                                color = CreditGreen
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DebitTerracottaBg
                    ) {
                        Column(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "-${ledger.totalSpent}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DebitTerracotta
                            )
                            Text(
                                tr("खर्च", "Spent"),
                                fontSize = 10.sp,
                                color = DebitTerracotta
                            )
                        }
                    }
                }
            }

            // Compact Earn Info Strip + WhatsApp Invite Action
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurfaceWarm
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = RitualGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            tr("दैनिक पूजा +1", "Daily puja +1"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = RitualInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurfaceWarm
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.GroupAdd,
                            contentDescription = null,
                            tint = RitualGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (referrals > 0) tr("मित्र +1 ($referrals)", "Friend +1 ($referrals)")
                            else tr("मित्र आमंत्रण +1", "Invite friend +1"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = RitualInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            RitualChoice(
                tr("WhatsApp पर साझा करें", "Share on WhatsApp"),
                false,
                onShare,
                Modifier.fillMaxWidth(),
                enabled = !busy,
                leadingIcon = { WhatsAppIcon() }
            )

            HorizontalDivider(color = Color(0x26B3833B))

            // Passbook Filter Tabs
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    PassbookFilter.ALL to tr("सभी", "All"),
                    PassbookFilter.EARNED to tr("प्राप्त (+)", "Earned (+)"),
                    PassbookFilter.SPENT to tr("खर्च (-)", "Spent (-)")
                )
                tabs.forEach { (tab, label) ->
                    val selected = filter == tab
                    Surface(
                        onClick = { filter = tab },
                        shape = RoundedCornerShape(50),
                        color = if (selected) RitualInk else CardSurfaceWarm,
                        contentColor = if (selected) Color(0xFFFFFDF8) else RitualInk,
                        border = BorderStroke(1.dp, if (selected) RitualInk else CardBorder)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }

            // Passbook Ledger Rows
            if (filteredEntries.isEmpty()) {
                val emptyText = when (filter) {
                    PassbookFilter.ALL -> tr(
                        "पहला सिक्का कमाने के लिए दैनिक पूजा पूरी करें",
                        "Complete daily puja to earn your first coin"
                    )
                    PassbookFilter.EARNED -> tr(
                        "अभी कोई सिक्का प्राप्त नहीं हुआ",
                        "No coins earned yet"
                    )
                    PassbookFilter.SPENT -> tr(
                        "अभी कोई सिक्का खर्च नहीं किया",
                        "No coins spent yet"
                    )
                }
                Text(
                    emptyText,
                    fontSize = 13.sp,
                    color = MutedInk,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visibleEntries.forEachIndexed { idx, entry ->
                        PassbookEntryRow(entry, hindi)
                        if (idx < visibleEntries.lastIndex) {
                            HorizontalDivider(color = Color(0x1FB3833B))
                        }
                    }
                    if (filteredEntries.size > 5) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                if (expanded) tr("कम दिखाएँ", "Show less")
                                else tr(
                                    "सभी ${filteredEntries.size} लेन-देन देखें",
                                    "View all ${filteredEntries.size} entries"
                                ),
                                fontSize = 13.sp,
                                color = RitualGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassbookEntryRow(entry: PassbookEntry, hindi: Boolean) {
    val icon = when (entry.kind) {
        PassbookEntryKind.PUJA_REWARD -> Icons.Outlined.LocalFireDepartment
        PassbookEntryKind.REFERRAL_REWARD -> Icons.Outlined.GroupAdd
        PassbookEntryKind.ITEM_UNLOCK -> Icons.Outlined.LockOpen
        PassbookEntryKind.PACKAGE_UNLOCK -> Icons.Outlined.AutoAwesome
    }
    val iconBg = when {
        entry.delta > 0 -> CreditGreenBg
        entry.delta < 0 -> DebitTerracottaBg
        else -> CardSurfaceWarm
    }
    val iconTint = when {
        entry.delta > 0 -> CreditGreen
        entry.delta < 0 -> DebitTerracotta
        else -> RitualGold
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            Modifier.size(36.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconBg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                if (hindi) entry.titleHi else entry.titleEn,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = RitualInk
            )
            Text(
                if (hindi) entry.subtitleHi else entry.subtitleEn,
                fontSize = 12.sp,
                color = MutedInk
            )
        }
        val badgeColor = when {
            entry.delta > 0 -> CreditGreenBg
            entry.delta < 0 -> DebitTerracottaBg
            else -> CardSurfaceWarm
        }
        val textColor = when {
            entry.delta > 0 -> CreditGreen
            entry.delta < 0 -> DebitTerracotta
            else -> RitualGold
        }
        val amountLabel = when {
            entry.delta > 0 -> "+${entry.delta}"
            entry.delta < 0 -> "${entry.delta}"
            else -> "✓"
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    amountLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (entry.delta != 0) {
                    Icon(
                        Icons.Outlined.Toll,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionSheet(
    account: DevotionAccount,
    hindi: Boolean,
    onDismiss: () -> Unit,
    onCustomize: () -> Unit,
    canCustomize: Boolean = true,
    page: DevotionPage = DevotionPage.PROFILE,
    player: BhajanPlayer,
    onMusic: () -> Unit,
    onSignedIn: () -> Unit = {}
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val catalog = remember(context) { ShrineCatalog(context) }
    var destination by remember(page) { mutableStateOf(page) }
    var shareAfterLogin by remember(page) { mutableStateOf(page == DevotionPage.SHARE) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }
    var busyLabel by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf(false) }
    val profile = account.profile

    fun errorMessage(error: Exception): String = when (error) {
        is MissingWhatsAppException -> tr(
            "निमंत्रण साझा करने के लिए WhatsApp या WhatsApp Business इंस्टॉल करें।",
            "Install WhatsApp or WhatsApp Business to share your invitation."
        )
        is GoogleSignInFailure -> when (error.reason) {
            GoogleSignInFailure.Reason.UNAVAILABLE -> tr(
                "Google साइन इन अभी उपलब्ध नहीं है। थोड़ी देर बाद फिर कोशिश करें।",
                "Google sign-in is currently unavailable. Please try again later."
            )
            GoogleSignInFailure.Reason.PROVIDER_ERROR -> tr(
                "Google साइन इन शुरू नहीं हो पाया। फिर कोशिश करें या Google Play सेवाएँ अपडेट करें।",
                "Google sign-in could not start. Try again or update Google Play services."
            )
            GoogleSignInFailure.Reason.INVALID_CREDENTIAL -> tr(
                "Google साइन इन पूरा नहीं हो पाया। कृपया फिर कोशिश करें।",
                "Google sign-in could not be completed. Please try again."
            )
        }
        is AccountConnectionFailure -> tr(
            "कनेक्शन नहीं हो पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
            "Could not connect. Check your internet connection and try again."
        )
        is java.io.IOException -> tr(
            "कनेक्शन नहीं हो पाया। इंटरनेट जाँचें और फिर कोशिश करें।",
            "Could not connect. Check your internet connection and try again."
        )
        else -> error.message ?: tr("फिर कोशिश करें", "Please try again")
    }

    fun run(label: String = tr("कृपया प्रतीक्षा करें…", "Please wait…"), action: suspend () -> Unit) {
        if (busy) return
        busy = true; busyLabel = label; message = ""; messageIsError = false
        scope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = errorMessage(error); messageIsError = true }
            finally { busy = false }
        }
    }

    suspend fun share() {
        val invitation = account.createShare()
        val text = tr(
            "पवित्र मंदिर पर मेरे साथ पूजा करें। डाउनलोड करें और Google से साइन इन करें:\n",
            "Join me for daily puja on Pavitra Mandir. Install and sign in with Google:\n"
        ) + invitation.getString("url")
        WhatsAppShare.open(context, text)
        account.queueShareOpened(invitation.getString("share_id"))
        message = tr("आपका निमंत्रण WhatsApp में खुल गया।", "WhatsApp opened with your invitation.")
        messageIsError = false
        try { account.recordShareOpened(invitation.getString("share_id")) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            message = tr(
                "WhatsApp खुल गया। शेयर गिनती इंटरनेट मिलने पर सहेजेंगे।",
                "WhatsApp opened. Your share count will sync when connected."
            )
        }
    }

    fun signIn() = run(tr("Google से साइन इन हो रहा है…", "Signing in with Google…")) {
        if (!account.signInWithGoogle(context, "")) {
            message = tr(
                "साइन इन पूरा नहीं हुआ। तैयार हों तो फिर कोशिश करें।",
                "Sign-in was not completed. Try again when you’re ready."
            )
        } else {
            val continueToShare = shareAfterLogin || destination == DevotionPage.SHARE
            shareAfterLogin = false
            destination = if (continueToShare) DevotionPage.SHARE
            else if (page == DevotionPage.PACKAGE) DevotionPage.PACKAGE
            else DevotionPage.PROFILE
            message = tr("साइन इन हो गया।", "You’re signed in.")
            // The requested WhatsApp handoff must not wait on optional progress sync.
            if (continueToShare) {
                busyLabel = tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")
                try { share() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { message = errorMessage(error); messageIsError = true }
            }
            try { account.sync() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (!continueToShare) {
                    message = tr(
                        "साइन इन हो गया। इंटरनेट मिलने पर प्रगति सहेजेंगे।",
                        "You’re signed in. Your progress will sync when connected."
                    )
                }
            }
            if (!continueToShare) onSignedIn()
        }
    }

    LaunchedEffect(page) {
        if (page == DevotionPage.SHARE && account.googleAccount) {
            run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {
                shareAfterLogin = false
                share()
            }
        } else if (account.signedIn && page in listOf(DevotionPage.PROFILE, DevotionPage.PACKAGE)) {
            refreshing = true
            try { account.sync() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = errorMessage(error); messageIsError = true }
            finally { refreshing = false }
        }
    }

    val sharing = destination == DevotionPage.SHARE || shareAfterLogin
    val signingIn = destination == DevotionPage.LOGIN || (!account.signedIn && destination == DevotionPage.PROFILE)
    val focused = sharing || signingIn
    val today = remember { LocalDate.now(ZoneId.of("Asia/Kolkata")) }
    val ledger = account.passbook(catalog, today)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFFAF6EE)
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (player.current != null || player.loading) {
                BhajanMiniPlayer(
                    player,
                    hindi,
                    onMusic,
                    Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sheet Header Row
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (sharing) {
                            WhatsAppIcon(Modifier.size(28.dp))
                        } else {
                            Icon(
                                if (destination == DevotionPage.PACKAGE) Icons.Outlined.AutoAwesome
                                else Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = RitualGold,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            when {
                                sharing -> tr("मित्र को आमंत्रित करें", "Invite a friend")
                                signingIn -> tr("साइन इन करें", "Sign in")
                                destination == DevotionPage.PACKAGE -> tr("सिक्के कमाएँ", "Earn coins")
                                else -> tr("मेरी प्रोफ़ाइल", "My profile")
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = RitualInk
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (account.signedIn && !focused) {
                            Surface(
                                shape = CircleShape,
                                color = CardSurfaceWarm,
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(
                                    onClick = { run { account.sync() } },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        Icons.Outlined.Refresh,
                                        contentDescription = tr("प्रगति ताज़ा करें", "Refresh progress"),
                                        tint = RitualInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = CardSurfaceWarm,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = tr("पूजा पर वापस जाएँ", "Return to puja"),
                                    tint = RitualInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Signed-In Account Identity Pill Row
                if (account.signedIn && !focused) {
                    val identity = profile?.optString("phone")?.takeIf { it.isNotEmpty() }?.let { "+91 $it" }
                        ?: profile?.optString("email").orEmpty()
                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                if (identity.isNotEmpty()) {
                                    Text(
                                        identity,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RitualInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (account.pending) {
                                    Text(
                                        tr("आज की पूजा सिंक बाकी है", "Today's puja waiting to sync"),
                                        fontSize = 12.sp,
                                        color = RitualGold
                                    )
                                }
                            }
                            TextButton(
                                onClick = { run { account.signOut(); destination = DevotionPage.LOGIN } },
                                enabled = !busy,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(16.dp), tint = MutedInk)
                                Spacer(Modifier.width(4.dp))
                                Text(tr("लॉग आउट", "Log out"), fontSize = 13.sp, color = MutedInk)
                            }
                        }
                    }
                }

                if (sharing) {
                    Surface(
                        color = CardSurfaceWarm,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (!account.googleAccount) tr(
                                    "1. Google से साइन इन करें\n2. WhatsApp में मित्र चुनकर निमंत्रण भेजें",
                                    "1. Sign in with Google\n2. Choose a friend in WhatsApp and send your invitation"
                                ) else tr(
                                    "WhatsApp में मित्र चुनें और निमंत्रण भेजें।",
                                    "Choose a friend in WhatsApp and send your invitation."
                                ),
                                color = RitualInk,
                                fontSize = 14.sp
                            )
                            Text(
                                tr(
                                    "मित्र पहली बार इंस्टॉल करके Google से साइन इन करे तो आपको +1 सिक्का मिलेगा।",
                                    "Earn +1 coin when a new friend installs and signs in with Google."
                                ),
                                fontSize = 13.sp,
                                color = MutedInk
                            )
                        }
                    }
                    if (account.googleAccount) {
                        RitualChoice(
                            tr("WhatsApp पर साझा करें", "Share on WhatsApp"),
                            false,
                            {
                                run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {
                                    share()
                                }
                            },
                            Modifier.fillMaxWidth(),
                            enabled = !busy,
                            leadingIcon = { WhatsAppIcon() }
                        )
                        TextButton(
                            onClick = { destination = DevotionPage.PROFILE; message = "" },
                            enabled = !busy
                        ) {
                            Text(tr("मेरी प्रोफ़ाइल देखें", "View my profile"))
                        }
                    }
                }

                if (!focused) {
                    // 1. Mandir Sajaye Quick Entry Card
                    MandirSajayeEntry(
                        account = account,
                        catalog = catalog,
                        hindi = hindi,
                        onCustomize = onCustomize,
                        enabled = !busy && canCustomize
                    )

                    // 2. Puja Streak Calendar
                    DevotionStreak(
                        streak = account.streakDays,
                        hindi = hindi,
                        completedDays = account.completedDays,
                        today = today
                    )

                    // 3. UX-Friendly Coin Passbook
                    CoinPassbookCard(
                        ledger = ledger,
                        referrals = profile?.optInt("referrals") ?: 0,
                        hindi = hindi,
                        busy = busy,
                        onShare = {
                            destination = DevotionPage.SHARE
                            if (account.googleAccount) {
                                run(tr("निमंत्रण तैयार हो रहा है…", "Preparing your invitation…")) {
                                    share()
                                }
                            } else {
                                shareAfterLogin = true
                                message = ""
                            }
                        }
                    )

                    if (!account.signedIn) {
                        OutlinedButton(
                            onClick = { destination = DevotionPage.LOGIN },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tr("सिक्के सहेजने के लिए लॉग इन करें", "Log in to keep your coins"))
                        }
                    }
                }

                if ((signingIn || sharing) && (!account.googleAccount || signingIn)) {
                    if (account.referredInstall) {
                        Text(
                            tr(
                                "Google से साइन इन करके अपने मित्र का निमंत्रण पूरा करें।",
                                "Sign in with Google to credit your friend's invitation."
                            ),
                            fontSize = 14.sp,
                            color = RitualInk
                        )
                    }
                    if (!sharing) {
                        Text(
                            tr(
                                "अपनी पूजा श्रृंखला, सिक्के और मंदिर के रूप अपने Google खाते में सहेजें।",
                                "Keep your puja streak, coins and temple designs with your Google account."
                            ),
                            fontSize = 14.sp,
                            color = MutedInk
                        )
                    }
                    if (!account.googleConfigured) {
                        Text(
                            tr(
                                "साइन इन अभी उपलब्ध नहीं है। पूजा जारी रख सकते हैं।",
                                "Sign-in is currently unavailable. You can continue your puja."
                            ),
                            fontSize = 14.sp,
                            color = MutedInk
                        )
                    } else {
                        OutlinedButton(
                            onClick = { signIn() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(17.dp),
                            border = BorderStroke(1.dp, Color(0xFF747775)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1F1F1F)
                            )
                        ) {
                            Image(
                                painterResource(R.drawable.google_sign_in_logo),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(tr("Google से साइन इन करें", "Sign in with Google"))
                        }
                    }
                    if (!sharing) {
                        MandirSajayeEntry(
                            account = account,
                            catalog = catalog,
                            hindi = hindi,
                            onCustomize = onCustomize,
                            enabled = !busy && canCustomize
                        )
                    }
                }

                if (busy || refreshing) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        if (busy) busyLabel else tr("प्रगति ताज़ा हो रही है…", "Refreshing your progress…"),
                        fontSize = 13.sp,
                        color = MutedInk,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                if (message.isNotEmpty()) {
                    Surface(
                        color = if (messageIsError) MaterialTheme.colorScheme.errorContainer else CardSurfaceWarm,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            message,
                            color = if (messageIsError) MaterialTheme.colorScheme.error else RitualInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }

                if (account.signedIn && !focused) {
                    TextButton(
                        onClick = { deleteConfirm = true },
                        enabled = !busy,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            tr("खाता और गतिविधि मिटाएँ", "Delete account and activity"),
                            fontSize = 12.sp,
                            color = MutedInk
                        )
                    }
                }
            }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(tr("खाता मिटाएँ?", "Delete account?")) },
            text = {
                Text(
                    tr(
                        "पूजा का इतिहास और अनलॉक हटा दिए जाएँगे।",
                        "Your puja history and unlock will be removed."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirm = false
                        run { account.delete() }
                    }
                ) {
                    Text(tr("मिटाएँ", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) {
                    Text(tr("वापस", "Cancel"))
                }
            }
        )
    }
}
