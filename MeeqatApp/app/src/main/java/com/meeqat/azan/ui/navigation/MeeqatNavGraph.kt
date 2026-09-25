package com.meeqat.azan.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meeqat.azan.ui.home.HomeScreen
import com.meeqat.azan.ui.settings.SettingsScreen

// Simple — exactly like Nizamul Awqat Nanded image: just prayer + alarm, no bottom nav clutter.
// Settings is reachable via header calendar icon -> settings.
@Composable
fun MeeqatNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onLocationClick = { navController.navigate("settings/location") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("settings/location") { SettingsScreen() }
            composable("settings/method") { SettingsScreen() }
            composable("settings/adjust") { SettingsScreen() }
            composable("settings/sound") { SettingsScreen() }
        }
    }
}
