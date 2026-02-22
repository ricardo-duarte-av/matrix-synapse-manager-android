package com.matrix.synapse.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.settings.domain.ExportAuditLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class AuditLogState(
    val entries: List<AuditLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val exportedJson: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogger: AuditLogger,
    private val exportUseCase: ExportAuditLogUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AuditLogState())
    val state: StateFlow<AuditLogState> = _state.asStateFlow()

    fun load(serverId: String) {
        _state.value = AuditLogState(isLoading = true)
        viewModelScope.launch {
            runCatching { auditLogger.getAllByServer(serverId) }
                .onSuccess { _state.value = AuditLogState(entries = it) }
                .onFailure { _state.value = AuditLogState(error = it.message) }
        }
    }

    fun export(serverId: String) {
        viewModelScope.launch {
            runCatching { exportUseCase.exportAsJson(serverId) }
                .onSuccess { _state.value = _state.value.copy(exportedJson = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}

@Composable
fun AuditLogScreen(
    serverId: String,
    viewModel: AuditLogViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId) { viewModel.load(serverId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Audit Log",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.export(serverId) },
                modifier = Modifier.testTag("export_button"),
            ) { Text("Export JSON") }
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(modifier = Modifier.testTag("audit_loading")) }

            state.error != null -> Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp).testTag("audit_error"),
            )

            state.entries.isEmpty() -> Text(
                text = "No audit entries",
                modifier = Modifier.padding(16.dp),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().testTag("audit_list"),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    AuditEntryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AuditEntryRow(entry: AuditLogEntry) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        .format(Date(entry.timestampMs))
    ListItem(
        headlineContent = { Text(entry.action.name) },
        supportingContent = {
            Column {
                entry.targetUserId?.let { Text("User: $it") }
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }
        },
        modifier = Modifier.testTag("audit_entry_${entry.id}"),
    )
}
