package com.worship.nityamandir.data

import java.time.LocalDate

enum class PassbookFilter { ALL, EARNED, SPENT }

enum class PassbookEntryKind { PUJA_REWARD, REFERRAL_REWARD, ITEM_UNLOCK, PACKAGE_UNLOCK }

data class RawHistoryEvent(
    val id: String,
    val kind: String,
    val at: String,
    val detail: String
)

data class PassbookEntry(
    val id: String,
    val kind: PassbookEntryKind,
    val delta: Int,
    val date: String,
    val categoryId: String? = null,
    val itemId: String? = null,
    val titleHi: String,
    val titleEn: String,
    val subtitleHi: String,
    val subtitleEn: String,
    val pendingSync: Boolean = false
)

data class PassbookLedger(
    val balance: Int,
    val totalEarned: Int,
    val totalSpent: Int,
    val entries: List<PassbookEntry>
) {
    fun filtered(filter: PassbookFilter): List<PassbookEntry> = when (filter) {
        PassbookFilter.ALL -> entries
        PassbookFilter.EARNED -> entries.filter { it.delta > 0 }
        PassbookFilter.SPENT -> entries.filter { it.delta < 0 }
    }
}

/** Pure coin passbook builder, independent of Android storage and org.json stubs. */
internal object CoinPassbook {
    private val monthsEn = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val monthsHi = arrayOf(
        "जन", "फ़र", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अग", "सितं", "अक्टू", "नवं", "दिसं"
    )

    fun formatDate(rawDate: String, hindi: Boolean, today: LocalDate): String {
        val iso = rawDate.trim().take(10)
        val parsed = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
        return when (parsed) {
            today -> if (hindi) "आज" else "Today"
            today.minusDays(1) -> if (hindi) "कल" else "Yesterday"
            else -> {
                val monthName = (if (hindi) monthsHi else monthsEn)[parsed.monthValue - 1]
                if (parsed.year == today.year) "${parsed.dayOfMonth} $monthName"
                else "${parsed.dayOfMonth} $monthName ${parsed.year}"
            }
        }
    }

    fun build(
        balance: Int,
        history: List<RawHistoryEvent>,
        categories: List<ShrineCategory>,
        today: LocalDate,
        pendingToday: Boolean = false,
        cost: (String, String) -> Int = { category, item -> ShrineUnlocks.cost(category, item) }
    ): PassbookLedger {
        val entries = mutableListOf<PassbookEntry>()
        val todayStr = today.toString()
        var todayPujaRecorded = false

        // Walk history from newest to oldest so recent passbook activity appears first.
        for (index in history.indices.reversed()) {
            val event = history[index]
            val entryId = event.id.ifBlank { "evt-$index" }
            when (event.kind) {
                "puja_completed" -> {
                    val datePart = event.detail.substringBefore("·").trim().takeIf { it.length >= 10 }
                        ?: event.at.take(10)
                    if (datePart.take(10) == todayStr) todayPujaRecorded = true
                    entries.add(
                        PassbookEntry(
                            id = entryId,
                            kind = PassbookEntryKind.PUJA_REWARD,
                            delta = 1,
                            date = datePart.take(10),
                            titleHi = "दैनिक पूजा पुरस्कार",
                            titleEn = "Daily puja reward",
                            subtitleHi = formatDate(datePart, hindi = true, today = today),
                            subtitleEn = formatDate(datePart, hindi = false, today = today)
                        )
                    )
                }
                "referral_coin" -> {
                    val datePart = event.at.take(10)
                    entries.add(
                        PassbookEntry(
                            id = entryId,
                            kind = PassbookEntryKind.REFERRAL_REWARD,
                            delta = 1,
                            date = datePart,
                            titleHi = "मित्र आमंत्रण पुरस्कार",
                            titleEn = "Friend referral reward",
                            subtitleHi = formatDate(datePart, hindi = true, today = today),
                            subtitleEn = formatDate(datePart, hindi = false, today = today)
                        )
                    )
                }
                "coin_unlock" -> {
                    val parts = event.detail.split(':', limit = 2)
                    val categoryId = parts.getOrNull(0).orEmpty()
                    val itemId = parts.getOrNull(1).orEmpty()
                    val category = categories.firstOrNull { it.id == categoryId }
                    val option = category?.options?.firstOrNull { it.id == itemId }
                    val itemCost = cost(categoryId, itemId).takeIf { it > 0 }
                        ?: ShrineUnlocks.cost(categoryId, itemId).coerceAtLeast(1)
                    val datePart = event.at.take(10)
                    val dateHi = formatDate(datePart, hindi = true, today = today)
                    val dateEn = formatDate(datePart, hindi = false, today = today)
                    entries.add(
                        PassbookEntry(
                            id = entryId,
                            kind = PassbookEntryKind.ITEM_UNLOCK,
                            delta = -itemCost,
                            date = datePart,
                            categoryId = categoryId.takeIf { it.isNotEmpty() },
                            itemId = itemId.takeIf { it.isNotEmpty() },
                            titleHi = if (option != null) "${option.hi} अनलॉक" else "मंदिर वस्तु अनलॉक",
                            titleEn = if (option != null) "Unlocked ${option.en}" else "Unlocked temple item",
                            subtitleHi = if (category != null) "${category.hi} · $dateHi" else dateHi,
                            subtitleEn = if (category != null) "${category.en} · $dateEn" else dateEn
                        )
                    )
                }
                "streak_unlock", "referral_unlock", "mock_purchase", "package_granted" -> {
                    val datePart = event.at.take(10)
                    entries.add(
                        PassbookEntry(
                            id = entryId,
                            kind = PassbookEntryKind.PACKAGE_UNLOCK,
                            delta = 0,
                            date = datePart,
                            titleHi = "संपूर्ण मंदिर पैकेज अनलॉक",
                            titleEn = "Full temple package unlocked",
                            subtitleHi = formatDate(datePart, hindi = true, today = today),
                            subtitleEn = formatDate(datePart, hindi = false, today = today)
                        )
                    )
                }
            }
        }

        if (pendingToday && !todayPujaRecorded) {
            entries.add(
                0,
                PassbookEntry(
                    id = "pending-$todayStr",
                    kind = PassbookEntryKind.PUJA_REWARD,
                    delta = 1,
                    date = todayStr,
                    titleHi = "दैनिक पूजा पुरस्कार",
                    titleEn = "Daily puja reward",
                    subtitleHi = "आज · सिंक बाकी",
                    subtitleEn = "Today · Sync pending",
                    pendingSync = true
                )
            )
        }

        val spentFromHistory = entries.filter { it.delta < 0 }.sumOf { -it.delta }
        val earnedFromHistory = entries.filter { it.delta > 0 }.sumOf { it.delta }
        val totalEarned = maxOf(earnedFromHistory, balance.coerceAtLeast(0) + spentFromHistory)

        return PassbookLedger(
            balance = balance.coerceAtLeast(0),
            totalEarned = totalEarned,
            totalSpent = spentFromHistory,
            entries = entries
        )
    }
}
