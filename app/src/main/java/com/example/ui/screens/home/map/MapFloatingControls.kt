package com.example.ui.screens.home.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.layout.appFloatingSide
import com.example.ui.layout.appFloatingTop

@Composable
fun MapFloatingControls(
    selectedFilter: String?,
    selectedMapType: String,
    isRouteOptimized: Boolean,
    onNavigateToAtendimentos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onFilterClick: () -> Unit,
    onOptimizeClick: () -> Unit,
    onMapTypeClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenterGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)

    Box(modifier = modifier.fillMaxSize()) {
        // Floating Top-Left Navigation Column (Atendimentos on TOP, Histórico BELOW)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .appFloatingTop(topOffset = 12.dp, horizontalOffset = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Atendimentos (Lista de Chamados e Clientes)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateToAtendimentos() },
                color = cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D).copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Atendimentos",
                        tint = vibrantBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Histórico (Abaixo de Atendimentos)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateToHistory() },
                color = cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D).copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Histórico",
                        tint = textWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Top-Right Navigation: Perfil Shortcut Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .appFloatingTop(topOffset = 12.dp, horizontalOffset = 16.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable { onNavigateToProfile() },
            color = cardBg.copy(alpha = 0.92f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D).copy(alpha = 0.6f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Perfil do Técnico",
                    tint = vibrantBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Floating Vertical Actions Column (Center Right)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .appFloatingSide(horizontalOffset = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4. Filtro de Serviços / Status
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onFilterClick() },
                color = if (selectedFilter != null && selectedFilter != "TODOS") vibrantBlue else cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Filtro de Serviços",
                        tint = if (selectedFilter != null && selectedFilter != "TODOS") Color.White else textWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Otimizar Rota
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOptimizeClick() },
                color = if (isRouteOptimized) vibrantBlue else cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AltRoute,
                        contentDescription = "Otimizar Rota",
                        tint = if (isRouteOptimized) Color.White else textWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Camadas / Tipo de Mapa
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onMapTypeClick() },
                color = if (selectedMapType != "DARK") vibrantBlue else cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = "Tipo de Mapa",
                        tint = if (selectedMapType != "DARK") Color.White else textWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom In & Out (+ / -)
            Surface(
                modifier = Modifier.width(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D).copy(alpha = 0.6f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp, 38.dp)
                            .clickable { onZoomIn() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom +", tint = textWhite, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(
                        color = Color(0xFF30363D).copy(alpha = 0.8f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp, 38.dp)
                            .clickable { onZoomOut() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = textWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 1. Minha Localização (GPS Re-center)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onRecenterGps() },
                color = cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização", tint = vibrantBlue, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
