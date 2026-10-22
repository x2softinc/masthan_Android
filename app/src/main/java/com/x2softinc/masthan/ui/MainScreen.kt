@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.x2softinc.masthan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x2softinc.masthan.APP_URL
import com.x2softinc.masthan.AppMetadata
import com.x2softinc.masthan.CITY
import com.x2softinc.masthan.MASJID_INFO
import com.x2softinc.masthan.data.AppJson
import com.x2softinc.masthan.data.LocationAuthorization
import com.x2softinc.masthan.data.LocationTracker
import com.x2softinc.masthan.data.MasjidInfo
import kotlinx.coroutines.launch

private enum class Tab(val title: String, val barTitle: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", "Masthan Gani Masjid", Icons.Filled.Schedule),
    QIBLA("Qibla", "Qibla", Icons.Filled.Explore),
    MASJID("Masjid", "Masjid Info", Icons.Filled.Apartment),
    SETTINGS("Settings", "Settings", Icons.Filled.Settings),
}

/** Screens pushed on top of the tabs (Settings rows and drawer legal items). */
private enum class Route(val title: String) {
    ADHAN_SOUND("Adhan Sound"),
    TERMS("Terms of Use"),
    PRIVACY("Privacy Policy"),
}

/** Tab bar + navigation drawer (port of Views/ContentView.swift). */
@Composable
fun MainScreen() {
    val context = LocalContext.current

    // Location starts at app launch and is shared app-wide.
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        LocationTracker.markPermissionRequested()
        LocationTracker.refreshAuthorization()
    }
    LaunchedEffect(Unit) {
        if (LocationTracker.authorization == LocationAuthorization.NOT_DETERMINED) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by remember { mutableStateOf<Route?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activeRoute = route
    if (activeRoute != null) {
        BackHandler { route = null }
        DetailScaffold(title = activeRoute.title, onBack = { route = null }) {
            when (activeRoute) {
                Route.ADHAN_SOUND -> AdhanSoundScreen()
                Route.TERMS -> EulaScreen()
                Route.PRIVACY -> LegalDocumentScreen(LegalDocument.PRIVACY_POLICY)
            }
        }
        return
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onClose = { scope.launch { drawerState.close() } },
                onRate = { scope.launch { drawerState.close() }; openUrl(context, APP_URL) },
                onShare = { scope.launch { drawerState.close() }; shareApp(context) },
                onHelp = { scope.launch { drawerState.close() }; openUrl(context, "https://x2softinc.com") },
                onTerms = { scope.launch { drawerState.close() }; route = Route.TERMS },
                onPrivacy = { scope.launch { drawerState.close() }; route = Route.PRIVACY },
            )
        },
    ) {
        val current = Tab.entries[tab]
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current.barTitle, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
                        }
                    },
                    actions = { if (current == Tab.DASHBOARD) DisplayCity() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BarColor),
                )
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(item.icon, null) },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        )
                    }
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding)) {
                when (current) {
                    Tab.DASHBOARD -> DashboardScreen()
                    Tab.QIBLA -> QiblaScreen()
                    Tab.MASJID -> MasjidScreen()
                    Tab.SETTINGS -> SettingsScreen(
                        onOpenAdhanSound = { route = Route.ADHAN_SOUND },
                        onOpenTerms = { route = Route.TERMS },
                        onOpenPrivacy = { route = Route.PRIVACY },
                    )
                }
            }
        }
    }
}

/** City shown in the dashboard bar: the device city, else the masjid's. */
@Composable
private fun DisplayCity() {
    val city by rememberStringPref(CITY)
    val masjidJson by rememberStringPref(MASJID_INFO)
    val displayCity = city.ifEmpty {
        runCatching { AppJson.decodeFromString<MasjidInfo>(masjidJson).city }.getOrNull().orEmpty()
    }
    if (displayCity.isNotEmpty()) {
        Text(
            displayCity,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

@Composable
private fun DetailScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BarColor),
            )
        },
    ) { padding -> Column(Modifier.padding(padding)) { content() } }
}

// region Drawer

@Composable
private fun DrawerContent(
    onClose: () -> Unit,
    onRate: () -> Unit,
    onShare: () -> Unit,
    onHelp: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    ModalDrawerSheet(Modifier.width(280.dp)) {
        // Header
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Filled.NightsStay, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(AppMetadata.appName(context), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(AppMetadata.APP_TAGLINE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()

        // Menu items
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            DrawerMenuItem(Icons.Filled.Star, "Rate the App", onRate)
            DrawerMenuItem(Icons.Filled.Share, "Share App", onShare)
            DrawerMenuItem(Icons.Filled.HelpOutline, "Help & Support", onHelp)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DrawerMenuItem(Icons.Filled.Description, "Terms of Use", onTerms)
            DrawerMenuItem(Icons.Filled.PanTool, "Privacy Policy", onPrivacy)
        }

        // Footer
        Text(
            "Version ${AppMetadata.appVersion(context)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun DrawerMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun shareApp(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out this amazing Prayer Times app!\n$APP_URL")
    }
    context.startActivity(Intent.createChooser(send, "Share App").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// endregion
