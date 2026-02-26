package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.matrix.synapse.feature.users.data.mxcToDownloadUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    serverUrl: String,
    serverId: String,
    userId: String,
    onEdit: () -> Unit,
    onDevices: () -> Unit,
    onWhois: () -> Unit,
    onMedia: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: UserDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeactivateDialog by remember { mutableStateOf(false) }
    var deleteMediaChecked by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, serverId, userId) {
        viewModel.loadUser(serverUrl, serverId, userId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect { onBack?.invoke() }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.user?.displayName ?: state.user?.userId ?: stringResource(R.string.user),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading && state.user == null) {
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
                Text(stringResource(R.string.user_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val userAvatarUrl = mxcToDownloadUrl(serverUrl, user.avatarUrl)
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadUser(serverUrl, serverId, userId) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Full-width avatar at top
            if (userAvatarUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                ) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Text(user.userId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(user.userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

            if (user.admin) {
                Text(stringResource(R.string.admin), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }

            HorizontalDivider()

            val isCurrentUser = userId == state.currentUserId
            if (isCurrentUser) {
                Text(
                    stringResource(R.string.you_cannot_lock_yourself),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.locked), style = MaterialTheme.typography.bodyMedium)
                if (state.isLocking) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                } else {
                    Switch(
                        checked = user.locked,
                        onCheckedChange = { locked -> viewModel.setLocked(serverUrl, userId, locked) },
                        enabled = !isCurrentUser,
                    )
                }
            }

            if (state.canSuspend) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.suspended), style = MaterialTheme.typography.bodyMedium)
                    if (state.isSuspending) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    } else {
                        Switch(
                            checked = user.suspended,
                            onCheckedChange = { suspended -> viewModel.setSuspended(serverUrl, userId, suspended) },
                            enabled = !isCurrentUser,
                        )
                    }
                }
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.edit_user))
            }
            OutlinedButton(onClick = onDevices, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.devices))
            }
            OutlinedButton(onClick = onWhois, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.whois_sessions))
            }
            OutlinedButton(onClick = onMedia, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.media))
            }

            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            if (state.isDeactivated || user.deactivated) {
                Text(
                    stringResource(R.string.user_deactivated),
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
                    enabled = !state.isDeactivating && userId != state.currentUserId,
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
        }
    }

    if (showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = false },
            title = { Text(stringResource(R.string.deactivate_user_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.deactivate_user_message))
                    Text(userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteMediaChecked,
                            onCheckedChange = { deleteMediaChecked = it },
                        )
                        Text(stringResource(R.string.delete_all_user_media))
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
                ) { Text(stringResource(R.string.deactivate)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeactivateDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
