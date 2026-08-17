package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrioritySummaryItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = label, 
                fontSize = 10.sp, 
                color = Color(0xFF94A3B8), 
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count parada${if (count != 1) "s" else ""}", 
                fontSize = 11.sp, 
                color = Color(0xFFF0F4F8), 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MapTypeOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (selected) vibrantBlue.copy(alpha = 0.2f) else Color(0xFF21262D),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, vibrantBlue) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textWhite)
                Text(text = subtitle, fontSize = 11.sp, color = textGray)
            }
            if (selected) {
                Icon(
                    Icons.Default.Check, 
                    contentDescription = null, 
                    tint = vibrantBlue, 
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileDetailRow(
    label: String, 
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF0F4F8))
    }
}
