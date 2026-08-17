package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
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

@Composable
fun StartServiceDialog(
    tasks: List<PoolTaskEntity>,
    onDismiss: () -> Unit,
    onTaskSelected: (Long) -> Unit
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule, 
                    contentDescription = null, 
                    tint = vibrantBlue, 
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "⏱️ Iniciar Atendimento", 
                    color = textWhite, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Selecione um cliente cadastrado para iniciar o atendimento:", 
                    fontSize = 12.sp, 
                    color = textGray
                )

                if (tasks.isEmpty()) {
                    Text("Nenhum cliente cadastrado no momento.", fontSize = 13.sp, color = textGray)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(tasks) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF21262D).copy(alpha = 0.5f))
                                    .clickable {
                                        onDismiss()
                                        onTaskSelected(task.id)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text(
                                        task.clientName, 
                                        fontWeight = FontWeight.Bold, 
                                        color = textWhite, 
                                        fontSize = 13.sp, 
                                        maxLines = 1
                                    )
                                    Text(task.serviceType, color = textGray, fontSize = 11.sp, maxLines = 1)
                                }
                                Icon(
                                    Icons.Default.ArrowForward, 
                                    contentDescription = null, 
                                    tint = vibrantBlue, 
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = textGray, fontWeight = FontWeight.Bold)
            }
        }
    )
}
