package com.example.moonshineproject.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moonshineproject.data.AppPreferences
import com.example.moonshineproject.ui.screens.HomeScreen
import com.example.moonshineproject.ui.screens.OnboardingScreen
import com.example.moonshineproject.ui.screens.SettingsScreen
import com.example.moonshineproject.ui.screens.SleepLogScreen
import com.example.moonshineproject.ui.screens.SleepSessionDetailScreen
import com.example.moonshineproject.ui.theme.MoonshineProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MoonshineProjectTheme {
                val prefs = remember { AppPreferences.getInstance(this) }
                var onboardingCompleted by remember { mutableStateOf(prefs.onboardingCompleted) }

                MoonshineNavigation(
                    onboardingCompleted = onboardingCompleted,
                    onOnboardingDone = {
                        prefs.onboardingCompleted = true
                        onboardingCompleted = true
                    }
                )
            }
        }
    }
}

@Composable
private fun MoonshineNavigation(
    onboardingCompleted: Boolean,
    onOnboardingDone: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startRoute = if (onboardingCompleted) "home" else "onboarding"
    val showBottomBar = currentRoute == "home" || currentRoute == "sleep_log"

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color(0xFF1A1A2E), contentColor = Color.White) {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Início") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF9B59B6),
                            selectedTextColor = Color(0xFF9B59B6),
                            indicatorColor = Color(0xFF2A1A3E)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute == "sleep_log",
                        onClick = {
                            navController.navigate("sleep_log") {
                                popUpTo("home")
                            }
                        },
                        icon = { Icon(Icons.Filled.DateRange, contentDescription = "Sleep Log") },
                        label = { Text("Diário") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF9B59B6),
                            selectedTextColor = Color(0xFF9B59B6),
                            indicatorColor = Color(0xFF2A1A3E)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        onOnboardingDone()
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    contentPadding = innerPadding,
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    contentPadding = innerPadding,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("sleep_log") {
                SleepLogScreen(
                    contentPadding = innerPadding,
                    onOpenSession = { sessionId ->
                        navController.navigate("sleep_detail/$sessionId")
                    }
                )
            }
            composable(
                route = "sleep_detail/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStack ->
                val sessionId = backStack.arguments?.getLong("sessionId") ?: return@composable
                SleepSessionDetailScreen(
                    contentPadding = innerPadding,
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
