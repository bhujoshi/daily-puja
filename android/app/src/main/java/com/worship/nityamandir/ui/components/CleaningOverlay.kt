package com.worship.nityamandir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.ui.theme.*

@Composable
fun CleaningOverlay(
    dustLevel: Float,
    flowerWitherFactor: Float,
    onCompleteCleaning: () -> Unit,
    modifier: Modifier = Modifier
) {
    var flowersCollected by remember { mutableStateOf(false) }
    var choukiWiped by remember { mutableStateOf(false) }
    var diyaWashed by remember { mutableStateOf(false) }

    val allCleaned = flowersCollected && choukiWiped && diyaWashed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AltarTeakDark.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(SacredGold, SacredBrass))
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = SacredGold
                )
                Text(
                    text = "मंदिर शुद्धि एवं सवेरे की सफाई",
                    color = SacredGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "कल के फूल निर्माल्य हो चुके हैं एवं मंदिर में धूल की परत आ गई है।\n(धूल स्तर: ${(dustLevel * 100).toInt()}% • निर्माल्य: ${(flowerWitherFactor * 100).toInt()}%)",
                color = SacredTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // Step 1: Nirmalya Collection
            CleanTaskRow(
                title = "1. निर्माल्य संग्रह (Nirmalya Collection)",
                subtitle = "बासी पुष्पों को उठाकर पवित्र निर्माल्य पात्र में रखें",
                isDone = flowersCollected,
                onClick = { flowersCollected = true }
            )

            // Step 2: Wiping the Chouki
            CleanTaskRow(
                title = "2. चौकी मार्जन (Wiping Altar)",
                subtitle = "गीले स्वच्छ वस्त्र से मंदिर की चौकी की धूल पोंछें",
                isDone = choukiWiped,
                onClick = { choukiWiped = true }
            )

            // Step 3: Washing the Diya
            CleanTaskRow(
                title = "3. दीपक प्रक्षालन (Washing Diya)",
                subtitle = "दीपक को धोकर स्वच्छ करें व नवीन बाती लगाएं",
                isDone = diyaWashed,
                onClick = { diyaWashed = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onCompleteCleaning,
                enabled = allCleaned,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SacredDeepSaffron,
                    disabledContainerColor = Color(0xFF4A3B32)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (allCleaned) "शुद्धिकरण पूर्ण • अब पूजा आरंभ करें" else "तीनों चरण पूर्ण करें",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun CleanTaskRow(
    title: String,
    subtitle: String,
    isDone: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDone) Color(0x334CAF50) else Color(0x22FFFFFF))
            .border(
                1.dp,
                if (isDone) Color(0xFF4CAF50) else Color(0x44FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SacredTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = SacredTextSecondary,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) Color(0xFF4CAF50) else Color.Transparent)
                .border(1.5.dp, if (isDone) Color(0xFF4CAF50) else SacredBrass, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
