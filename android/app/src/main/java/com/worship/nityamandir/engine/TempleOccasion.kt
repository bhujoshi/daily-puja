package com.worship.nityamandir.engine

import com.worship.nityamandir.data.ShrineSelection
import java.time.LocalDate
import java.time.MonthDay

/** Visual theme for the post-aarti sanctum celebration across different Hindu festivals. */
enum class OccasionCelebrationStyle {
    DIWALI_DEEPMALA,
    NAVRATRI_SHAKTI,
    JANMASHTAMI_MORPANKH,
    GANESH_MODAK,
    SHIVRATRI_JYOTI,
    RAM_SURYA
}

/**
 * Defines a festival/occasion temple setting, its active calendar window,
 * its 1-tap ShrineSelection preset, and its post-Aarti celebration style.
 */
data class TempleOccasion(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val badgeHi: String,
    val badgeEn: String,
    val subtitleHi: String,
    val subtitleEn: String,
    val blessingHi: String,
    val blessingEn: String,
    val startMonthDay: MonthDay,
    val endMonthDay: MonthDay,
    val presetSelection: ShrineSelection,
    val celebrationStyle: OccasionCelebrationStyle,
    val exclusiveShrineId: String? = null,
    val primaryColorHex: Long = 0xFFFF9E2C,
    val secondaryColorHex: Long = 0xFFA82424,
    val accentGoldHex: Long = 0xFFFFE082
) {
    fun title(hindi: Boolean): String = if (hindi) titleHi else titleEn
    fun badge(hindi: Boolean): String = if (hindi) badgeHi else badgeEn
    fun subtitle(hindi: Boolean): String = if (hindi) subtitleHi else subtitleEn
    fun blessing(hindi: Boolean): String = if (hindi) blessingHi else blessingEn

    /** Checks whether [date] falls within this occasion's annual festival window. */
    fun isActiveOn(date: LocalDate): Boolean {
        val current = MonthDay.from(date)
        return if (startMonthDay <= endMonthDay) {
            current in startMonthDay..endMonthDay
        } else {
            // Handles year-wrapping windows if ever needed (e.g., Dec -> Jan).
            current >= startMonthDay || current <= endMonthDay
        }
    }

    /** True when the user's current selection matches this occasion's key temple configuration. */
    fun matchesSelection(selection: ShrineSelection): Boolean {
        if (exclusiveShrineId != null) {
            return selection["shrine"] == exclusiveShrineId
        }
        return selection["shrine"] == presetSelection["shrine"] &&
            selection["idols"] == presetSelection["idols"]
    }
}

object TempleOccasions {
    const val OVERRIDE_NONE = "none"

    val DIWALI = TempleOccasion(
        id = "diwali",
        titleHi = "दीपावली उत्सव मंदिर",
        titleEn = "Diwali Utsav Mandir",
        badgeHi = "दीपावली विशेष",
        badgeEn = "Diwali Special",
        subtitleHi = "स्वर्ण दीपमाला, अष्टदल कमल रंगोली और शुभ लक्ष्मी चरण",
        subtitleEn = "Golden Deepmala, lotus rangoli & sacred Lakshmi Charan",
        blessingHi = "शुभ दीपावली · माँ लक्ष्मी और श्री गणेश आपके घर सुख-समृद्धि लाएँ",
        blessingEn = "Shubh Deepavali · May Maa Lakshmi & Lord Ganesha bless your home",
        startMonthDay = MonthDay.of(11, 1),
        endMonthDay = MonthDay.of(11, 11),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "diwali",
                "idols" to "original",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "laddu"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.DIWALI_DEEPMALA,
        exclusiveShrineId = "diwali",
        primaryColorHex = 0xFFFF9E2C,
        secondaryColorHex = 0xFFA82424,
        accentGoldHex = 0xFFFFE082
    )

