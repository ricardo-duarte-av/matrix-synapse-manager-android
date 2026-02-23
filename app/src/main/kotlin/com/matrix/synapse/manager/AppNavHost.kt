package com.matrix.synapse.manager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.matrix.synapse.feature.auth.ui.LoginScreen
import com.matrix.synapse.feature.devices.ui.DeviceListScreen
import com.matrix.synapse.feature.devices.ui.WhoisScreen
import com.matrix.synapse.feature.servers.ui.ServerFormScreen
import com.matrix.synapse.feature.settings.ui.AppLockSettingsScreen
import com.matrix.synapse.feature.settings.ui.AuditLogScreen
import com.matrix.synapse.feature.users.ui.UserDetailScreen
import com.matrix.synapse.feature.users.ui.UserEditScreen
import com.matrix.synapse.feature.users.ui.UserListScreen
import com.matrix.synapse.feature.rooms.ui.RoomDetailScreen
import com.matrix.synapse.feature.rooms.ui.RoomListScreen
import com.matrix.synapse.feature.stats.ui.ServerDashboardScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ServerSetup,
        modifier = modifier,
    ) {
        composable<ServerSetup> {
            ServerFormScreen(
                onServerAdded = { serverId, serverUrl ->
                    navController.navigate(Login(serverId, serverUrl))
                },
            )
        }

        composable<Login> { backStack ->
            val route = backStack.toRoute<Login>()
            LoginScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                onLoginSuccess = {
                    navController.navigate(UserList(route.serverId, route.serverUrl)) {
                        popUpTo<ServerSetup> { inclusive = true }
                    }
                },
            )
        }

        composable<UserList> { backStack ->
            val route = backStack.toRoute<UserList>()
            UserListScreen(
                serverUrl = route.serverUrl,
                onUserClick = { userId ->
                    navController.navigate(UserDetail(route.serverId, route.serverUrl, userId))
                },
                onAuditLog = {
                    navController.navigate(AuditLog(route.serverId))
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
                onBack = { navController.popBackStack() },
            )
        }

        composable<RoomDetail> { backStack ->
            val route = backStack.toRoute<RoomDetail>()
            RoomDetailScreen(
                serverUrl = route.serverUrl,
                serverId = route.serverId,
                roomId = route.roomId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ServerDashboard> { backStack ->
            val route = backStack.toRoute<ServerDashboard>()
            ServerDashboardScreen(
                serverUrl = route.serverUrl,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
