package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.users.data.UserSummary
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    serverId: String,
    serverUrl: String,
    onUserClick: (userId: String) -> Unit,
    onAuditLog: () -> Unit = {},
    onSettings: () -> Unit = {},
    onServers: () -> Unit = {},
    onRooms: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onMedia: () -> Unit = {},
    onFederation: () -> Unit = {},
    viewModel: UserListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.init(serverId, serverUrl) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = state.currentServer?.displayName ?: serverUrl,
                subtitle = serverUrl,
                onTitleClick = onServers,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(text = { Text("Servers") }, onClick = { onServers(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Media") }, onClick = { onMedia(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Federation") }, onClick = { onFederation(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Rooms") }, onClick = { onRooms(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Stats") }, onClick = { onDashboard(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Audit log") }, onClick = { onAuditLog(); menuExpanded = false })
                            DropdownMenuItem(text = { Text("Settings") }, onClick = { onSettings(); menuExpanded = false })
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    viewModel.search(q)
                },
                label = { Text("Search users") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .testTag("user_search"),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("user_list_loading")) }

                state.error != null -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("user_list_error"),
                )

                else -> UserList(
                    users = state.users,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    onUserClick = onUserClick,
                    onLoadMore = { viewModel.loadNextPage() },
                )
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<UserSummary>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onUserClick: (userId: String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            hasMore && lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { shouldLoadMore }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_list"),
    ) {
        items(users, key = { it.userId }) { user ->
            UserRow(user = user, onClick = { onUserClick(user.userId) })
            HorizontalDivider()
        }
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun UserRow(user: UserSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.userId) },
        supportingContent = user.displayName?.let { { Text(it) } },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("user_row_${user.userId}"),
    )
}
