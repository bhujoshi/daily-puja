package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Defines a weekday-specific sacred temple configuration (Vara Puja) with a dedicated
 * deity, sanctum architecture, garlands, blossoms, lamps, and prasad — all free to all devotees.
 */
data class DailyTemple(
    val dayOfWeek: DayOfWeek,
    val id: String,
    val dayNameHi: String,
    val dayNameEn: String,
    val shortDayHi: String,
    val shortDayEn: String,
    val deityNameHi: String,
    val deityNameEn: String,
    val titleHi: String,
    val titleEn: String,
    val subtitleHi: String,
    val subtitleEn: String,
    val presetSelection: ShrineSelection
) {
    fun dayName(hindi: Boolean): String = if (hindi) dayNameHi else dayNameEn
    fun shortDay(hindi: Boolean): String = if (hindi) shortDayHi else shortDayEn
    fun deityName(hindi: Boolean): String = if (hindi) deityNameHi else deityNameEn
    fun title(hindi: Boolean): String = if (hindi) titleHi else titleEn
    fun subtitle(hindi: Boolean): String = if (hindi) subtitleHi else subtitleEn

    /** True when the user's current shrine and idol match today's weekday temple. */
    fun matchesSelection(selection: ShrineSelection): Boolean =
        selection["shrine"] == presetSelection["shrine"] &&
            selection["idols"] == presetSelection["idols"]
}

