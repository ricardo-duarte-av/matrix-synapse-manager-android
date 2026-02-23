package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UserDetailScreen(
    serverUrl: String,
    serverId: String,
    userId: String,
    onEdit: () -> Unit,
    onDevices: () -> Unit,
    onWhois: () -> Unit,
    onMedia: () -> Unit = {},
    viewModel: UserDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var deleteMediaChecked by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, serverId, userId) {
        viewModel.loadUser(serverUrl, serverId, userId)
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val user = state.user
        if (user == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("User not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(user.displayName ?: user.userId, style = MaterialTheme.typography.headlineSmall)
            Text(user.userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            if (user.admin) {
                Text("Admin", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Locked", style = MaterialTheme.typography.bodyMedium)
                if (state.isLocking) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                } else {
                    Switch(
                        checked = user.locked,
                        onCheckedChange = { locked -> viewModel.setLocked(serverUrl, userId, locked) },
                    )
                }
            }

            if (state.canSuspend) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Suspended", style = MaterialTheme.typography.bodyMedium)
                    if (state.isSuspending) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    } else {
                        Switch(
                            checked = user.suspended,
                            onCheckedChange = { suspended -> viewModel.setSuspended(serverUrl, userId, suspended) },
                        )
                    }
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit User")
            }
            OutlinedButton(onClick = onDevices, modifier = Modifier.fillMaxWidth()) {
                Text("Devices")
            }
            OutlinedButton(onClick = onWhois, modifier = Modifier.fillMaxWidth()) {
                Text("Whois / Sessions")
            }
            OutlinedButton(onClick = onMedia, modifier = Modifier.fillMaxWidth()) {
                Text("Media")
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            if (state.isDeactivated || user.deactivated) {
                Text(
                    "User Deactivated",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                OutlinedButton(
                    onClick = { showDeactivateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    enabled = !state.isDeactivating,
                ) {
                    if (state.isDeactivating) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp))
                    } else {
                        Text("Deactivate User")
                    }
                }
            }
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text("Deactivate User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This action is irreversible. The user will be permanently deactivated.")
                    Text(userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteMediaChecked,
                            onCheckedChange = { deleteMediaChecked = it },
                        )
                        Text("Delete all user media")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateDialog = false
                        viewModel.deactivateUser(serverUrl, serverId, userId, deleteMediaChecked)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Deactivate") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") }
            },
        )
    }
}
