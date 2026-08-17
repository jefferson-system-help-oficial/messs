package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditTaskScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OsmMapScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PoolViewModel

class MainActivity : ComponentActivity() {
    private val poolViewModel: PoolViewModel by viewModels()
    private var incomingSharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        incomingSharedText = parseIntentData(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(incomingSharedText) {
                        val text = incomingSharedText
                        if (!text.isNullOrBlank()) {
                            val encoded = Uri.encode(text)
                            navController.navigate("add_task?sharedText=$encoded")
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = poolViewModel,
                                onNavigateToAdd = { sharedText ->
                                    if (sharedText != null) {
                                        val encoded = Uri.encode(sharedText)
                                        navController.navigate("add_task?sharedText=$encoded")
                                    } else {
                                        navController.navigate("add_task")
                                    }
                                },
                                onNavigateToDetail = { taskId ->
                                    navController.navigate("detail/$taskId")
                                },
                                onNavigateToMap = { navController.navigate("osm_map") }
                            )
                        }
                        composable("osm_map") {
                            OsmMapScreen(
                                viewModel = poolViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "add_task?sharedText={sharedText}",
                            arguments = listOf(
                                navArgument("sharedText") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val sharedTextArg = backStackEntry.arguments?.getString("sharedText")
                            AddEditTaskScreen(
                                viewModel = poolViewModel,
                                initialSharedText = sharedTextArg ?: incomingSharedText,
                                onNavigateBack = {
                                    incomingSharedText = null
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(
                            route = "detail/{taskId}",
                            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                            DetailScreen(
                                taskId = taskId,
                                viewModel = poolViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingSharedText = parseIntentData(intent)
    }

    private fun parseIntentData(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()
            }
            else -> null
        }
    }
}
