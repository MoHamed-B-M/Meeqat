package com.meeqat.azan.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meeqat.azan.ui.home.HomeScreen
import com.meeqat.azan.ui.qibla.QiblaScreen
import com.meeqat.azan.ui.settings.SettingsScreen
import com.meeqat.azan.ui.tracker.TrackerScreen
import com.meeqat.azan.ui.theme.MeeqatRef

sealed class MeeqatDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Prayer : MeeqatDestination("home", "Prayer", Icons.Filled.Home, Icons.Outlined.Home)
    data object Qibla : MeeqatDestination("qibla", "Qibla", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object Tracker : MeeqatDestination("tracker", "Tracker", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    data object Settings : MeeqatDestination("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    // legacy calendar kept for internal nav, but not in bottom bar
    data object Calendar : MeeqatDestination("calendar", "Calendar", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    data object LocationSettings : MeeqatDestination("settings/location", "Location", Icons.Filled.Home, Icons.Outlined.Home)
    data object MethodSettings : MeeqatDestination("settings/method", "Method", Icons.Filled.Home, Icons.Outlined.Home)
    data object AdjustSettings : MeeqatDestination("settings/adjust", "Adjust", Icons.Filled.Home, Icons.Outlined.Home)
    data object SoundSettings : MeeqatDestination("settings/sound", "Sound", Icons.Filled.Home, Icons.Outlined.Home)
}

private val topLevelDestinations = listOf(
    MeeqatDestination.Prayer,
    MeeqatDestination.Qibla,
    MeeqatDestination.Tracker,
    MeeqatDestination.Settings
)

@Composable
fun MeeqatNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val navSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            topLevelDestinations.forEach { dest ->
                val selected = currentRoute == dest.route || (dest.route == "home" && currentRoute == "home")
                item(
                    selected = selected,
                    onClick = {
                        navController.navigate(dest.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (selected) MeeqatRef.Peach else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            dest.label,
                            color = if (selected) MeeqatRef.Peach else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                )
            }
        },
        layoutType = when (navSuiteType) {
            NavigationSuiteType.NavigationBar -> NavigationSuiteType.NavigationBar
            NavigationSuiteType.NavigationRail -> NavigationSuiteType.NavigationRail
            NavigationSuiteType.NavigationDrawer -> NavigationSuiteType.NavigationRail
            else -> NavigationSuiteType.NavigationBar
        },
        navigationSuiteColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MeeqatRef.Navy,
            navigationRailContainerColor = MeeqatRef.Navy,
        ),
        modifier = modifier
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MeeqatRef.Navy,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MeeqatDestination.Prayer.route,
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                composable(MeeqatDestination.Prayer.route) {
                    HomeScreen(onLocationClick = { navController.navigate(MeeqatDestination.LocationSettings.route) })
                }
                composable(MeeqatDestination.Qibla.route) { QiblaScreen() }
                composable(MeeqatDestination.Tracker.route) { TrackerScreen() }
                composable(MeeqatDestination.Calendar.route) { TrackerScreen() }
                composable(MeeqatDestination.Settings.route) {
                    SettingsScreen(
                        onNavigateLocation = { navController.navigate(MeeqatDestination.LocationSettings.route) },
                        onNavigateMethod = { navController.navigate(MeeqatDestination.MethodSettings.route) },
                        onNavigateAdjust = { navController.navigate(MeeqatDestination.AdjustSettings.route) },
                        onNavigateSound = { navController.navigate(MeeqatDestination.SoundSettings.route) }
                    )
                }
                composable(MeeqatDestination.LocationSettings.route) { SettingsScreen() }
                composable(MeeqatDestination.MethodSettings.route) { SettingsScreen() }
                composable(MeeqatDestination.AdjustSettings.route) { SettingsScreen() }
                composable(MeeqatDestination.SoundSettings.route) { SettingsScreen() }
            }
        }
    }
}
