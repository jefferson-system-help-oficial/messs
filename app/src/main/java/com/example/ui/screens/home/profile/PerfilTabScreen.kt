package com.example.ui.screens.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.screens.home.components.ProfileDetailRow

@Composable
fun PerfilTabScreen(
    tasks: List<PoolTaskEntity>,
    onBackToMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    val completedCount = tasks.count { it.isCompleted || it.status == "CONCLUIDA" }
    val pendingCount = tasks.size - completedCount

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
    ) {
        // Navigation Header: "Voltar ao Mapa"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToMap,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cardBg)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar ao Mapa",
                    tint = textWhite
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Perfil do Técnico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textWhite
                )
                Text(
                    text = "Configurações operacionais e credenciais",
                    fontSize = 12.sp,
                    color = textGray
                )
            }
        }

        HorizontalDivider(color = Color(0xFF21262D), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(appScrollableContentPadding(additionalTop = 16.dp, additionalBottom = 24.dp, horizontal = 16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Technician Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(vibrantBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Foto do Perfil",
                            tint = vibrantBlue,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Técnico Especialista",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textWhite
                    )
                    Text(
                        text = "Tratador & Operador de Piscinas",
                        fontSize = 13.sp,
                        color = vibrantBlue,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$completedCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4CAF50))
                            Text(text = "Finalizadas", fontSize = 11.sp, color = textGray)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF30363D)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$pendingCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFBC02D))
                            Text(text = "Pendentes", fontSize = 11.sp, color = textGray)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF30363D)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${tasks.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = vibrantBlue)
                            Text(text = "Total na Rota", fontSize = 11.sp, color = textGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Operational Parameters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "📋 Parâmetros Operacionais",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textWhite
                    )

                    ProfileDetailRow(label = "Empresa", value = "Só Piscinas Ltda")
                    ProfileDetailRow(label = "Região", value = "Goiânia, Inhumas & Goianira")
                    ProfileDetailRow(label = "Veículo de Visita", value = "Moto - Placa GOY-5D21")
                    ProfileDetailRow(label = "Status Diário", value = "Em Rota de Atendimentos")
                    ProfileDetailRow(label = "Versão do App", value = "v2.4.0-Nativo")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "💦 Só Piscinas • Tecnologia em Campo",
                fontSize = 11.sp,
                color = textGray.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
