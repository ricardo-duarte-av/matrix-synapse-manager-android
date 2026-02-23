package com.matrix.synapse.manager

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.matrix.synapse.feature.auth.ui.LoginScreen
import com.matrix.synapse.feature.devices.ui.DeviceListScreen
import com.matrix.synapse.feature.devices.ui.WhoisScreen
import com.matrix.synapse.feature.servers.ui.ServerFormScreen
import com.matrix.synapse.feature.servers.ui.ServerListScreen
import com.matrix.synapse.feature.settings.ui.AppLockSettingsScreen
import com.matrix.synapse.feature.settings.ui.AuditLogScreen
import com.matrix.synapse.feature.users.ui.UserDetailScreen
import com.matrix.synapse.feature.users.ui.UserEditScreen
import com.matrix.synapse.feature.users.ui.UserListScreen
import com.matrix.synapse.feature.rooms.ui.RoomDetailScreen
import com.matrix.synapse.feature.rooms.ui.RoomListScreen
import com.matrix.synapse.feature.stats.ui.ServerDashboardScreen
import com.matrix.synapse.feature.media.ui.MediaListScreen
import com.matrix.synapse.feature.media.ui.MediaDetailScreen
import com.matrix.synapse.feature.federation.ui.FederationListScreen
import com.matrix.synapse.feature.federation.ui.FederationDetailScreen
import com.matrix.synapse.feature.jobs.ui.BackgroundJobsScreen
import com.matrix.synapse.manager.MoreScreen

private enum class MainTab(val routePattern: String, val label: String) {
    Users("UserList", "Users"),
    Rooms("RoomList", "Rooms"),
    Media("MediaList", "Media"),
    Stats("ServerDashboard", "Stats"),
    More("More", "More"),
}

private fun String?.isTabRoute(): Boolean = this != null && MainTab.entries.any { routePattern -> this.contains(routePattern.routePattern) }

