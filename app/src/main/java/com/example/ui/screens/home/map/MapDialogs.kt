package com.example.ui.screens.home.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.home.components.MapTypeOptionItem

@Composable
fun ClientFilterDialog(
    selectedFilter: String?,
    onFilterSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg.copy(alpha = 0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Filtrar Serviços & Clientes", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MapTypeOptionItem(
                    title = "📍 Todos os Clientes",
                    subtitle = "Exibe todas as paradas no mapa",
                    selected = selectedFilter == null || selectedFilter == "TODOS",
                    onClick = { onFilterSelected("TODOS") }
                )
                MapTypeOptionItem(
                    title = "🔴 Tratamento Pendente",
                    subtitle = "Piscinas que necessitam de choque/químicos",
                    selected = selectedFilter == "TRATAMENTO_PENDENTE",
                    onClick = { onFilterSelected("TRATAMENTO_PENDENTE") }
                )
                MapTypeOptionItem(
                    title = "🟡 Aguardando Limpeza",
                    subtitle = "Piscinas tratadas aguardando aspiração/limpeza",
                    selected = selectedFilter == "AGUARDANDO_LIMPEZA",
                    onClick = { onFilterSelected("AGUARDANDO_LIMPEZA") }
                )
                MapTypeOptionItem(
                    title = "🟢 Concluídas (Tratadas & Limpas)",
                    subtitle = "Atendimentos com ciclo 100% finalizado",
                    selected = selectedFilter == "CONCLUIDA",
                    onClick = { onFilterSelected("CONCLUIDA") }
                )
                MapTypeOptionItem(
                    title = "🟠 Chamados Atrasados",
                    subtitle = "Atendimentos que passaram da data programada",
                    selected = selectedFilter == "ATRASADA",
                    onClick = { onFilterSelected("ATRASADA") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = vibrantBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun MapTypeDialog(
    selectedMapType: String,
    onMapTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Estilo do Mapa", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MapTypeOptionItem(
                    title = "🌙 Modo Escuro",
                    subtitle = "Navegação noturna otimizada",
                    selected = selectedMapType == "DARK",
                    onClick = { onMapTypeSelected("DARK") }
                )
                MapTypeOptionItem(
                    title = "☀️ Modo Padrão (Claro)",
                    subtitle = "Vias e cidades vetorizadas",
                    selected = selectedMapType == "LIGHT",
                    onClick = { onMapTypeSelected("LIGHT") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = vibrantBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OptimizeRouteDialog(
    isRouteOptimized: Boolean,
    selectedFilter: String?,
    onOptimizeCategorySelected: (String) -> Unit,
    onDisableRoute: () -> Unit,
    onDismiss: () -> Unit
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg.copy(alpha = 0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AltRoute, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Otimizar Rota por Serviço", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Selecione a prioridade para gerar a melhor sequência de paradas:",
                    fontSize = 12.sp,
                    color = textGray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                MapTypeOptionItem(
                    title = "🧪 Rota de Tratamentos Químicos",
                    subtitle = "Otimiza apenas chamados de choque e cloro",
                    selected = isRouteOptimized && selectedFilter == "TRATAMENTO",
                    onClick = { onOptimizeCategorySelected("TRATAMENTO") }
                )

                MapTypeOptionItem(
                    title = "🧹 Rota de Limpeza & Aspiração",
                    subtitle = "Otimiza apenas manutenção de rotina",
                    selected = isRouteOptimized && selectedFilter == "LIMPEZA",
                    onClick = { onOptimizeCategorySelected("LIMPEZA") }
                )

                MapTypeOptionItem(
                    title = "⚠️ Rota de Chamados Atrasados",
                    subtitle = "Prioriza paradas críticas e em atraso",
                    selected = isRouteOptimized && selectedFilter == "ATRASADO",
                    onClick = { onOptimizeCategorySelected("ATRASADO") }
                )

                MapTypeOptionItem(
                    title = "🚀 Rota Completa (Todas as Paradas)",
                    subtitle = "Otimiza a rota sequencial de todos os clientes",
                    selected = isRouteOptimized && (selectedFilter == null || selectedFilter == "TODOS"),
                    onClick = { onOptimizeCategorySelected("TODOS") }
                )
            }
        },
        confirmButton = {
            Row {
                if (isRouteOptimized) {
                    TextButton(onClick = onDisableRoute) {
                        Text("Desativar Rota", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = vibrantBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