    val NAVRATRI = TempleOccasion(
        id = "navratri",
        titleHi = "नवरात्रि शक्ति मंदिर",
        titleEn = "Navratri Shakti Mandir",
        badgeHi = "नवरात्रि पर्व",
        badgeEn = "Navratri Festival",
        subtitleHi = "अखंड ज्योति, कुमकुम श्री यंत्र रंगोली और देवी चरण",
        subtitleEn = "Akhand Jyoti, Kumkum Sri Yantra rangoli & Devi Charan",
        blessingHi = "जय माता दी · माँ दुर्गा आपकी सभी मनोकामनाएँ पूर्ण करें",
        blessingEn = "Jai Mata Di · May Maa Durga fulfill all your wishes",
        startMonthDay = MonthDay.of(9, 27),
        endMonthDay = MonthDay.of(10, 17),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "swarna_peeth",
                "idols" to "durga",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "halwa"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.NAVRATRI_SHAKTI,
        exclusiveShrineId = null,
        primaryColorHex = 0xFFD85816,
        secondaryColorHex = 0xFF881818,
        accentGoldHex = 0xFFFFCA3A
    )

    val GANESH_CHATURTHI = TempleOccasion(
        id = "ganesh_chaturthi",
        titleHi = "गणेशोत्सव मंदिर",
        titleEn = "Ganeshotsav Mandir",
        badgeHi = "गणेश चतुर्थी",
        badgeEn = "Ganesh Chaturthi",
        subtitleHi = "मंगल स्वस्तिक रंगोली, दीपमाला और मोदक उत्सव",
        subtitleEn = "Auspicious Swastik rangoli, Deepmala & Modak celebration",
        blessingHi = "गणपति बप्पा मोरया · विघ्नहर्ता आपके सभी संकट हरें",
        blessingEn = "Ganpati Bappa Morya · May Lord Ganesha remove all obstacles",
        startMonthDay = MonthDay.of(9, 9),
        endMonthDay = MonthDay.of(9, 26),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "marble",
                "idols" to "ganesh_hanuman",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "laddu"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.GANESH_MODAK,
        exclusiveShrineId = null,
        primaryColorHex = 0xFFFF7A18,
        secondaryColorHex = 0xFFA82424,
        accentGoldHex = 0xFFFFD54F
    )

    val JANMASHTAMI = TempleOccasion(
        id = "janmashtami",
        titleHi = "जन्माष्टमी कुंज मंदिर",
        titleEn = "Janmashtami Kunj Mandir",
        badgeHi = "श्रीकृष्ण जन्माष्टमी",
        badgeEn = "Janmashtami Special",
        subtitleHi = "मोरपंख कमल रंगोली, यमुना दीपदान और बाल-कृष्ण चरण",
        subtitleEn = "Peacock-lotus rangoli, Yamuna diyas & Bal-Krishna footprints",
        blessingHi = "जय श्री राधे कृष्ण · नंदलाल आपके जीवन में आनंद भरें",
        blessingEn = "Jai Shri Radhe Krishna · May Lord Krishna fill your life with joy",
        startMonthDay = MonthDay.of(8, 12),
        endMonthDay = MonthDay.of(9, 8),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "kamal_peeth",
                "idols" to "radha_krishna",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "halwa"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.JANMASHTAMI_MORPANKH,
        exclusiveShrineId = null,
        primaryColorHex = 0xFFE26A8D,
        secondaryColorHex = 0xFF2B6CB0,
        accentGoldHex = 0xFFFFE082
    )

    val RAM_NAVAMI = TempleOccasion(
        id = "ram_navami",
        titleHi = "राम नवमी अयोध्या मंदिर",
        titleEn = "Ram Navami Ayodhya Mandir",
        badgeHi = "श्रीराम नवमी",
        badgeEn = "Ram Navami Special",
        subtitleHi = "सूर्यवंशी स्वर्ण रंगोली और सरयू दीपमाला",
        subtitleEn = "Suryavanshi golden rangoli & Saryu Deepmala",
        blessingHi = "जय श्री राम · मर्यादा पुरुषोत्तम श्रीराम की कृपा बनी रहे",
        blessingEn = "Jai Shri Ram · May Lord Rama bless your family with peace",
        startMonthDay = MonthDay.of(3, 22),
        endMonthDay = MonthDay.of(4, 20),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "swarna_peeth",
                "idols" to "ram_darbar",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "laddu"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.RAM_SURYA,
        exclusiveShrineId = null,
        primaryColorHex = 0xFFFF9E2C,
        secondaryColorHex = 0xFFD85816,
        accentGoldHex = 0xFFFFE777
    )

