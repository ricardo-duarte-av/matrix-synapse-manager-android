package com.matrix.synapse.feature.servers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val ScreenPadding = 24.dp
private val FieldSpacing = 16.dp
private val SectionSpacing = 24.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormScreen(
    serverIdToEdit: String? = null,
    onServerAdded: (serverId: String, serverUrl: String) -> Unit,
    onServerUpdated: () -> Unit = {},
    viewModel: ServerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val serverToEdit by viewModel.serverToEdit.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(serverIdToEdit) {
        serverIdToEdit?.let { viewModel.loadForEdit(it) }
    }
    LaunchedEffect(serverToEdit) {
        serverToEdit?.let { server ->
            urlInput = server.inputUrl
            displayName = server.displayName
        }
    }

    if (state is ServerFormState.Success) {
        val success = state as ServerFormState.Success
        if (serverIdToEdit != null) {
            onServerUpdated()
        } else {
            onServerAdded(success.server.id, success.server.homeserverUrl)
        }
        viewModel.resetState()
    }

    val isEditMode = serverIdToEdit != null

    Scaffold(
        topBar = {
            SynapseTopBar(title = if (isEditMode) stringResource(R.string.edit_server) else stringResource(R.string.add_server))
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ScreenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(FieldSpacing),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.server_form_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(SectionSpacing))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { if (!isEditMode) urlInput = it },
                label = { Text(stringResource(R.string.server_url)) },
                placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                singleLine = true,
                enabled = !isEditMode,
                readOnly = isEditMode,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                isError = state is ServerFormState.Error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("server_url_field"),
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.display_name_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state is ServerFormState.Error) {
                Text(
                    text = (state as ServerFormState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("server_form_error"),
                )
            }

            Spacer(Modifier.height(SectionSpacing))

            Button(
                onClick = {
                    if (isEditMode && serverIdToEdit != null) {
                        viewModel.updateServer(serverIdToEdit, displayName)
                    } else {
                        viewModel.addServer(urlInput, displayName)
                    }
                },
                enabled = state !is ServerFormState.Discovering,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(if (isEditMode) "save_server_button" else "add_server_button"),
            ) {
                if (state is ServerFormState.Discovering) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(if (isEditMode) stringResource(R.string.save) else stringResource(R.string.add_server))
                }
            }
        }
    }
}
