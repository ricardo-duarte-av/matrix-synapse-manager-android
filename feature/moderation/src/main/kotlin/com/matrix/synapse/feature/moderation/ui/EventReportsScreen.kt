package com.matrix.synapse.feature.moderation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.moderation.data.EventReportSummary
import com.matrix.synapse.feature.rooms.data.RoomSummary
import com.matrix.synapse.feature.users.data.UserSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventReportsScreen(
    serverId: String,
    serverUrl: String,
    onReportClick: (reportId: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: EventReportsViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.load(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var roomDropdownExpanded by remember { mutableStateOf(false) }
    var userDropdownExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        if (last >= state.reports.size - 3 && state.hasMore && !state.isLoadingMore) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.event_reports_title, state.total),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RoomFilterDropdown(
                            rooms = state.rooms,
                            roomsLoading = state.roomsLoading,
                            selectedRoomId = state.filterRoomId.takeIf { it.isNotBlank() },
                            expanded = roomDropdownExpanded,
                            onExpandedChange = { roomDropdownExpanded = it; if (it) userDropdownExpanded = false },
                            onRoomSelected = { viewModel.setFilters(it ?: "", state.filterUserId) },
                            modifier = Modifier.weight(1f),
                        )
                        UserFilterDropdown(
                            users = state.users,
                            usersLoading = state.usersLoading,
                            selectedUserId = state.filterUserId.takeIf { it.isNotBlank() },
                            expanded = userDropdownExpanded,
                            onExpandedChange = { userDropdownExpanded = it; if (it) roomDropdownExpanded = false },
                            onUserSelected = { viewModel.setFilters(state.filterRoomId, it ?: "") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.setSortNewestFirst(!state.sortNewestFirst)
                            },
                        ) { Text(if (state.sortNewestFirst) stringResource(R.string.newest_first) else stringResource(R.string.oldest_first)) }
                    }
                }
            }

            when {
                state.isLoading && state.reports.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("reports_loading")) }
                state.error != null && state.reports.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.load(serverId, serverUrl) }) { Text(stringResource(R.string.retry)) }
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("reports_list"),
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        EventReportRow(
                            report = report,
                            onClick = { onReportClick(report.id) },
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
                        }
                    }
                    if (state.reports.isEmpty() && !state.isLoading) {
                        item {
                            Text(
                                stringResource(R.string.no_reports_found),
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventReportRow(
    report: EventReportSummary,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                report.eventId.take(40) + if (report.eventId.length > 40) "…" else "",
                maxLines = 1,
            )
        },
        supportingContent = {
            Column {
                Text(stringResource(R.string.room_label, report.roomId), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                report.reason?.take(60)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                Text(
                    formatTs(report.receivedTs),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("report_row_${report.id}"),
    )
}

private fun formatTs(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomFilterDropdown(
    rooms: List<RoomSummary>,
    roomsLoading: Boolean,
    selectedRoomId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRoomSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRoom = rooms.find { it.roomId == selectedRoomId }
    val label = when {
        selectedRoomId == null -> stringResource(R.string.all_rooms)
        selectedRoom != null -> (selectedRoom.name?.takeIf { it.isNotBlank() } ?: selectedRoom.roomId)
        else -> selectedRoomId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("filter_room"),
    ) {
        OutlinedTextField(
            value = if (roomsLoading && rooms.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.room_filter)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_rooms)) },
                onClick = { onRoomSelected(null); onExpandedChange(false) },
            )
            rooms.forEach { room ->
                DropdownMenuItem(
                    text = { Text(room.name?.takeIf { it.isNotBlank() } ?: room.roomId, maxLines = 1) },
                    onClick = { onRoomSelected(room.roomId); onExpandedChange(false) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFilterDropdown(
    users: List<UserSummary>,
    usersLoading: Boolean,
    selectedUserId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUserSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedUser = users.find { it.userId == selectedUserId }
    val label = when {
        selectedUserId == null -> stringResource(R.string.all_reporters)
        selectedUser != null -> (selectedUser.displayName?.takeIf { it.isNotBlank() } ?: selectedUser.userId)
        else -> selectedUserId
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.testTag("filter_user"),
    ) {
        OutlinedTextField(
            value = if (usersLoading && users.isEmpty()) stringResource(R.string.loading) else label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reporter)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.all_reporters)) },
                onClick = { onUserSelected(null); onExpandedChange(false) },
            )
            users.forEach { user ->
                DropdownMenuItem(
                    text = { Text(user.displayName?.takeIf { it.isNotBlank() } ?: user.userId, maxLines = 1) },
                    onClick = { onUserSelected(user.userId); onExpandedChange(false) },
                )
            }
        }
    }
}
