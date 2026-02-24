package com.promotion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.promotion.app.ui.PromotionViewModel
import com.promotion.app.ui.screens.*
import com.promotion.app.ui.theme.PromotionAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromotionAppTheme {
                PromotionApp()
            }
        }
    }
}

@Composable
fun PromotionApp(viewModel: PromotionViewModel = viewModel()) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val yearlyReport by viewModel.yearlyReport.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val dbCount by viewModel.dbCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color(0xFF0F172A),
        bottomBar = {
            if (currentRoute != "timeline") {
                NavigationBar(containerColor = Color(0xFF1E293B)) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Employees") },
                        selected = currentRoute == "employees",
                        onClick = { navController.navigate("employees") { popUpTo("employees") { inclusive = true } } },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6366F1),
                            selectedTextColor = Color(0xFF6366F1),
                            indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Report") },
                        selected = currentRoute == "report",
                        onClick = { navController.navigate("report") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6366F1),
                            selectedTextColor = Color(0xFF6366F1),
                            indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Sync") },
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6366F1),
                            selectedTextColor = Color(0xFF6366F1),
                            indicatorColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "employees",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("employees") {
                EmployeeListScreen(
                    employees = employees,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.updateSearch(it) },
                    onEmployeeClick = { empId ->
                        viewModel.selectEmployee(empId)
                        navController.navigate("timeline")
                    },
                    dbCount = dbCount
                )
            }

            composable("timeline") {
                TimelineScreen(
                    predictions = predictions,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("report") {
                YearlyReportScreen(
                    yearlyData = yearlyReport,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    apiUrl = viewModel.apiUrl,
                    lastSyncTime = viewModel.lastSyncTime,
                    syncStatus = syncStatus,
                    dbCount = dbCount,
                    onApiUrlChange = { viewModel.updateApiUrl(it) },
                    onSyncNow = { viewModel.syncNow() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
