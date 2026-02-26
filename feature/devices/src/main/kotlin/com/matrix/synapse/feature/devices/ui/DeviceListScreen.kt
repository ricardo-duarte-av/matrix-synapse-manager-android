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
import com.matrix.synapse.core.ui.EmptyStateContent
import com.matrix.synapse.core.ui.Spacing
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
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
            SynapseTopBar(title = stringResource(R.string.devices))
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
                    modifier = Modifier.padding(Spacing.ScreenPadding).testTag("device_list_error"),
                )

                state.devices.isEmpty() -> EmptyStateContent(
                    title = stringResource(R.string.no_devices_found),
                    modifier = Modifier.testTag("device_list_empty"),
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
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_device_desc))
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
        title = { Text(stringResource(R.string.delete_device)) },
        text = { Text(stringResource(R.string.delete_device_message, deviceId)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_device"),
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
