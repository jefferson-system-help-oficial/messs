package com.example.ui.screens.home.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.PoolTaskEntity
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapTabScreen(
    tasks: List<PoolTaskEntity>,
    unfilteredTasks: List<PoolTaskEntity>,
    selectedFilter: String?,
    isRouteOptimized: Boolean,
    streetRoutePoints: List<GeoPoint>,
    routeDistanceKm: Double,
    routeDurationMin: Int,
    startLat: Double,
    startLng: Double,
    startName: String,
    onFilterChange: (String) -> Unit,
    onSetRouteOptimized: (Boolean) -> Unit,
    onNavigateToAtendimentos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var selectedMapType by remember { mutableStateOf("DARK") }
    var showMapTypeDialog by remember { mutableStateOf(false) }
    var showClientFilterDialog by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }

    // Priority counts for the active route card
    val criticoCount = remember(unfilteredTasks) { unfilteredTasks.count { it.status == "VERMELHO" && !it.isCompleted } }
    val altoCount = remember(unfilteredTasks) { unfilteredTasks.count { it.status == "LARANJA" && !it.isCompleted } }
    val medioCount = remember(unfilteredTasks) { unfilteredTasks.count { it.status == "AMARELO" && !it.isCompleted } }
    val baixoCount = remember(unfilteredTasks) { unfilteredTasks.count { it.status == "VERDE" || it.isCompleted } }

    Box(modifier = modifier.fillMaxSize()) {
        HomeMapView(
            tasks = tasks,
            isRouteOptimized = isRouteOptimized,
            streetRoutePoints = streetRoutePoints,
            selectedMapType = selectedMapType,
            startLat = startLat,
            startLng = startLng,
            startName = startName,
            onMapViewReady = { mapViewRef = it }
        )

        MapFloatingControls(
            selectedFilter = selectedFilter,
            selectedMapType = selectedMapType,
            isRouteOptimized = isRouteOptimized,
            onNavigateToAtendimentos = onNavigateToAtendimentos,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToProfile = onNavigateToProfile,
            onFilterClick = { showClientFilterDialog = true },
            onOptimizeClick = { showOptimizeDialog = true },
            onMapTypeClick = { showMapTypeDialog = true },
            onZoomIn = { mapViewRef?.controller?.zoomIn() },
            onZoomOut = { mapViewRef?.controller?.zoomOut() },
            onRecenterGps = {
                mapViewRef?.controller?.animateTo(GeoPoint(startLat, startLng))
                mapViewRef?.controller?.setZoom(15.0)
            }
        )

        // Active Route Summary Card
        if (isRouteOptimized) {
            val optimizedRouteTitle = when (selectedFilter) {
                "TRATAMENTO" -> "🧪 Rota: Tratamentos Químicos"
                "LIMPEZA" -> "🧹 Rota: Limpeza & Aspiração"
                "ATRASADO" -> "⚠️ Rota: Atendimentos Atrasados"
                else -> "🛣️ Rota Otimizada Completa"
            }

            RouteSummaryCard(
                optimizedRouteTitle = optimizedRouteTitle,
                routeDistanceKm = routeDistanceKm,
                routeDurationMin = routeDurationMin,
                stopCount = tasks.size,
                criticoCount = criticoCount,
                altoCount = altoCount,
                medioCount = medioCount,
                baixoCount = baixoCount,
                onCloseClick = { onSetRouteOptimized(false) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Dialogs
        if (showClientFilterDialog) {
            ClientFilterDialog(
                selectedFilter = selectedFilter,
                onFilterSelected = {
                    onFilterChange(it)
                    showClientFilterDialog = false
                },
                onDismiss = { showClientFilterDialog = false }
            )
        }

        if (showMapTypeDialog) {
            MapTypeDialog(
                selectedMapType = selectedMapType,
                onMapTypeSelected = {
                    selectedMapType = it
                    showMapTypeDialog = false
                },
                onDismiss = { showMapTypeDialog = false }
            )
        }

        if (showOptimizeDialog) {
            OptimizeRouteDialog(
                isRouteOptimized = isRouteOptimized,
                selectedFilter = selectedFilter,
                onOptimizeCategorySelected = { category ->
                    onFilterChange(category)
                    onSetRouteOptimized(true)
                    showOptimizeDialog = false
                },
                onDisableRoute = {
                    onSetRouteOptimized(false)
                    showOptimizeDialog = false
                },
                onDismiss = { showOptimizeDialog = false }
            )
        }
    }
}
