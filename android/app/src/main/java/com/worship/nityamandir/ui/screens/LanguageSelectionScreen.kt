package com.worship.nityamandir.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.ui.components.RitualChoice
import com.worship.nityamandir.ui.components.RitualGlassPanel
import com.worship.nityamandir.ui.components.RitualInk
import com.worship.nityamandir.ui.components.ShrineControlsTheme
import com.worship.nityamandir.ui.theme.DarkSanctumBackground
import com.worship.nityamandir.ui.theme.SacredGold
import com.worship.nityamandir.ui.theme.SacredTextPrimary

/** A language choice is the first interaction on a new installation. */
@Composable
fun LanguageSelectionScreen(onSelectLanguage: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().background(DarkSanctumBackground)
            .testTag("language-selection")
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ॐ", color = SacredGold, fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "पवित्र मंदिर\nPavitra Mandir",
            color = SacredTextPrimary, fontSize = 26.sp,
            fontFamily = FontFamily.Serif, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        ShrineControlsTheme {
            RitualGlassPanel(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                Text(
                    "भाषा चुनें\nChoose your language",
                    modifier = Modifier.fillMaxWidth(),
                    color = RitualInk, fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                RitualChoice("हिंदी", false, { onSelectLanguage(true) }, Modifier.fillMaxWidth())
                RitualChoice("English", false, { onSelectLanguage(false) }, Modifier.fillMaxWidth())
                Text(
                    "भाषा बाद में भी बदल सकते हैं।\nYou can change this later in the menu.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = RitualInk, fontSize = 14.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}
