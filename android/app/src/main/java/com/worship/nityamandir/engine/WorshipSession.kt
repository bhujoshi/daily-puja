package com.worship.nityamandir.engine

/** Explicit ritual order: flowers are a separate fourth step with independent deity completion. */
enum class WorshipStep(val hi: String, val en: String) {
    LIGHT("दीप प्रज्वलन", "Light the oil lamp"),
    BATH("देव स्नान", "Sacred bathing"),
    TILAK("तिलक अर्पण", "Apply tilak"),
    FLOWERS("पुष्प अर्पण", "Offer flowers"),
    BELL("घंटी नाद", "Ring the bell"),
    CONCH("शंख नाद", "Sound the conch"),
    PRASAD("प्रसाद अर्पण", "Offer prasad"),
    RECITATION("आरती पाठ", "Aarti recitation"),
    AARTI("दीप से आरती", "Aarti with the diya")
}

data class WorshipSession(
    val step: WorshipStep = WorshipStep.LIGHT,
    val lit: Boolean = false,
    val bathed: Set<Int> = emptySet(),
    val tilak: Set<Int> = emptySet(),
    val flowers: Set<Int> = emptySet(),
    val offeredFlowers: Map<Int, Int> = emptyMap(),
    val recitationComplete: Boolean = false,
    val bellRung: Boolean = false,
    val conchBlown: Boolean = false,
    val prasadOffered: Boolean = false,
    val aartiLit: Boolean = false,
    val aartiComplete: Boolean = false,
    val complete: Boolean = false
) {
    val canContinue: Boolean get() = when(step) {
        WorshipStep.LIGHT -> lit
        WorshipStep.BATH -> bathed.containsAll(listOf(0,1))
        WorshipStep.TILAK -> tilak.containsAll(listOf(0,1))
        WorshipStep.FLOWERS -> flowers.containsAll(listOf(0,1))
        WorshipStep.BELL -> bellRung
        WorshipStep.CONCH -> conchBlown
        WorshipStep.PRASAD -> prasadOffered
        WorshipStep.RECITATION -> recitationComplete
        WorshipStep.AARTI -> aartiComplete
    }
    fun next(): WorshipSession = if(!canContinue || complete) this else if(step==WorshipStep.AARTI) copy(complete=true)
        else copy(step=WorshipStep.values()[step.ordinal+1])
}
