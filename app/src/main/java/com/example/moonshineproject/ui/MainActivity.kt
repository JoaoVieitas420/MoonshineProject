package com.example.moonshineproject.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moonshineproject.ui.screens.HomeScreen
import com.example.moonshineproject.ui.screens.SleepLogScreen
import com.example.moonshineproject.ui.theme.MoonshineProjectTheme

class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            android.util.Log.d("Moonshine", "Permissão ${it.key}: ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            MoonshineProjectTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    containerColor = Color(0xFF0D0D1A),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF1A1A2E),
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = currentRoute == "home",
                                onClick = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Sentinel") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF9B59B6),
                                    selectedTextColor = Color(0xFF9B59B6),
                                    unselectedIconColor = Color(0xFF555555),
                                    unselectedTextColor = Color(0xFF555555),
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
                                icon = { Icon(Icons.Filled.DateRange, contentDescription = "Diário") },
                                label = { Text("Diário") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF9B59B6),
                                    selectedTextColor = Color(0xFF9B59B6),
                                    unselectedIconColor = Color(0xFF555555),
                                    unselectedTextColor = Color(0xFF555555),
                                    indicatorColor = Color(0xFF2A1A3E)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") { HomeScreen() }
                        composable("sleep_log") { SleepLogScreen() }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        if (permissionsToRequest.isNotEmpty())
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }
}