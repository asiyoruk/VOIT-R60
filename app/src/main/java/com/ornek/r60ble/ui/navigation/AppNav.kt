package com.ornek.r60ble.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ornek.r60ble.ui.connect.ConnectScreen
import com.ornek.r60ble.ui.history.HistoryScreen
import com.ornek.r60ble.ui.live.LiveScreen
import com.ornek.r60ble.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Connect("connect", "Bağlan", Icons.Default.Search),
    Live("live", "Canlı", Icons.Default.Home),
    History("history", "Geçmiş", Icons.AutoMirrored.Filled.List),
    Settings("settings", "Ayarlar", Icons.Default.Settings)
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    Scaffold(bottomBar = {
        NavigationBar {
            Dest.entries.forEach { d ->
                NavigationBarItem(
                    selected = current?.destination?.route == d.route,
                    onClick = {
                        nav.navigate(d.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    icon = { Icon(d.icon, d.label) }, label = { Text(d.label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Dest.Connect.route, modifier = Modifier.padding(pad)) {
            composable(Dest.Connect.route) { ConnectScreen() }
            composable(Dest.Live.route) { LiveScreen() }
            composable(Dest.History.route) { HistoryScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
