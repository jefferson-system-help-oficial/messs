package com.example.ui.screens.home.model

import org.osmdroid.util.GeoPoint

enum class HomeTab(val label: String) {
    MAPA("Mapa"),
    CALENDARIO("Calendário"),
    ATENDIMENTOS("Atendimentos"),
    HISTORICO("Histórico"),
    PERFIL("Perfil")
}

enum class MapLayerType(val key: String, val title: String, val subtitle: String) {
    DARK("DARK", "🌙 Modo Escuro", "Navegação noturna otimizada"),
    LIGHT("LIGHT", "☀️ Modo Padrão (Claro)", "Vias e cidades vetorizadas"),
    SATELLITE("SATELLITE", "🛰️ Satélite", "Imagens aéreas de alta resolução"),
    TOPO("TOPO", "🏔️ Topográfico", "Curvas de nível e relevo")
}

data class RouteOptimizationState(
    val isOptimized: Boolean = false,
    val streetPoints: List<GeoPoint> = emptyList(),
    val distanceKm: Double = 0.0,
    val durationMin: Int = 0,
    val activeFilterCategory: String? = null
)