    val MAHASHIVRATRI = TempleOccasion(
        id = "mahashivratri",
        titleHi = "महाशिवरात्रि ज्योतिर्लिंग मंदिर",
        titleEn = "Maha Shivratri Jyotirlinga Mandir",
        badgeHi = "महाशिवरात्रि पर्व",
        badgeEn = "Maha Shivratri",
        subtitleHi = "त्रिपुंड्र बिल्व मंडल और रुद्र ज्योति दीपमाला",
        subtitleEn = "Tripundra Bilva mandala & Rudra Jyoti lamps",
        blessingHi = "हर हर महादेव · भगवान भोलेनाथ आपका कल्याण करें",
        blessingEn = "Har Har Mahadev · May Lord Shiva bestow peace and wisdom",
        startMonthDay = MonthDay.of(2, 10),
        endMonthDay = MonthDay.of(3, 10),
        presetSelection = ShrineSelection(
            mapOf(
                "shrine" to "divya_darshan",
                "idols" to "shiva",
                "garland" to "original",
                "flowers" to "original",
                "incense" to "original",
                "lamp" to "brass",
                "shankh" to "ivory",
                "aarti" to "traditional",
                "prasad" to "laddu"
            )
        ),
        celebrationStyle = OccasionCelebrationStyle.SHIVRATRI_JYOTI,
        exclusiveShrineId = null,
        primaryColorHex = 0xFFFFB74D,
        secondaryColorHex = 0xFF5C6BC0,
        accentGoldHex = 0xFFFFF3C4
    )

    val all: List<TempleOccasion> = listOf(
        DIWALI,
        NAVRATRI,
        GANESH_CHATURTHI,
        JANMASHTAMI,
        RAM_NAVAMI,
        MAHASHIVRATRI
    )

    private val exclusiveShrineOwners: Map<String, TempleOccasion> =
        all.mapNotNull { occasion -> occasion.exclusiveShrineId?.let { it to occasion } }.toMap()

    fun byId(id: String?): TempleOccasion? = all.firstOrNull { it.id == id }

    /**
     * Resolves the currently active occasion for [date], honoring an optional
     * [overrideOccasionId] (used in debug mode or preview testing).
     */
    fun activeOccasion(date: LocalDate, overrideOccasionId: String? = null): TempleOccasion? {
        if (overrideOccasionId == OVERRIDE_NONE) return null
        if (!overrideOccasionId.isNullOrBlank()) {
            byId(overrideOccasionId)?.let { return it }
        }
        return all.firstOrNull { it.isActiveOn(date) }
    }

    /**
     * Resolves the occasion governing the current shrine scene:
     * - If the shrine itself is an occasion-exclusive shrine (e.g. `"diwali"`), returns that occasion.
     * - Otherwise, if [activeOccasion] is active and matches the current selection, returns [activeOccasion].
     */
    fun forSelection(
        selection: ShrineSelection,
        activeOccasion: TempleOccasion? = null
    ): TempleOccasion? {
        exclusiveShrineOwners[selection["shrine"]]?.let { return it }
        if (activeOccasion != null && activeOccasion.matchesSelection(selection)) {
            return activeOccasion
        }
        return null
    }

    /**
     * Determines whether a shrine option should be visible in Mandir Sajaye.
     * Occasion-exclusive shrines (such as `"diwali"`) are shown only during their occasion
     * or when already selected by the user.
     */
    fun isShrineVisible(
        shrineId: String,
        activeOccasion: TempleOccasion?,
        currentSelection: ShrineSelection = ShrineSelection()
    ): Boolean {
        val owner = exclusiveShrineOwners[shrineId] ?: return true
        return activeOccasion?.id == owner.id || currentSelection["shrine"] == shrineId
    }
}
