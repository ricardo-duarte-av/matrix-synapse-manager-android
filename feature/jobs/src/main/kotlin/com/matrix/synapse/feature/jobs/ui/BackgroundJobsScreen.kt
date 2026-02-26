package com.matrix.synapse.feature.jobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val JOB_NAMES = listOf(
    "regenerate_directory" to R.string.job_regenerate_directory,
    "populate_stats_process_rooms" to R.string.job_populate_stats,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundJobsScreen(
    serverId: String,
    serverUrl: String,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.load(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = stringResource(R.string.background_jobs),
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.enabled == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.enabled == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { viewModel.load(serverId, serverUrl) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            else -> PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.load(serverId, serverUrl) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.enabled?.let { enabled ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(R.string.background_updates), style = MaterialTheme.typography.titleMedium)
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { viewModel.setEnabled(it) },
                                    enabled = !state.isToggling,
                                )
                            }
                        }
                    }

                    if (state.currentUpdates.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.current_updates), style = MaterialTheme.typography.titleMedium)
                                state.currentUpdates.forEach { (dbName, info) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(info.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(dbName, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(stringResource(R.string.items_count, info.totalItemCount), style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${info.totalDurationMs.toLong()} ms",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.run_job), style = MaterialTheme.typography.titleMedium)
                            JOB_NAMES.forEach { (name, labelResId) ->
                                Button(
                                    onClick = { viewModel.startJob(name) },
                                    enabled = !state.isStartingJob,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(labelResId))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
