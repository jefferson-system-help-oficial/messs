package com.example.ui.screens.home.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.PoolTaskEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun HomeMapView(
    tasks: List<PoolTaskEntity>,
    isRouteOptimized: Boolean,
    streetRoutePoints: List<GeoPoint>,
    selectedMapType: String,
    startLat: Double,
    startLng: Double,
    startName: String,
    onMapViewReady: (MapView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentAppliedType by remember { mutableStateOf("") }

    // Google Maps Style Pulsing Halo Animation for User Location
    val infiniteTransition = rememberInfiniteTransition(label = "mapHaloTransition")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mapHaloPulse"
    )

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(10.8)
                controller.setCenter(GeoPoint(-16.50, -49.46))

                // Apply Dark Mode Filter to OpenStreetMap tiles
                val darkMatrix = ColorMatrix(floatArrayOf(
                    -0.85f, 0.00f, 0.00f, 0.00f, 255f,
                    0.00f, -0.85f, 0.00f, 0.00f, 255f,
                    0.00f, 0.00f, -0.80f, 0.00f, 255f,
                    0.00f, 0.00f, 0.00f, 1.00f, 0f
                ))
                overlayManager.tilesOverlay.setColorFilter(
                    ColorMatrixColorFilter(darkMatrix)
                )
                onMapViewReady(this)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { mapView ->
            if (currentAppliedType != selectedMapType) {
                currentAppliedType = selectedMapType
                when (selectedMapType) {
                    "SATELLITE" -> {
                        val satSource = XYTileSource(
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
                        val darkMatrix = ColorMatrix(floatArrayOf(
                            -0.85f, 0.00f, 0.00f, 0.00f, 255f,
                            0.00f, -0.85f, 0.00f, 0.00f, 255f,
                            0.00f, 0.00f, -0.80f, 0.00f, 255f,
                            0.00f, 0.00f, 0.00f, 1.00f, 0f
                        ))
                        mapView.overlayManager.tilesOverlay.setColorFilter(
                            ColorMatrixColorFilter(darkMatrix)
                        )
                    }
                }
            }

            mapView.overlays.clear()

            if (isRouteOptimized && streetRoutePoints.isNotEmpty()) {
                val points = ArrayList(streetRoutePoints)

                val borderPolyline = Polyline(mapView).apply {
                    setPoints(points)
                    outlinePaint.color = android.graphics.Color.WHITE
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                mapView.overlays.add(borderPolyline)

                val mainPolyline = Polyline(mapView).apply {
                    setPoints(points)
                    outlinePaint.color = android.graphics.Color.parseColor("#1A73E8")
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                mapView.overlays.add(mainPolyline)
            }

            // User Location Marker - Google Maps style blue dot with soft pulsing halo
            val startMarker = Marker(mapView).apply {
                position = GeoPoint(startLat, startLng)
                title = startName
                snippet = "Sua Posição GPS"
                icon = createUserLocationHaloDrawable(context, haloPulse)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(startMarker)

            tasks.forEachIndexed { index, task ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(task.latitude, task.longitude)
                    title = if (isRouteOptimized) "#${index + 1} - ${task.clientName}" else task.clientName
                    snippet = "${task.address} | ${task.serviceType}"
                    icon = createColoredPinDrawable(context, task.displayStatus, task.isCompleted)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}

private fun createColoredPinDrawable(context: Context, status: String, isCompleted: Boolean): Drawable {
    val color = when {
        isCompleted || status == "CONCLUIDA" || status == "VERDE" -> 0xFF2E7D32.toInt()
        status == "TRATAMENTO_PENDENTE" || status == "VERMELHO" -> 0xFFD32F2F.toInt()
        status == "AGUARDANDO_LIMPEZA" || status == "AMARELO" -> 0xFFFBC02D.toInt()
        status == "ATRASADA" || status == "LARANJA" -> 0xFFE65100.toInt()
        status == "AGENDADA" -> 0xFF1E88E5.toInt()
        else -> 0xFF1976D2.toInt()
    }

    val density = context.resources.displayMetrics.density
    val px = (28 * density).toInt()
    val py = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(px, py, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val path = Path()
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
    paint.style = Paint.Style.FILL
    canvas.drawPath(path, paint)

    val scaleMatrix = Matrix()
    scaleMatrix.setScale(0.82f, 0.82f, radius, radius * 0.82f)
    val innerPath = Path()
    path.transform(scaleMatrix, innerPath)

    paint.color = color
    canvas.drawPath(innerPath, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(radius, radius * 0.82f, radius * 0.22f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserLocationHaloDrawable(context: Context, pulseFraction: Float): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (64 * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    val minHaloRadius = 8f * density
    val maxHaloRadius = 26f * density
    val haloRadius = minHaloRadius + (maxHaloRadius - minHaloRadius) * pulseFraction
    val haloAlpha = ((1f - pulseFraction) * 0.40f * 255).toInt().coerceIn(0, 255)

    paint.color = android.graphics.Color.parseColor("#1A73E8")
    paint.alpha = haloAlpha
    paint.style = Paint.Style.FILL
    canvas.drawCircle(centerX, centerY, haloRadius, paint)

    paint.color = android.graphics.Color.parseColor("#4285F4")
    paint.alpha = (0.25f * 255).toInt()
    canvas.drawCircle(centerX, centerY, 11f * density, paint)

    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    canvas.drawCircle(centerX, centerY, 8.5f * density, paint)

    paint.color = android.graphics.Color.parseColor("#1A73E8")
    canvas.drawCircle(centerX, centerY, 6.5f * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}
