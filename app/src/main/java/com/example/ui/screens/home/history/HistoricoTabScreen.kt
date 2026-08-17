package com.example.ui.screens.home.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PoolTaskEntity
import com.example.ui.layout.appScrollableContentPadding

@Composable
fun HistoricoTabScreen(
    tasks: List<PoolTaskEntity>,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    val completedTasks = tasks.filter { it.isCompleted || it.status == "CONCLUIDA" }
    val totalVolume = completedTasks.sumOf { it.poolSizeLiters }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToMap,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .border(1.dp, Color(0xFF30363D), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar ao Mapa",
                        tint = textWhite
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Histórico & Desempenho",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textWhite
                    )
                    Text(
                        text = "Relatório diário de rotas e conclusões",
                        fontSize = 12.sp,
                        color = textGray
                    )
                }
            }

            IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, Color(0xFF30363D), CircleShape)
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = vibrantBlue, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Completed Visits
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${completedTasks.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    Text(text = "Visitas Concluídas", fontSize = 11.sp, color = textGray)
                }
            }

            // Total Volume Treated
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "%,d L".format(totalVolume), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    Text(text = "Volume Tratado", fontSize = 11.sp, color = textGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Atendimentos Finalizados",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (completedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = textGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum atendimento finalizado hoje.", color = textGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = appScrollableContentPadding(additionalTop = 4.dp, additionalBottom = 16.dp, horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(completedTasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(task.id) },
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.clientName, fontWeight = FontWeight.Bold, color = textWhite, fontSize = 14.sp)
                                Text("📍 ${task.address}", color = textGray, fontSize = 11.sp, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("💧 %,d L • ${task.serviceType}".format(task.poolSizeLiters), color = vibrantBlue, fontSize = 11.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                            ) {
                                Text(
                                    text = "Concluído",
                                    fontSize = 10.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