private fun String?.selectedTab(): MainTab = when {
    this == null -> MainTab.Users
    this.contains(MainTab.Users.routePattern) -> MainTab.Users
    this.contains(MainTab.Rooms.routePattern) -> MainTab.Rooms
    this.contains(MainTab.Stats.routePattern) -> MainTab.Stats
    this.contains(MainTab.Media.routePattern) -> MainTab.Media
    this.contains(MainTab.More.routePattern) -> MainTab.More
    else -> MainTab.Users
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showTabs = currentRoute.isTabRoute()
    val selectedTab = currentRoute.selectedTab()

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showTabs && backStackEntry != null) {
                val entry = backStackEntry!!
                val (serverId, serverUrl) = when (selectedTab) {
                    MainTab.Users -> run {
                        val r = entry.toRoute<UserList>()
                        r.serverId to r.serverUrl
                    }
                    MainTab.Rooms -> run {
                        val r = entry.toRoute<RoomList>()
                        r.serverId to r.serverUrl
                    }
                    MainTab.Stats -> run {
                        val r = entry.toRoute<ServerDashboard>()
                        r.serverId to r.serverUrl
                    }
                    MainTab.Media -> run {
                        val r = entry.toRoute<MediaList>()
                        r.serverId to r.serverUrl
                    }
                    MainTab.More -> run {
                        val r = entry.toRoute<More>()
                        r.serverId to r.serverUrl
                    }
                }
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                when (tab) {
                                    MainTab.Users -> navController.navigate(UserList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    MainTab.Rooms -> navController.navigate(RoomList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    MainTab.Stats -> navController.navigate(ServerDashboard(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    MainTab.Media -> navController.navigate(MediaList(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                    MainTab.More -> navController.navigate(More(serverId, serverUrl)) {
                                        popUpTo<Login> { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        MainTab.Users -> Icons.Filled.Person
                                        MainTab.Rooms -> Icons.Filled.Home
                                        MainTab.Stats -> Icons.Filled.Info
                                        MainTab.Media -> Icons.Filled.Search
                                        MainTab.More -> Icons.Filled.MoreVert
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = ServerList,
            modifier = modifier.padding(paddingValues),
        ) {
        composable<ServerList> {
            ServerListScreen(
                onAddServer = { navController.navigate(ServerSetup) },
                onEditServer = { serverId ->
                    navController.navigate(ServerEdit(serverId))
                },
                onOpenLogin = { serverId, serverUrl ->
                    navController.navigate(Login(serverId, serverUrl)) {
                        popUpTo<ServerList> { inclusive = true }
                    }
                },
                onOpenUserList = { serverId, serverUrl ->
                    navController.navigate(UserList(serverId, serverUrl)) {
                        popUpTo<ServerList> { inclusive = true }
                    }
                },
            )
        }

        composable<ServerSetup> {
            ServerFormScreen(
                onServerAdded = { serverId, serverUrl ->
                    navController.navigate(Login(serverId, serverUrl)) {
                        popUpTo<ServerSetup> { inclusive = true }
                    }
                },
            )
        }

        composable<ServerEdit> { backStack ->
            val route = backStack.toRoute<ServerEdit>()
            ServerFormScreen(
                serverIdToEdit = route.serverId,
                onServerAdded = { _, _ -> },
                onServerUpdated = { navController.popBackStack() },
            )
        }

        composable<Login> { backStack ->
            val route = backStack.toRoute<Login>()
            LoginScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onLoginSuccess = {
                    navController.navigate(UserList(route.serverId, route.serverUrl)) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
            )
        }

        composable<UserList> { backStack ->
            val route = backStack.toRoute<UserList>()
            UserListScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onUserClick = { userId ->
                    navController.navigate(UserDetail(route.serverId, route.serverUrl, userId))
                },
                onAuditLog = {
                    navController.navigate(AuditLog(route.serverId))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onSettings = {
                    navController.navigate(AppLockSettings)
                },
                onRooms = {
                    navController.navigate(RoomList(route.serverId, route.serverUrl))
                },
                onDashboard = {
                    navController.navigate(ServerDashboard(route.serverId, route.serverUrl))
                },
                onMedia = {
                    navController.navigate(MediaList(route.serverId, route.serverUrl))
                },
                onFederation = {
                    navController.navigate(FederationList(route.serverId, route.serverUrl))
                },
            )
        }

        composable<UserDetail> { backStack ->
            val route = backStack.toRoute<UserDetail>()
            UserDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                userId = route.userId,
                onEdit = { navController.navigate(UserEdit(route.serverUrl, route.userId)) },
                onDevices = { navController.navigate(DeviceList(route.serverUrl, route.userId)) },
                onWhois = { navController.navigate(Whois(route.serverUrl, route.userId)) },
                onMedia = {
                    navController.navigate(
                        MediaList(route.serverId, route.serverUrl, filterUserId = route.userId)
                    )
                },
            )
        }

        composable<UserEdit> { backStack ->
            val route = backStack.toRoute<UserEdit>()
            UserEditScreen(
                serverUrl = route.serverUrl,
                existingUserId = route.userId,
                onSaved = { navController.popBackStack() },
            )
        }

        composable<DeviceList> { backStack ->
            val route = backStack.toRoute<DeviceList>()
            DeviceListScreen(serverUrl = route.serverUrl, userId = route.userId)
        }

        composable<Whois> { backStack ->
            val route = backStack.toRoute<Whois>()
            WhoisScreen(serverUrl = route.serverUrl, userId = route.userId)
        }

        composable<AuditLog> { backStack ->
            AuditLogScreen(serverId = backStack.toRoute<AuditLog>().serverId)
        }

        composable<AppLockSettings> {
            AppLockSettingsScreen()
        }

        composable<RoomList> { backStack ->
            val route = backStack.toRoute<RoomList>()
            RoomListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onRoomClick = { roomId ->
                    navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
            )
        }

        composable<RoomDetail> { backStack ->
            val route = backStack.toRoute<RoomDetail>()
            RoomDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                roomId = route.roomId,
                onBack = { navController.popBackStack() },
                onMedia = {
                    navController.navigate(
                        MediaList(route.serverId, route.serverUrl, filterRoomId = route.roomId)
                    )
                },
            )
        }

        composable<ServerDashboard> { backStack ->
            val route = backStack.toRoute<ServerDashboard>()
            ServerDashboardScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
            )
        }

        composable<MediaList> { backStack ->
            val route = backStack.toRoute<MediaList>()
            MediaListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                filterUserId = route.filterUserId,
                filterRoomId = route.filterRoomId,
                onMediaClick = { serverName, mediaId ->
                    navController.navigate(MediaDetail(route.serverId, route.serverUrl, serverName, mediaId))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = null,
            )
        }

        composable<MediaDetail> { backStack ->
            val route = backStack.toRoute<MediaDetail>()
            MediaDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                serverName = route.serverName,
                mediaId = route.mediaId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<More> { backStack ->
            val route = backStack.toRoute<More>()
            MoreScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onFederation = {
                    navController.navigate(FederationList(route.serverId, route.serverUrl))
                },
                onBackgroundJobs = {
                    navController.navigate(BackgroundJobs(route.serverId, route.serverUrl))
                },
                onAuditLog = {
                    navController.navigate(AuditLog(route.serverId))
                },
                onSettings = {
                    navController.navigate(AppLockSettings)
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
            )
        }

        composable<FederationList> { backStack ->
            val route = backStack.toRoute<FederationList>()
            FederationListScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onDestinationClick = { destination ->
                    navController.navigate(FederationDetail(route.serverId, route.serverUrl, destination))
                },
                onServers = {
                    navController.navigate(ServerList) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<FederationDetail> { backStack ->
            val route = backStack.toRoute<FederationDetail>()
            FederationDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                destination = route.destination,
                onRoomClick = { roomId ->
                    navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<BackgroundJobs> { backStack ->
            val route = backStack.toRoute<BackgroundJobs>()
            BackgroundJobsScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
}
