package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.viewmodel.PoolViewModel
import com.example.ui.layout.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsmMapScreen(
    viewModel: PoolViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    var isRouteOptimized by remember { mutableStateOf(false) }

    // Default to GPS active for real-time location tracking
    var startLocationType by remember { mutableStateOf("GPS") } 
    
    // Real GPS Location state
    var realGpsLat by remember { mutableStateOf(-16.6869) }
    var realGpsLng by remember { mutableStateOf(-49.2648) }
    var hasRealGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf("Buscando GPS real...") }

    // Map Type / Layer State ("DARK", "SATELLITE", "TOPO", "LIGHT")
    var selectedMapType by remember { mutableStateOf("DARK") }
    var currentAppliedType by remember { mutableStateOf("") }
    var showMapTypeDialog by remember { mutableStateOf(false) }
    var showClientFilterDialog by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }

    // Selected task for detailed bottom sheet & photo/address edits
    var selectedTaskForDetails by remember { mutableStateOf<com.example.data.PoolTaskEntity?>(null) }
    var showAdjustAddressDialog by remember { mutableStateOf(false) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && selectedTaskForDetails != null) {
            val updated = selectedTaskForDetails!!.copy(photoUri = uri.toString())
            viewModel.updateTask(updated)
            selectedTaskForDetails = updated
            android.widget.Toast.makeText(context, "Foto do local atualizada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Collapsible Route Summary State
    var isRouteSummaryExpanded by remember { mutableStateOf(false) }

    // Map view reference for controls
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Google Maps Style Pulsing Halo Animation for User Location
    val infiniteTransition = rememberInfiniteTransition(label = "haloTransition")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloPulse"
    )

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        realGpsLat = location.latitude
                        realGpsLng = location.longitude
                        hasRealGps = true
                        gpsStatusMessage = "GPS Real Ativo"
                    } else {
                        gpsStatusMessage = "Aguardando sinal GPS..."
                    }
                }.addOnFailureListener {
                    gpsStatusMessage = "Erro no GPS"
                }
            } catch (e: SecurityException) {
                gpsStatusMessage = "Permissão negada"
            }
        } else {
            gpsStatusMessage = "Permissão negada"
        }
    }

    LaunchedEffect(startLocationType) {
        if (startLocationType == "GPS") {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    val currentStartLat = if (hasRealGps) realGpsLat else -16.6869
    val currentStartLng = if (hasRealGps) realGpsLng else -49.2648
    val currentStartName = "Minha Localização (GPS)"

    // Filtering system for the Map
    val mapFilters = listOf(
        "TODOS" to "Todos",
        "ATRASADO" to "⚠️ Atrasados",
        "LIMPEZA" to "🧹 Limpezas",
        "TRATAMENTO" to "🧪 Tratamentos"
    )
    var activeMapFilter by remember { mutableStateOf("TODOS") }

    val filteredTasks = remember(tasks, activeMapFilter) {
        tasks.filter { task ->
            val isAtrasado = (task.status == "VERMELHO" || task.status == "LARANJA") && !task.isCompleted
            val isLimpeza = task.serviceType.contains("Limpeza", ignoreCase = true) || task.serviceType.contains("Aspir", ignoreCase = true)
            val isTratamento = task.serviceType.contains("Tratamento", ignoreCase = true) || task.serviceType.contains("Choque", ignoreCase = true)

            when (activeMapFilter) {
                "ATRASADO" -> isAtrasado
                "LIMPEZA" -> isLimpeza
                "TRATAMENTO" -> isTratamento
                else -> true
            }
        }
    }

    // Priority counts for the bottom card matching reference image
    val criticoCount = remember(tasks) { tasks.count { it.status == "VERMELHO" && !it.isCompleted } }
    val altoCount = remember(tasks) { tasks.count { it.status == "LARANJA" && !it.isCompleted } }
    val medioCount = remember(tasks) { tasks.count { it.status == "AMARELO" && !it.isCompleted } }
    val baixoCount = remember(tasks) { tasks.count { it.status == "VERDE" || it.isCompleted } }

    val orderedTasks = remember(filteredTasks, isRouteOptimized, currentStartLat, currentStartLng) {
        if (isRouteOptimized && filteredTasks.isNotEmpty()) {
            val unvisited = filteredTasks.toMutableList()
            val route = mutableListOf<com.example.data.PoolTaskEntity>()
            var currentLat = currentStartLat
            var currentLng = currentStartLng

            while (unvisited.isNotEmpty()) {
                val next = unvisited.minByOrNull { task ->
                    val dLat = task.latitude - currentLat
                    val dLon = task.longitude - currentLng
                    dLat * dLat + dLon * dLon
                }
                if (next != null) {
                    route.add(next)
                    unvisited.remove(next)
                    currentLat = next.latitude
                    currentLng = next.longitude
                } else {
                    break
                }
            }
            route
        } else {
            filteredTasks
        }
    }

    var streetRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeDistanceKm by remember { mutableStateOf(0.0) }
    var routeDurationMin by remember { mutableStateOf(0) }

    LaunchedEffect(orderedTasks, isRouteOptimized, currentStartLat, currentStartLng) {
        if (isRouteOptimized && orderedTasks.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val coordsBuilder = StringBuilder()
                    coordsBuilder.append("$currentStartLng,$currentStartLat")
                    orderedTasks.forEach { task ->
                        coordsBuilder.append(";${task.longitude},${task.latitude}")
                    }
                    val url = "https://router.project-osrm.org/route/v1/driving/$coordsBuilder?overview=full&geometries=geojson"
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyString = response.body?.string()
                            if (bodyString != null) {
                                val json = org.json.JSONObject(bodyString)
                                val routes = json.getJSONArray("routes")
                                if (routes.length() > 0) {
                                    val routeObj = routes.getJSONObject(0)
                                    val distMeters = routeObj.getDouble("distance")
                                    val durSeconds = routeObj.getDouble("duration")
                                    
                                    val geometry = routeObj.getJSONObject("geometry")
                                    val coordinates = geometry.getJSONArray("coordinates")
                                    val points = mutableListOf<GeoPoint>()
                                    for (i in 0 until coordinates.length()) {
                                        val pt = coordinates.getJSONArray(i)
                                        points.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        streetRoutePoints = points
                                        routeDistanceKm = distMeters / 1000.0
                                        routeDurationMin = (durSeconds / 60.0).toInt().coerceAtLeast(1)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val fallback = mutableListOf<GeoPoint>()
                    fallback.add(GeoPoint(currentStartLat, currentStartLng))
                    orderedTasks.forEach { fallback.add(GeoPoint(it.latitude, it.longitude)) }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        streetRoutePoints = fallback
                        routeDistanceKm = orderedTasks.size * 3.5
                        routeDurationMin = orderedTasks.size * 12
                    }
                }
            }
        } else {
            streetRoutePoints = emptyList()
            routeDistanceKm = 0.0
            routeDurationMin = 0
        }
    }

    DisposableEffect(context) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        onDispose {}
    }

    // Dark Premium Palette Colors matching the reference image
    val darkBg = Color(0xFF0B0F17)
    val cardBg = Color(0xFF131B2E)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    Scaffold(
        containerColor = darkBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Professional Bottom Navigation Bar matching reference image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = cardBg,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { /* Map tab active */ }
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Mapa", tint = vibrantBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Mapa", fontSize = 10.sp, color = vibrantBlue, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateBack() }
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Atendimentos", tint = textGray, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Atendimentos", fontSize = 10.sp, color = textGray)
                    }
                    // Center FAB action
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(vibrantBlue)
                            .clickable { /* Add */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { /* Historico */ }
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Histórico", tint = textGray, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Histórico", fontSize = 10.sp, color = textGray)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { /* Perfil */ }
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = textGray, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Perfil", fontSize = 10.sp, color = textGray)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(darkBg)
        ) {
            // Main Map View (OSMDroid) Full Edge-to-Edge
            AndroidView(
                factory = { ctx ->
                    org.osmdroid.config.Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(10.8)
                        controller.setCenter(GeoPoint(-16.50, -49.46))

                        // Apply Dark Mode Filter to OpenStreetMap tiles
                        val darkMatrix = android.graphics.ColorMatrix(floatArrayOf(
                            -0.85f, 0.00f, 0.00f, 0.00f, 255f,
                            0.00f, -0.85f, 0.00f, 0.00f, 255f,
                            0.00f, 0.00f, -0.80f, 0.00f, 255f,
                            0.00f, 0.00f, 0.00f, 1.00f, 0f
                        ))
                        overlayManager.tilesOverlay.setColorFilter(
                            android.graphics.ColorMatrixColorFilter(darkMatrix)
                        )
                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    if (currentAppliedType != selectedMapType) {
                        currentAppliedType = selectedMapType
                        when (selectedMapType) {
                            "SATELLITE" -> {
                                val satSource = org.osmdroid.tileprovider.tilesource.XYTileSource(
                                    "EsriWorldImagery",
                                    0, 19, 256, ".jpg",
                                    arrayOf(
                                        "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
                                        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
                                    )
                                )
                                mapView.setTileSource(satSource)
                                mapView.overlayManager.tilesOverlay.setColorFilter(null)
                            }
                            "TOPO" -> {
                                mapView.setTileSource(TileSourceFactory.OpenTopo)
                                mapView.overlayManager.tilesOverlay.setColorFilter(null)
                            }
                            "LIGHT" -> {
                                mapView.setTileSource(TileSourceFactory.MAPNIK)
                                mapView.overlayManager.tilesOverlay.setColorFilter(null)
                            }
                            else -> { // "DARK"
                                mapView.setTileSource(TileSourceFactory.MAPNIK)
                                val darkMatrix = android.graphics.ColorMatrix(floatArrayOf(
                                    -0.85f, 0.00f, 0.00f, 0.00f, 255f,
                                    0.00f, -0.85f, 0.00f, 0.00f, 255f,
                                    0.00f, 0.00f, -0.80f, 0.00f, 255f,
                                    0.00f, 0.00f, 0.00f, 1.00f, 0f
                                ))
                                mapView.overlayManager.tilesOverlay.setColorFilter(
                                    android.graphics.ColorMatrixColorFilter(darkMatrix)
                                )
                            }
                        }
                    }

                    mapView.overlays.clear()

                    // Draw Google Maps style dual-layer Polyline path along real streets
                    if (isRouteOptimized && streetRoutePoints.isNotEmpty()) {
                        val points = ArrayList(streetRoutePoints)

                        val borderPolyline = Polyline(mapView).apply {
                            setPoints(points)
                            outlinePaint.color = android.graphics.Color.WHITE
                            outlinePaint.strokeWidth = 14f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        }
                        mapView.overlays.add(borderPolyline)

                        val mainPolyline = Polyline(mapView).apply {
                            setPoints(points)
                            outlinePaint.color = android.graphics.Color.parseColor("#1A73E8")
                            outlinePaint.strokeWidth = 8f
                            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                            outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                        }
                        mapView.overlays.add(mainPolyline)
                    }

                    // User Location Marker - Google Maps style blue dot with soft pulsing halo
                    val startMarker = Marker(mapView).apply {
                        position = GeoPoint(currentStartLat, currentStartLng)
                        title = currentStartName
                        snippet = "Sua Posição GPS"
                        icon = createUserLocationHaloDrawable(context, haloPulse)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mapView.overlays.add(startMarker)

                    // Task Markers
                    orderedTasks.forEachIndexed { index, task ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(task.latitude, task.longitude)
                            title = if (isRouteOptimized) "#${index + 1} - ${task.clientName}" else task.clientName
                            snippet = "${task.address} | ${task.serviceType}"
                            icon = createColoredPinDrawable(context, task.status, task.isCompleted)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                selectedTaskForDetails = task
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )

            // Floating Back Button (Top Left)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .appFloatingTop(topOffset = 12.dp, horizontalOffset = 16.dp)
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .clickable { onNavigateBack() },
                color = cardBg.copy(alpha = 0.92f),
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = textWhite, modifier = Modifier.size(20.dp))
                }
            }

            // Floating Vertical Actions Column (Top Right) - Aligned like Google Maps
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .appFloatingTop(topOffset = 12.dp, horizontalOffset = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Minha Localização (GPS Re-center)
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            mapViewRef?.controller?.animateTo(GeoPoint(currentStartLat, currentStartLng))
                            mapViewRef?.controller?.setZoom(15.0)
                        },
                    color = cardBg.copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização", tint = vibrantBlue, modifier = Modifier.size(20.dp))
                    }
                }

                // 2. Camadas / Tipo de Mapa (Satélite / Relevo / Escuro / Claro)
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showMapTypeDialog = true },
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

                // 3. Otimizar Rota (Directions / Route)
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showOptimizeDialog = true },
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

                // 4. Filtro de Clientes / Status (Atrasados, Limpezas, Tratamentos)
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showClientFilterDialog = true },
                    color = if (activeMapFilter != "TODOS") vibrantBlue else cardBg.copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "Filtro de Clientes", 
                            tint = if (activeMapFilter != "TODOS") Color.White else textWhite, 
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
                                .clickable { mapViewRef?.controller?.zoomIn() },
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
                                .clickable { mapViewRef?.controller?.zoomOut() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom -", tint = textWhite, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Client Filter Dialog (Semi-transparente)
            if (showClientFilterDialog) {
                AlertDialog(
                    onDismissRequest = { showClientFilterDialog = false },
                    containerColor = cardBg.copy(alpha = 0.95f),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Filtrar Clientes", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MapTypeOptionItem(
                                title = "📍 Todos os Clientes",
                                subtitle = "Exibe todas as paradas no mapa",
                                selected = activeMapFilter == "TODOS",
                                onClick = { activeMapFilter = "TODOS"; viewModel.setFilter("TODOS"); showClientFilterDialog = false }
                            )
                            MapTypeOptionItem(
                                title = "⚠️ Clientes Atrasados",
                                subtitle = "Exibe apenas chamados urgentes e atrasados",
                                selected = activeMapFilter == "ATRASADO",
                                onClick = { activeMapFilter = "ATRASADO"; viewModel.setFilter("ATRASADO"); showClientFilterDialog = false }
                            )
                            MapTypeOptionItem(
                                title = "🧹 Serviços de Limpeza",
                                subtitle = "Exibe apenas limpezas e aspirações",
                                selected = activeMapFilter == "LIMPEZA",
                                onClick = { activeMapFilter = "LIMPEZA"; viewModel.setFilter("LIMPEZA"); showClientFilterDialog = false }
                            )
                            MapTypeOptionItem(
                                title = "🧪 Tratamentos de Água",
                                subtitle = "Exibe apenas dosagens e choque químico",
                                selected = activeMapFilter == "TRATAMENTO",
                                onClick = { activeMapFilter = "TRATAMENTO"; viewModel.setFilter("TRATAMENTO"); showClientFilterDialog = false }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showClientFilterDialog = false }) {
                            Text("Fechar", color = vibrantBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Map Type Dialog (Modo Escuro e Modo Padrão)
            if (showMapTypeDialog) {
                AlertDialog(
                    onDismissRequest = { showMapTypeDialog = false },
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
                                onClick = { selectedMapType = "DARK"; showMapTypeDialog = false }
                            )
                            MapTypeOptionItem(
                                title = "☀️ Modo Padrão (Claro)",
                                subtitle = "Vias e cidades vetorizadas",
                                selected = selectedMapType == "LIGHT",
                                onClick = { selectedMapType = "LIGHT"; showMapTypeDialog = false }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMapTypeDialog = false }) {
                            Text("Fechar", color = vibrantBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Optimize Route Category Dialog
            if (showOptimizeDialog) {
                AlertDialog(
                    onDismissRequest = { showOptimizeDialog = false },
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
                                selected = isRouteOptimized && activeMapFilter == "TRATAMENTO",
                                onClick = {
                                    activeMapFilter = "TRATAMENTO"
                                    viewModel.setFilter("TRATAMENTO")
                                    isRouteOptimized = true
                                    showOptimizeDialog = false
                                }
                            )

                            MapTypeOptionItem(
                                title = "🧹 Rota de Limpeza & Aspiração",
                                subtitle = "Otimiza apenas manutenção de rotina",
                                selected = isRouteOptimized && activeMapFilter == "LIMPEZA",
                                onClick = {
                                    activeMapFilter = "LIMPEZA"
                                    viewModel.setFilter("LIMPEZA")
                                    isRouteOptimized = true
                                    showOptimizeDialog = false
                                }
                            )

                            MapTypeOptionItem(
                                title = "⚠️ Rota de Chamados Atrasados",
                                subtitle = "Prioriza paradas críticas e em atraso",
                                selected = isRouteOptimized && activeMapFilter == "ATRASADO",
                                onClick = {
                                    activeMapFilter = "ATRASADO"
                                    viewModel.setFilter("ATRASADO")
                                    isRouteOptimized = true
                                    showOptimizeDialog = false
                                }
                            )

                            MapTypeOptionItem(
                                title = "🚀 Rota Completa (Todas as Paradas)",
                                subtitle = "Otimiza a rota sequencial de todos os clientes",
                                selected = isRouteOptimized && activeMapFilter == "TODOS",
                                onClick = {
                                    activeMapFilter = "TODOS"
                                    viewModel.setFilter("TODOS")
                                    isRouteOptimized = true
                                    showOptimizeDialog = false
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Row {
                            if (isRouteOptimized) {
                                TextButton(onClick = {
                                    isRouteOptimized = false
                                    showOptimizeDialog = false
                                }) {
                                    Text("Desativar Rota", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showOptimizeDialog = false }) {
                                Text("Cancelar", color = vibrantBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }

            // Compact Bottom Card "Rotas Inteligentes" (Exibida APENAS quando a rota é otimizada/clicada)
            if (isRouteOptimized) {
                val optimizedRouteTitle = when (activeMapFilter) {
                    "TRATAMENTO" -> "🧪 Rota: Tratamentos Químicos"
                    "LIMPEZA" -> "🧹 Rota: Limpeza & Aspiração"
                    "ATRASADO" -> "⚠️ Rota: Atendimentos Atrasados"
                    else -> "🛣️ Rota Otimizada Completa"
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .padding(12.dp)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = optimizedRouteTitle,
                                        fontWeight = FontWeight.Bold,
                                        color = textWhite,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                                    ) {
                                        Text(
                                            text = "Ativa",
                                            fontSize = 10.sp,
                                            color = Color(0xFF4CAF50),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (routeDistanceKm > 0) 
                                        "${String.format("%.1f", routeDistanceKm)} km • Est. ~$routeDurationMin min (${orderedTasks.size} paradas)" 
                                        else "Calculando trajeto Inhumas ➔ Goianira ➔ Trindade...",
                                    fontSize = 11.sp,
                                    color = textGray
                                )
                            }
                            
                            IconButton(
                                onClick = { isRouteOptimized = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar Rota", tint = textGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF21262D)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Priority Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrioritySummaryItem(color = Color(0xFFD32F2F), label = "Crítico", count = criticoCount)
                            PrioritySummaryItem(color = Color(0xFFE65100), label = "Alto", count = altoCount)
                            PrioritySummaryItem(color = Color(0xFFFBC02D), label = "Médio", count = medioCount)
                            PrioritySummaryItem(color = Color(0xFF2E7D32), label = "Baixo", count = baixoCount)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Maps External Route Button
                        Button(
                            onClick = {
                                if (orderedTasks.isNotEmpty()) {
                                    val destination = orderedTasks.last()
                                    val waypoints = if (orderedTasks.size > 1) {
                                        orderedTasks.dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
                                    } else null
                                    
                                    val uriString = if (waypoints != null) {
                                        "https://www.google.com/maps/dir/?api=1&origin=$currentStartLat,$currentStartLng&destination=${destination.latitude},${destination.longitude}&waypoints=$waypoints&travelmode=driving"
                                    } else {
                                        "https://www.google.com/maps/dir/?api=1&origin=$currentStartLat,$currentStartLng&destination=${destination.latitude},${destination.longitude}&travelmode=driving"
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        context.startActivity(browserIntent)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir NAVEGAÇÃO Rota no Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selected Client Location & Photo Details Sheet (Tapped on Map Marker)
            if (selectedTaskForDetails != null) {
                val st = selectedTaskForDetails!!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .padding(12.dp)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.98f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header with Client Name and Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.clientName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textWhite,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = st.serviceType,
                                    fontSize = 12.sp,
                                    color = vibrantBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { selectedTaskForDetails = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = textGray)
                            }
                        }

                        // Photo of Location / Pool Facade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0D1117)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (st.photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = st.photoUri,
                                    contentDescription = "Foto do Local / Fachada",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = textGray, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Nenhuma foto da fachada anexada", fontSize = 11.sp, color = textGray)
                                }
                            }

                            // Overlay button to pick or change photo
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clickable { galleryLauncher.launch("image/*") }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alterar Foto", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Address Row with Quick Adjust Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0D1117))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = st.address, fontSize = 12.sp, color = textWhite, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Lat: %.4f, Lng: %.4f".format(st.latitude, st.longitude), fontSize = 10.sp, color = textGray)
                                }
                            }

                            OutlinedButton(
                                onClick = { showAdjustAddressDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, vibrantBlue)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajustar", fontSize = 11.sp, color = vibrantBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action Buttons: Open Google Maps Navigation
                        Button(
                            onClick = {
                                val uri = Uri.parse("google.navigation:q=${st.latitude},${st.longitude}")
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${st.latitude},${st.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navegar para este Cliente (Google Maps)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Address & GPS Adjustment Dialog
            if (showAdjustAddressDialog && selectedTaskForDetails != null) {
                var newAddressText by remember { mutableStateOf(selectedTaskForDetails!!.address) }
                var newLatText by remember { mutableStateOf(selectedTaskForDetails!!.latitude.toString()) }
                var newLngText by remember { mutableStateOf(selectedTaskForDetails!!.longitude.toString()) }

                AlertDialog(
                    onDismissRequest = { showAdjustAddressDialog = false },
                    containerColor = cardBg,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PinDrop, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ajustar Endereço / GPS", color = textWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ajuste o endereço ou coordenadas caso a localização esteja incorreta:", fontSize = 12.sp, color = textGray)
                            OutlinedTextField(
                                value = newAddressText,
                                onValueChange = { newAddressText = it },
                                label = { Text("Endereço Completo", color = textGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                    focusedBorderColor = vibrantBlue, unfocusedBorderColor = Color(0xFF30363D)
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = newLatText,
                                    onValueChange = { newLatText = it },
                                    label = { Text("Latitude", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = Color(0xFF30363D)
                                    )
                                )
                                OutlinedTextField(
                                    value = newLngText,
                                    onValueChange = { newLngText = it },
                                    label = { Text("Longitude", color = textGray) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textWhite, unfocusedTextColor = textWhite,
                                        focusedBorderColor = vibrantBlue, unfocusedBorderColor = Color(0xFF30363D)
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val latDouble = newLatText.toDoubleOrNull() ?: selectedTaskForDetails!!.latitude
                                val lngDouble = newLngText.toDoubleOrNull() ?: selectedTaskForDetails!!.longitude
                                val updated = selectedTaskForDetails!!.copy(
                                    address = newAddressText.trim(),
                                    latitude = latDouble,
                                    longitude = lngDouble
                                )
                                viewModel.updateTask(updated)
                                selectedTaskForDetails = updated
                                showAdjustAddressDialog = false
                                android.widget.Toast.makeText(context, "Endereço e GPS atualizados!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = vibrantBlue)
                        ) {
                            Text("Salvar Ajuste", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAdjustAddressDialog = false }) {
                            Text("Cancelar", color = textGray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MapTypeOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardBg = Color(0xFF161B22)
    val vibrantBlue = Color(0xFF1A73E8)
    val textWhite = Color(0xFFF0F4F8)
    val textGray = Color(0xFF94A3B8)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (selected) vibrantBlue.copy(alpha = 0.2f) else Color(0xFF21262D),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, vibrantBlue) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textWhite)
                Text(text = subtitle, fontSize = 11.sp, color = textGray)
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = vibrantBlue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PrioritySummaryItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            Text(text = "$count parada${if (count != 1) "s" else ""}", fontSize = 11.sp, color = Color(0xFFF0F4F8), fontWeight = FontWeight.Bold)
        }
    }
}

fun createColoredPinDrawable(context: Context, status: String, isCompleted: Boolean): android.graphics.drawable.Drawable {
    val color = when {
        isCompleted || status == "CONCLUIDA" || status == "VERDE" -> 0xFF2E7D32.toInt() // Green (Concluída)
        status == "TRATAMENTO_PENDENTE" || status == "VERMELHO" -> 0xFFD32F2F.toInt() // Red (Tratamento Pendente)
        status == "AGUARDANDO_LIMPEZA" || status == "AMARELO" -> 0xFFFBC02D.toInt() // Yellow (Aguardando Limpeza)
        status == "ATRASADA" || status == "LARANJA" -> 0xFFE65100.toInt() // Orange (Atrasada)
        status == "AGENDADA" -> 0xFF1E88E5.toInt() // Blue (Agendada)
        else -> 0xFF1976D2.toInt()
    }

    val density = context.resources.displayMetrics.density
    val px = (28 * density).toInt()
    val py = (38 * density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(px, py, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    val path = android.graphics.Path()
    val radius = px / 2f
    
    path.moveTo(radius, py.toFloat())
    path.cubicTo(
        radius - radius * 0.8f, py - radius * 1.2f,
        0f, radius * 1.5f,
        0f, radius
    )
    path.arcTo(
        0f, 0f, px.toFloat(), px.toFloat(),
        180f, 180f, false
    )
    path.cubicTo(
        px.toFloat(), radius * 1.5f,
        radius + radius * 0.8f, py - radius * 1.2f,
        radius, py.toFloat()
    )
    path.close()

    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawPath(path, paint)

    val scaleMatrix = android.graphics.Matrix()
    scaleMatrix.setScale(0.82f, 0.82f, radius, radius * 0.82f)
    val innerPath = android.graphics.Path()
    path.transform(scaleMatrix, innerPath)
    
    paint.color = color
    canvas.drawPath(innerPath, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius * 0.82f, radius * 0.22f, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun createUserLocationHaloDrawable(context: Context, pulseFraction: Float): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (64 * density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    // 1. Expanding Soft Pulsing Halo Ring (Google Maps Style)
    val minHaloRadius = 8f * density
    val maxHaloRadius = 26f * density
    val haloRadius = minHaloRadius + (maxHaloRadius - minHaloRadius) * pulseFraction
    val haloAlpha = ((1f - pulseFraction) * 0.40f * 255).toInt().coerceIn(0, 255)

    paint.color = android.graphics.Color.parseColor("#1A73E8")
    paint.alpha = haloAlpha
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(centerX, centerY, haloRadius, paint)

    // 2. Soft Accent Ring
    paint.color = android.graphics.Color.parseColor("#4285F4")
    paint.alpha = (0.25f * 255).toInt()
    canvas.drawCircle(centerX, centerY, 11f * density, paint)

    // 3. Crisp White Border Ring
    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    canvas.drawCircle(centerX, centerY, 8.5f * density, paint)

    // 4. Inner Google Blue Core
    paint.color = android.graphics.Color.parseColor("#1A73E8")
    canvas.drawCircle(centerX, centerY, 6.5f * density, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}
