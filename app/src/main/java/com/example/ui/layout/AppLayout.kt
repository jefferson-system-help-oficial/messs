package com.example.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Global architectural layout utility for Android System Insets and Responsive Dimensions.
 * Ensures consistent behavior across all devices:
 * - Status bar (top)
 * - Navigation bar / Gesture bar (bottom - 3 buttons or gesture pill)
 * - Display Cutout / Notch / Punch Hole cameras
 * - IME / Software Keyboard dynamic adjustment
 * - Foldables & Large screens (Z Flip, Z Fold, Tablets, S20 FE, S8)
 * - Portrait & Landscape orientations
 */

/**
 * Standard content padding for scrollable lists (LazyColumn, Column with verticalScroll).
 * Guarantees that the bottom-most list items are completely visible and never trapped
 * under bottom navigation bars, system bars, or floating action buttons.
 */
@Composable
fun appScrollableContentPadding(
    additionalTop: Dp = 16.dp,
    additionalBottom: Dp = 96.dp,
    horizontal: Dp = 16.dp
): PaddingValues {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val effectiveBottom = maxOf(additionalBottom + navBarBottom, imeBottom + 16.dp)

    return PaddingValues(
        start = horizontal + WindowInsets.displayCutout.asPaddingValues().calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        end = horizontal + WindowInsets.displayCutout.asPaddingValues().calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
        top = additionalTop,
        bottom = effectiveBottom
    )
}

/**
 * Universal Screen Scaffold that enforces system safe areas globally for any screen.
 */
@Composable
fun AppScreenScaffold(
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF0D1117),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    // Whether this screen is an edge-to-edge full bleed map (requiring manual overlay insets)
    isFullBleedMap: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
            .imePadding(),
        containerColor = containerColor,
        // For full-bleed maps, allow content to span under system bars, while topBar/bottomBar handle insets
        contentWindowInsets = if (isFullBleedMap) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullBleedMap) PaddingValues(0.dp) else innerPadding)
        ) {
            // Center content with adaptive maximum width for foldables (Z Fold) and tablets
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .align(Alignment.Center)
            ) {
                content(innerPadding)
            }
        }
    }
}

/**
 * Universal Adaptive Dialog that respects safe insets, display cutouts, and keyboard appearance.
 * Prevents dialog content from getting clipped on compact foldables (e.g. Galaxy Z Flip cover/flex mode)
 * and guarantees scrollability when the keyboard is open.
 */
@Composable
fun AppAdaptiveDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    containerColor: Color = Color(0xFF161B22),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 560.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Modifier extension to safely place floating top-left/top-right controls
 * respecting the system status bar and display notch/cutout.
 */
fun Modifier.appFloatingTop(
    topOffset: Dp = 12.dp,
    horizontalOffset: Dp = 16.dp
): Modifier = this
    .statusBarsPadding()
    .displayCutoutPadding()
    .padding(top = topOffset, start = horizontalOffset, end = horizontalOffset)

/**
 * Modifier extension to safely place floating side action controls
 * respecting system navigation bars and display cutout in both portrait and landscape.
 */
fun Modifier.appFloatingSide(
    verticalOffset: Dp = 0.dp,
    horizontalOffset: Dp = 16.dp
): Modifier = this
    .navigationBarsPadding()
    .displayCutoutPadding()
    .padding(horizontal = horizontalOffset, vertical = verticalOffset)
