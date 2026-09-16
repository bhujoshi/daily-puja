package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.ui.theme.*

@Composable
fun CurtainView(
    isClosed: Boolean,
    isCleanlinessConfirmed: Boolean,
    onConfirmPurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSnanMantra by remember { mutableStateOf(false) }

    // Curtain parting animation: 0f = fully closed, 1f = fully parted
    val openProgress by animateFloatAsState(
        targetValue = if (isClosed) 0f else 1f,
        animationSpec = tween(durationMillis = 1200),
        label = "curtainAnimation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Left Curtain Panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterStart)
                .offset(x = (-300 * openProgress).dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SilkCurtainRed,
                            Color(0xFF6B0000),
                            SilkCurtainRed
                        )
                    )
                )
                .border(2.dp, SilkCurtainGoldTrim)
        ) {
            // Gold Tassel / Border on inner edge
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.verticalGradient(
                            listOf(SacredGold, Color(0xFFB8860B), SacredGold)
                        )
                    )
            )
        }

        // Right Curtain Panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.CenterEnd)
                .offset(x = (300 * openProgress).dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            SilkCurtainRed,
                            Color(0xFF6B0000),
                            SilkCurtainRed
                        )
                    )
                )
                .border(2.dp, SilkCurtainGoldTrim)
        ) {
            // Gold Tassel / Border on inner edge
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(SacredGold, Color(0xFFB8860B), SacredGold)
                        )
                    )
            )
        }

        // Purity Confirmation Dialog (appears when curtains are closed and purity not yet confirmed)
        if (isClosed && !isCleanlinessConfirmed) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = AltarTeakWood),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SacredGold, SacredBrass)))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ॐ शुद्धि संकल्प ॐ",
                        color = SacredGold,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "क्या आपने स्नान कर शुद्धि प्राप्त कर ली है?\n(Have you taken a bath & cleansed yourself?)",
                        color = SacredTextPrimary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "मंदिर में प्रवेश करने से पूर्व तन और मन की शुचिता अनिवार्य है।",
                        color = SacredTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    if (showSnanMantra) {
                        Surface(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "गङ्गे च यमुने चैव गोदावरि सरस्वति।\nनर्मदे सिन्धु कावेरि जलेऽस्मिन् संनिधिं कुरु॥",
                                color = SacredGold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showSnanMantra = !showSnanMantra },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(SacredBrass, SacredGold)))
                        ) {
                            Text(
                                text = if (showSnanMantra) "मंत्र छिपाएं" else "स्नान मंत्र",
                                color = SacredGold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onConfirmPurity,
                            colors = ButtonDefaults.buttonColors(containerColor = SacredDeepSaffron),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = "हाँ, मैं शुद्ध हूँ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
