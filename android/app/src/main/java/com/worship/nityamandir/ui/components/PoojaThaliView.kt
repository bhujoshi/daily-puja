package com.worship.nityamandir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.data.model.PoojaItem
import com.worship.nityamandir.ui.theme.SacredGold
import com.worship.nityamandir.ui.theme.SacredTextPrimary

@Composable
fun PoojaThaliView(
    selectedItem: PoojaItem?,
    onItemSelected: (PoojaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // A brass/gold thali background at the bottom
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3B2F15), Color(0xFF1F180A))
                ),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .border(2.dp, SacredGold, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "पूजा सामग्री (Pooja Thali)",
                color = SacredGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Only showing requested items for now
                PoojaItemIcon(item = PoojaItem.FLOWER, icon = "🌸", label = "पुष्प (Flower)", isSelected = selectedItem == PoojaItem.FLOWER, onClick = { onItemSelected(PoojaItem.FLOWER) })
                PoojaItemIcon(item = PoojaItem.KALASH, icon = "💧", label = "जल (Water)", isSelected = selectedItem == PoojaItem.KALASH, onClick = { onItemSelected(PoojaItem.KALASH) })
            }
        }
    }
}

@Composable
private fun PoojaItemIcon(
    item: PoojaItem,
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0x66FFD700) else Color(0x22FFFFFF))
                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) SacredGold else Color.Gray, CircleShape)
        ) {
            Text(text = icon, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) SacredGold else SacredTextPrimary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
