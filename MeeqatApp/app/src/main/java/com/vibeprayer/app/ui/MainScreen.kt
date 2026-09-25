package com.vibeprayer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibeprayer.app.VibeApp
import com.vibeprayer.app.domain.PrayerBackgroundEngine
import com.vibeprayer.app.ui.components.DynamicBackground
import com.vibeprayer.app.ui.components.FloatingNavBar
import com.vibeprayer.app.ui.home.HomeScreen
import com.vibeprayer.app.ui.home.HomeViewModel
import com.vibeprayer.app.ui.settings.SettingsScreen
import com.vibeprayer.app.ui.theme.VibeTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibeprayer.app.R

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as VibeApp
    val settings by app.prefs.settings.collectAsState(initial = com.vibeprayer.app.data.model.UserSettings())
    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val homeUi by homeVm.ui.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            now = System.currentTimeMillis()
        }
    }
    val bgRes = remember(now, homeUi.times) {
        try {
            PrayerBackgroundEngine.backgroundFor(now, homeUi.times)
        } catch (_: Exception) {
            R.drawable.default_bg
        }
    }

    VibeTheme(theme = settings.theme, redAccents = settings.redAccents) {
        val nav = rememberNavController()
        var selected by remember { mutableStateOf(0) }
        var navVisible by remember { mutableStateOf(true) }
        val connection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    navVisible = when {
                        available.y < -6 -> false
                        available.y > 6 -> true
                        else -> navVisible
                    }
                    return Offset.Zero
                }
            }
        }
        androidx.compose.runtime.LaunchedEffect(nav) {
            nav.currentBackStackEntryFlow.collect { entry ->
                selected = if (entry.destination.route == "settings") 1 else 0
            }
        }
        Box(Modifier.fillMaxSize().nestedScroll(connection)) {
            DynamicBackground(bgRes = bgRes)
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    HomeScreen(
                        onSettingsClick = { nav.navigate("settings") },
                        viewModel = homeVm
                    )
                }
                composable("settings") { SettingsScreen() }
            }
            FloatingNavBar(
                selected = selected,
                visible = navVisible,
                onSelect = { idx ->
                    selected = idx
                    navVisible = true
                    if (idx == 0) nav.navigate("home") { launchSingleTop = true }
                    else nav.navigate("settings") { launchSingleTop = true }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
