package com.matrix.synapse.feature.devices.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.devices.data.DeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    serverUrl: String,
    userId: String,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl, userId) { viewModel.init(serverUrl, userId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.pendingDeleteDeviceId != null) {
        DeleteConfirmDialog(
            deviceId = state.pendingDeleteDeviceId!!,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devices", style = MaterialTheme.typography.titleLarge) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading || state.isDeleting -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("device_list_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp).testTag("device_list_error"),
                )

                state.devices.isEmpty() -> Text(
                    text = "No devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().testTag("device_list"),
                ) {
                    items(state.devices, key = { it.deviceId }) { device ->
                        DeviceRow(device = device, onDelete = { viewModel.requestDelete(device.deviceId) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: DeviceInfo, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.deviceId) },
        supportingContent = device.displayName?.let { { Text(it) } },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_device_${device.deviceId}"),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete device")
            }
        },
        modifier = Modifier.testTag("device_row_${device.deviceId}"),
    )
}

@Composable
private fun DeleteConfirmDialog(
    deviceId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete device?") },
        text = { Text("Remove $deviceId from this account. The user will be signed out on that device.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_device"),
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
