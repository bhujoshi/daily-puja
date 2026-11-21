package com.worship.nityamandir.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.engine.WorshipStep

/** A short numbered instruction keeps step context inside the compact top sanctum pill. */
@Composable
fun PujaStepGuide(
    step: WorshipStep,
    hindi: Boolean,
    modifier: Modifier = Modifier,
    deityCount: Int = 2,
    doneCount: Int = 0,
    aartiRunning: Boolean = false
) {
    val multiProgress = if (deityCount > 1) " ($doneCount/$deityCount)" else ""
    val instruction = when (step) {
        WorshipStep.LIGHT -> if (hindi) "दीप पर टैप कर जलाएँ" else "Tap oil lamp to light"
        WorshipStep.BATH -> if (hindi) {
            if (deityCount > 1) "लोटा या मूर्ति टैप करें$multiProgress"
            else "जल लोटे से स्नान कराएँ"
        } else {
            if (deityCount > 1) "Tap kalash or deity$multiProgress"
            else "Tap kalash to bathe"
        }
        WorshipStep.TILAK -> if (hindi) {
            if (deityCount > 1) "तिलक कटोरी या मूर्ति टैप करें$multiProgress"
            else "तिलक कटोरी पर टैप करें"
        } else {
            if (deityCount > 1) "Tap tilak or deity$multiProgress"
            else "Tap tilak bowls to apply"
        }
        WorshipStep.FLOWERS -> if (hindi) "थाली से फूल अर्पित करें$multiProgress" else "Tap flowers to offer$multiProgress"
        WorshipStep.CONCH -> if (hindi) "थाली पर शंख बजाएँ" else "Tap shankh to sound"
        WorshipStep.PRASAD -> if (hindi) "थाली से भोग अर्पित करें" else "Tap prasad bowl to offer"
        WorshipStep.AARTI -> if (aartiRunning) {
            if (hindi) "आरती · उँगली से दीप घुमाएँ" else "Aarti · Drag to wave lamp"
        } else {
            if (hindi) "आरती दीप पर टैप करें" else "Tap aarti lamp to begin"
        }
    }
    val number = step.ordinal + 1
    val total = WorshipStep.values().size
    val accessibleInstruction = if (hindi) "चरण $number/$total। $instruction" else "Step $number of $total. $instruction"
    Text(
        text = "$number/$total · $instruction",
        modifier = modifier.testTag("puja-step-guide").clearAndSetSemantics {
            contentDescription = accessibleInstruction
            liveRegion = LiveRegionMode.Polite
        },
        color = RitualInk,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
