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

/** A short numbered instruction keeps step context inside the compact ritual dock. */
@Composable
fun PujaStepGuide(step: WorshipStep, hindi: Boolean, modifier: Modifier = Modifier) {
    val instruction = when (step) {
        WorshipStep.LIGHT -> if (hindi) "दीप जलाकर पूजा शुरू करें" else "Light the diya to begin"
        WorshipStep.BATH -> if (hindi) "हर देवता का स्नान कराएँ" else "Bathe each deity"
        WorshipStep.TILAK -> if (hindi) "हर देवता को तिलक लगाएँ" else "Apply tilak to each deity"
        WorshipStep.FLOWERS -> if (hindi) "हर देवता को फूल अर्पित करें" else "Place flowers before each deity"
        WorshipStep.CONCH -> if (hindi) "प्रार्थना में शंख बजाएँ" else "Sound the conch in prayer"
        WorshipStep.PRASAD -> if (hindi) "श्रद्धा से भोग अर्पित करें" else "Offer food with a grateful heart"
        WorshipStep.AARTI -> if (hindi) "आरती शुरू करें और पूरी होने दें" else "Begin the aarti and let it finish"
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
