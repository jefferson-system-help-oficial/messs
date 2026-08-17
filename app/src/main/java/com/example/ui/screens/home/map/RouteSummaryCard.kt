package com.example.ui.screens.home.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.home.components.PrioritySummaryItem

@Composable
fun RouteSummaryCard(
    optimizedRouteTitle: String,
    routeDistanceKm: Double,
    routeDurationMin: Int,
    stopCount: Int,
    criticoCount: Int,
    altoCount: Int,
    medioCount: Int,
    baixoCount: Int,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF131B2E)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    Card(
        modifier = modifier
            .padding(12.dp)
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = optimizedRouteTitle,
                            fontWeight = FontWeight.Bold,
                            color = textWhite,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                        ) {
                            Text(
                                text = "Ativa",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (routeDistanceKm > 0)
                            "${String.format("%.1f", routeDistanceKm)} km • Est. ~$routeDurationMin min ($stopCount paradas)"
                        else "Calculando trajeto Inhumas ➔ Goianira ➔ Trindade...",
                        fontSize = 11.sp,
                        color = textGray
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar Rota", tint = textGray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF21262D)))
            Spacer(modifier = Modifier.height(10.dp))

            // Priority Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrioritySummaryItem(color = Color(0xFFD32F2F), label = "Crítico", count = criticoCount)
                PrioritySummaryItem(color = Color(0xFFE65100), label = "Alto", count = altoCount)
                PrioritySummaryItem(color = Color(0xFFFBC02D), label = "Médio", count = medioCount)
                PrioritySummaryItem(color = Color(0xFF2E7D32), label = "Baixo", count = baixoCount)
            }
        }
    }
}