object DailyTemples {
    val MONDAY = DailyTemple(
        dayOfWeek = DayOfWeek.MONDAY,
        id = "monday_shiva",
        dayNameHi = "सोमवार",
        dayNameEn = "Monday",
        shortDayHi = "सोम",
        shortDayEn = "Mon",
        deityNameHi = "भगवान शिव",
        deityNameEn = "Lord Shiva",
        titleHi = "सोमवार · भगवान शिव मंदिर",
        titleEn = "Monday · Lord Shiva Mandir",
        subtitleHi = "दिव्य दर्शन गर्भगृह, मोगरा पुष्प, लोबान धूप और पंचमुखी दीप",
        subtitleEn = "Divya Darshan sanctum, white mogra, loban dhoop & panchmukhi diya",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "divya_darshan",
                "idols" to "shiva",
                "garland" to "mogra",
                "flowers" to "mogra_flower",
                "incense" to "dhoop",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "halwa"
            )
        )
    )

    val TUESDAY = DailyTemple(
        dayOfWeek = DayOfWeek.TUESDAY,
        id = "tuesday_hanuman",
        dayNameHi = "मंगलवार",
        dayNameEn = "Tuesday",
        shortDayHi = "मंगल",
        shortDayEn = "Tue",
        deityNameHi = "श्री हनुमान जी",
        deityNameEn = "Lord Hanuman",
        titleHi = "मंगलवार · श्री हनुमान मंदिर",
        titleEn = "Tuesday · Lord Hanuman Mandir",
        subtitleHi = "नक्काशीदार मंदिर, गेंदे की माला, पंचमुखी दीप और लड्डू भोग",
        subtitleEn = "Carved sanctuary, marigold garland, panchmukhi diya & laddu bhog",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "carved",
                "idols" to "hanuman",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "laddu"
            )
        )
    )

    val WEDNESDAY = DailyTemple(
        dayOfWeek = DayOfWeek.WEDNESDAY,
        id = "wednesday_ganesha",
        dayNameHi = "बुधवार",
        dayNameEn = "Wednesday",
        shortDayHi = "बुध",
        shortDayEn = "Wed",
        deityNameHi = "श्री गणेश एवं हनुमान जी",
        deityNameEn = "Lord Ganesha & Hanuman",
        titleHi = "बुधवार · श्री गणेश मंदिर",
        titleEn = "Wednesday · Lord Ganesha Mandir",
        subtitleHi = "संगमरमर मंदिर, गेंदे की माला और केसर मोदक भोग",
        subtitleEn = "Marble arch sanctum, marigold garland & kesar modak bhog",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "marble",
                "idols" to "ganesh_hanuman",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "original",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "modak"
            )
        )
    )

    val THURSDAY = DailyTemple(
        dayOfWeek = DayOfWeek.THURSDAY,
        id = "thursday_radha_krishna",
        dayNameHi = "गुरुवार",
        dayNameEn = "Thursday",
        shortDayHi = "गुरु",
        shortDayEn = "Thu",
        deityNameHi = "श्री राधा-कृष्ण",
        deityNameEn = "Radha & Krishna",
        titleHi = "गुरुवार · श्री राधा-कृष्ण मंदिर",
        titleEn = "Thursday · Radha-Krishna Mandir",
        subtitleHi = "कमल पीठ, मोर दीपस्तंभ, स्वर्ण शंख और गुलाब पुष्प",
        subtitleEn = "Kamal Peeth sanctum, peacock lamp, golden shankh & rose blossoms",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "kamal_peeth",
                "idols" to "radha_krishna",
                "garland" to "rose",
                "flowers" to "rose",
                "incense" to "kamal_dhoop",
                "lamp" to "peacock",
                "shankh" to "swarna",
                "aarti" to "original",
                "prasad" to "laddu"
            )
        )
    )

    val FRIDAY = DailyTemple(
        dayOfWeek = DayOfWeek.FRIDAY,
        id = "friday_durga",
        dayNameHi = "शुक्रवार",
        dayNameEn = "Friday",
        shortDayHi = "शुक्र",
        shortDayEn = "Fri",
        deityNameHi = "माँ दुर्गा",
        deityNameEn = "Maa Durga",
        titleHi = "शुक्रवार · माँ दुर्गा मंदिर",
        titleEn = "Friday · Maa Durga Mandir",
        subtitleHi = "स्वर्ण पीठ, गुलाब पुष्प, नागकन्या पंच-आरती और हलवा भोग",
        subtitleEn = "Swarna Peeth sanctum, rose blossoms, panch-aarti & halwa bhog",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "swarna_peeth",
                "idols" to "durga",
                "garland" to "rose",
                "flowers" to "rose",
                "incense" to "dhoop",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "karpura",
                "prasad" to "halwa"
            )
        )
    )

    val SATURDAY = DailyTemple(
        dayOfWeek = DayOfWeek.SATURDAY,
        id = "saturday_shanidev",
        dayNameHi = "शनिवार",
        dayNameEn = "Saturday",
        shortDayHi = "शनि",
        shortDayEn = "Sat",
        deityNameHi = "श्री शनिदेव महाराज",
        deityNameEn = "Lord Shani Dev",
        titleHi = "शनिवार · श्री शनिदेव मंदिर",
        titleEn = "Saturday · Lord Shani Dev Mandir",
        subtitleHi = "हाथीदाँत मंदिर, नील ऑर्किड पुष्प, लोबान धूप और पंचमुखी दीप",
        subtitleEn = "Ivory sanctum, blue orchid blossoms, loban dhoop & panchmukhi diya",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "ivory",
                "idols" to "shanidev",
                "garland" to "mogra",
                "flowers" to "orchid",
                "incense" to "dhoop",
                "lamp" to "brass",
                "shankh" to "swarna",
                "aarti" to "traditional",
                "prasad" to "halwa"
            )
        )
    )

    val SUNDAY = DailyTemple(
        dayOfWeek = DayOfWeek.SUNDAY,
        id = "sunday_lakshmi",
        dayNameHi = "रविवार",
        dayNameEn = "Sunday",
        shortDayHi = "रवि",
        shortDayEn = "Sun",
        deityNameHi = "माता महालक्ष्मी",
        deityNameEn = "Maa Lakshmi",
        titleHi = "रविवार · माता महालक्ष्मी मंदिर",
        titleEn = "Sunday · Maa Lakshmi Mandir",
        subtitleHi = "स्वर्ण गर्भगृह, कमल व मोती माला, कमल धूप और मोर दीपस्तंभ",
        subtitleEn = "Golden sanctum, lotus & pearl garland, kamal dhoop & peacock lamp",
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "swarna",
                "idols" to "lakshmi",
                "garland" to "lotus",
                "flowers" to "rose",
                "incense" to "kamal_dhoop",
                "lamp" to "peacock",
                "shankh" to "swarna",
                "aarti" to "original",
                "prasad" to "halwa"
            )
        )
    )

    val all: List<DailyTemple> = listOf(
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY
    )

    private val byDayOfWeek: Map<DayOfWeek, DailyTemple> = all.associateBy { it.dayOfWeek }

    fun forDay(dayOfWeek: DayOfWeek): DailyTemple = byDayOfWeek.getValue(dayOfWeek)

    fun forDate(date: LocalDate): DailyTemple = forDay(date.dayOfWeek)

    fun byId(id: String?): DailyTemple? = all.firstOrNull { it.id == id }
}

/**
 * Pure daily temple auto-switch rules, independent of Android storage.
 * Keeps automatic switching per-day and per-account scope while honoring same-day manual choices.
 */
object DailyTempleSync {
    const val KEY_AUTO_ENABLED = "auto_daily_temple_enabled"

    fun scopeKey(accountId: String?, signedIn: Boolean): String =
        if (signedIn && !accountId.isNullOrBlank()) accountId else "guest"

    fun lastAppliedPreferenceKey(scope: String): String =
        "daily_temple_last_applied_$scope"

    fun manualOverridePreferenceKey(scope: String): String =
        "daily_temple_manual_override_$scope"

    fun shouldAutoApply(
        autoEnabled: Boolean,
        todayIsoDate: String,
        lastAppliedIsoDate: String?,
        manualOverrideIsoDate: String?
    ): Boolean {
        if (!autoEnabled) return false
        if (manualOverrideIsoDate == todayIsoDate) return false
        return lastAppliedIsoDate != todayIsoDate
    }
}
