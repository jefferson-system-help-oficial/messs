package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.PoolTaskEntity
import com.example.ui.screens.home.calendar.CalendarioTabScreen
import com.example.ui.screens.home.components.HomeBottomBar
import com.example.ui.screens.home.components.StartServiceDialog
import com.example.ui.screens.home.history.HistoricoTabScreen
import com.example.ui.screens.home.map.MapTabScreen
import com.example.ui.screens.home.model.HomeTab
import com.example.ui.screens.home.profile.PerfilTabScreen
import com.example.ui.screens.home.tasks.AtendimentosTabScreen
import com.example.ui.viewmodel.PoolViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HomeScreen(
    viewModel: PoolViewModel,
    onNavigateToAdd: (String?) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()

    var currentTab by remember { mutableStateOf(HomeTab.MAPA) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>("TODOS") }
    var showStartServiceDialog by remember { mutableStateOf(false) }

    // Intercept hardware back button when not on Map tab
    BackHandler(enabled = currentTab != HomeTab.MAPA) {
        currentTab = HomeTab.MAPA
    }

    // Routing & GPS State
    var isRouteOptimized by remember { mutableStateOf(false) }
    var streetRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeDistanceKm by remember { mutableStateOf(0.0) }
    var routeDurationMin by remember { mutableStateOf(0) }

    // Real GPS Location state (defaulting to Inhumas/Goiânia region base)
    var realGpsLat by remember { mutableStateOf(-16.3575) }
    var realGpsLng by remember { mutableStateOf(-49.4975) }
    var hasRealGps by remember { mutableStateOf(false) }

    // Location Permission & Updates
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    realGpsLat = lastKnown.latitude
                    realGpsLng = lastKnown.longitude
                    hasRealGps = true
                }
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    5f,
                    object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            realGpsLat = loc.latitude
                            realGpsLng = loc.longitude
                            hasRealGps = true
                        }
                        override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) {}
                    }
                )
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    realGpsLat = lastKnown.latitude
                    realGpsLng = lastKnown.longitude
                    hasRealGps = true
                }
            } catch (_: Exception) {}
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Filter tasks based on Search query & selected Filter category
    val filteredTasks = remember(tasks, searchQuery, selectedFilter) {
        tasks.filter { task ->
            val matchesSearch = searchQuery.isBlank() ||
                    task.clientName.contains(searchQuery, ignoreCase = true) ||
                    task.address.contains(searchQuery, ignoreCase = true) ||
                    task.serviceType.contains(searchQuery, ignoreCase = true)

            val displayStatus = task.displayStatus
            val matchesFilter = when (selectedFilter) {
                null, "TODOS" -> true
                "TRATAMENTO", "TRATAMENTO_PENDENTE" -> displayStatus == "TRATAMENTO_PENDENTE" || (!task.isCompleted && !task.isWaterTreated)
                "LIMPEZA", "AGUARDANDO_LIMPEZA" -> displayStatus == "AGUARDANDO_LIMPEZA" || (!task.isCompleted && task.cleaningState == "AGUARDANDO_LIMPEZA")
                "CONCLUIDA" -> displayStatus == "CONCLUIDA" || task.isCompleted
                "ATRASADO", "ATRASADA" -> displayStatus == "ATRASADA"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    // OSRM Street Route Calculation
    LaunchedEffect(isRouteOptimized, filteredTasks, realGpsLat, realGpsLng) {
        if (isRouteOptimized && filteredTasks.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val sorted = filteredTasks.sortedBy { task ->
                        val dLat = Math.toRadians(task.latitude - realGpsLat)
                        val dLon = Math.toRadians(task.longitude - realGpsLng)
                        val a = sin(dLat / 2) * sin(dLat / 2) +
                                cos(Math.toRadians(realGpsLat)) * cos(Math.toRadians(task.latitude)) *
                                sin(dLon / 2) * sin(dLon / 2)
                        2 * atan2(sqrt(a), sqrt(1 - a))
                    }

                    val coordString = buildString {
                        append("$realGpsLng,$realGpsLat;")
                        sorted.forEachIndexed { i, t ->
                            append("${t.longitude},${t.latitude}")
                            if (i < sorted.size - 1) append(";")
                        }
                    }

                    val url = URL("https://router.project-osrm.org/route/v1/driving/$coordString?overview=full&geometries=geojson")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 7000
                    conn.readTimeout = 7000
                    conn.requestMethod = "GET"

                    if (conn.responseCode == 200) {
                        val resp = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(resp)
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val r = routes.getJSONObject(0)
                            val dist = r.getDouble("distance") / 1000.0
                            val dur = (r.getDouble("duration") / 60.0).toInt()
                            val geom = r.getJSONObject("geometry")
                            val coords = geom.getJSONArray("coordinates")
                            val pts = mutableListOf<GeoPoint>()
                            for (k in 0 until coords.length()) {
                                val pt = coords.getJSONArray(k)
                                pts.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
                            }

                            withContext(Dispatchers.Main) {
                                streetRoutePoints = pts
                                routeDistanceKm = dist
                                routeDurationMin = dur
                            }
                        }
                    } else {
                        // Fallback straight line
                        val fallbackPts = mutableListOf<GeoPoint>()
                        fallbackPts.add(GeoPoint(realGpsLat, realGpsLng))
                        sorted.forEach { fallbackPts.add(GeoPoint(it.latitude, it.longitude)) }
                        withContext(Dispatchers.Main) {
                            streetRoutePoints = fallbackPts
                        }
                    }
                } catch (_: Exception) {
                    val fallbackPts = mutableListOf<GeoPoint>()
                    fallbackPts.add(GeoPoint(realGpsLat, realGpsLng))
                    filteredTasks.forEach { fallbackPts.add(GeoPoint(it.latitude, it.longitude)) }
                    withContext(Dispatchers.Main) {
                        streetRoutePoints = fallbackPts
                    }
                }
            }
        } else {
            streetRoutePoints = emptyList()
            routeDistanceKm = 0.0
            routeDurationMin = 0
        }
    }

    Scaffold(
        containerColor = Color(0xFF0D1117),
        bottomBar = {
            if (currentTab != HomeTab.PERFIL) {
                HomeBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    onNavigateToAdd = onNavigateToAdd,
                    onStartServiceClick = { showStartServiceDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117))
        ) {
            when (currentTab) {
                HomeTab.MAPA -> {
                    MapTabScreen(
                        tasks = filteredTasks,
                        unfilteredTasks = tasks,
                        selectedFilter = selectedFilter,
                        isRouteOptimized = isRouteOptimized,
                        streetRoutePoints = streetRoutePoints,
                        routeDistanceKm = routeDistanceKm,
                        routeDurationMin = routeDurationMin,
                        startLat = realGpsLat,
                        startLng = realGpsLng,
                        startName = if (hasRealGps) "Minha Posição GPS" else "Base Operacional (Inhumas)",
                        onFilterChange = { selectedFilter = it },
                        onSetRouteOptimized = { isRouteOptimized = it },
                        onNavigateToAtendimentos = { currentTab = HomeTab.ATENDIMENTOS },
                        onNavigateToHistory = { currentTab = HomeTab.HISTORICO },
                        onNavigateToProfile = { currentTab = HomeTab.PERFIL }
                    )
                }

                HomeTab.CALENDARIO -> {
                    CalendarioTabScreen(
                        tasks = tasks,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToAdd = { onNavigateToAdd(null) },
                        onNavigateToProfile = { currentTab = HomeTab.PERFIL },
                        onBackToMap = { currentTab = HomeTab.MAPA }
                    )
                }

                HomeTab.ATENDIMENTOS -> {
                    AtendimentosTabScreen(
                        tasks = filteredTasks,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSearchChange = { searchQuery = it },
                        onFilterChange = { selectedFilter = it },
                        onNavigateToAdd = { onNavigateToAdd(null) },
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToProfile = { currentTab = HomeTab.PERFIL },
                        onBackToMap = { currentTab = HomeTab.MAPA }
                    )
                }

                HomeTab.HISTORICO -> {
                    HistoricoTabScreen(
                        tasks = tasks,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToProfile = { currentTab = HomeTab.PERFIL },
                        onBackToMap = { currentTab = HomeTab.MAPA }
                    )
                }

                HomeTab.PERFIL -> {
                    PerfilTabScreen(
                        tasks = tasks,
                        onBackToMap = { currentTab = HomeTab.MAPA }
                    )
                }
            }

            if (showStartServiceDialog) {
                StartServiceDialog(
                    tasks = tasks,
                    onDismiss = { showStartServiceDialog = false },
                    onTaskSelected = { taskId ->
                        onNavigateToDetail(taskId)
                    }
                )
            }
        }
    }
}
