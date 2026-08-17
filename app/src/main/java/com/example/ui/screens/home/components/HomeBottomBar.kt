package com.example.ui.screens.home.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.home.model.HomeTab

@Composable
fun HomeBottomBar(
    currentTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onNavigateToAdd: (String?) -> Unit,
    onStartServiceClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showQuickActionMenu by remember { mutableStateOf(false) }

    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, bottom = 8.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = cardBg.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263043)),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mapa Tab (Icon only)
                val isMapa = currentTab == HomeTab.MAPA
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isMapa) vibrantBlue.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onTabSelected(HomeTab.MAPA) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "Mapa",
                        tint = if (isMapa) vibrantBlue else textGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Floating Action Button "+"
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(vibrantBlue)
                            .clickable { showQuickActionMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ações Rápidas",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showQuickActionMenu,
                        onDismissRequest = { showQuickActionMenu = false },
                        modifier = Modifier
                            .background(cardBg)
                            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(16.dp))
                            },
                            text = { Text("Novo Cliente", color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showQuickActionMenu = false
                                onNavigateToAdd(null)
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(16.dp))
                            },
                            text = { Text("Importar do WhatsApp", color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showQuickActionMenu = false
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    onNavigateToAdd(clipText)
                                } else {
                                    Toast.makeText(context, "Área de transferência vazia.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(16.dp))
                            },
                            text = { Text("Iniciar Atendimento", color = textWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                showQuickActionMenu = false
                                onStartServiceClick()
                            }
                        )
                    }
                }

                // Calendário Tab (Icon only)
                val isCalendario = currentTab == HomeTab.CALENDARIO
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isCalendario) vibrantBlue.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onTabSelected(HomeTab.CALENDARIO) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Calendário",
                        tint = if (isCalendario) vibrantBlue else textGray.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
