package com.example.ui.screens.home.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PoolTaskEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Card de exibição de um atendimento na lista de tarefas.
 * Mostra nome do cliente, endereço, status geral, status de água e limpeza,
 * tipo de serviço e volume da piscina.
 *
 * @param task Entidade da tarefa
 * @param onClick Callback ao clicar no card (navega para detalhes)
 * @param modifier Modifier para estilização externa
 */
@Composable
fun TaskListItemCard(
    task: PoolTaskEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Cores (idealmente viriam de um tema global)
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)
    val borderColor = Color(0xFF30363D)

    // Status geral (displayStatus) – usa os mesmos mapeamentos da tela principal
    val statusInfo = TaskStatusUtils.getStatusInfo(task.displayStatus)
    val statusColor = statusInfo.color
    val statusLabel = statusInfo.label

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra vertical à esquerda indicando o status geral
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                // Linha superior: nome do cliente + badge de status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.clientName,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )

                    StatusBadge(
                        label = statusLabel,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Endereço
                Text(
                    text = "📍 ${task.address}",
                    fontSize = 12.sp,
                    color = textGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Linha de status duplo: Água e Limpeza
                DualStatusRow(
                    isWaterTreated = task.isWaterTreated,
                    isCleaningDone = task.isCleaningDone,
                    cleaningState = task.cleaningState,
                    cleaningScheduledDate = task.cleaningScheduledDate
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rodapé: tipo de serviço e volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF21262D),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = "🛠️ ${task.serviceType}",
                            fontSize = 11.sp,
                            color = vibrantBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "💧 %,d L".format(task.poolSizeLiters),
                        fontSize = 11.sp,
                        color = textGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ================================================================================================
// COMPONENTES AUXILIARES
// ================================================================================================

@Composable
private fun StatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DualStatusRow(
    isWaterTreated: Boolean,
    isCleaningDone: Boolean,
    cleaningState: String,
    cleaningScheduledDate: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Badge de Água
        val waterColor = if (isWaterTreated) Color(0xFF4CAF50) else Color(0xFFEF5350)
        val waterBg = if (isWaterTreated) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFB71C1C).copy(alpha = 0.3f)
        val waterBorder = if (isWaterTreated) Color(0xFF4CAF50).copy(alpha = 0.6f) else Color(0xFFE53935).copy(alpha = 0.6f)
        val waterLabel = if (isWaterTreated) "💧 Água: Tratada" else "💧 Água: Pendente"

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = waterBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, waterBorder),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = waterLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = waterColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                maxLines = 1
            )
        }

        // Badge de Limpeza
        val cleanColor = when {
            isCleaningDone -> Color(0xFF4CAF50)
            cleaningState == "AGUARDANDO_LIMPEZA" -> Color(0xFFFBC02D)
            else -> Color(0xFFE53935)
        }
        val cleanBg = cleanColor.copy(alpha = 0.15f)
        val cleanBorder = cleanColor.copy(alpha = 0.6f)
        val cleanLabel = when {
            isCleaningDone -> "🧹 Limpeza: Feita"
            cleaningState == "AGUARDANDO_LIMPEZA" -> "🧹 Limpar: ${cleaningScheduledDate.ifBlank { "Amanhã" }}"
            else -> "🧹 Limpeza: Pendente"
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = cleanBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, cleanBorder),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = cleanLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = cleanColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                maxLines = 1
            )
        }
    }
}

// ================================================================================================
// UTILITÁRIO DE STATUS (compartilhado com a tela principal)
// ================================================================================================

object TaskStatusUtils {
    data class StatusInfo(val color: Color, val label: String)

    fun getStatusInfo(status: String): StatusInfo {
        return when (status) {
            "CONCLUIDA" -> StatusInfo(Color(0xFF4CAF50), "🟢 CONCLUÍDA")
            "AGUARDANDO_LIMPEZA" -> StatusInfo(Color(0xFFFBC02D), "🟡 AGUARDANDO LIMPEZA")
            "TRATAMENTO_PENDENTE" -> StatusInfo(Color(0xFFE53935), "🔴 PRECISA TRATAR")
            "ATRASADA" -> StatusInfo(Color(0xFFFB8C00), "🟠 ATRASADA")
            "AGENDADA" -> StatusInfo(Color(0xFF1E88E5), "🔵 AGENDADA")
            else -> StatusInfo(Color(0xFFFBC02D), status)
        }
    }

    fun isPending(task: PoolTaskEntity): Boolean =
        task.date.isBlank() && task.cleaningScheduledDate.isBlank()

    fun isAwaitingCleaning(task: PoolTaskEntity): Boolean =
        task.cleaningScheduledDate.isNotBlank()

    fun isCompleted(task: PoolTaskEntity): Boolean =
        task.date.isNotBlank() && task.cleaningScheduledDate.isBlank()

    fun isScheduled(task: PoolTaskEntity): Boolean =
        task.cleaningScheduledDate.isNotBlank() && task.date.isBlank()

    fun isOverdue(task: PoolTaskEntity, todayIso: String): Boolean {
        if (task.date.isBlank()) return false
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val taskDate = format.parse(task.date)
            val today = format.parse(todayIso)
            taskDate != null && today != null && taskDate.before(today)
        } catch (_: Exception) {
            false
        }
    }
}