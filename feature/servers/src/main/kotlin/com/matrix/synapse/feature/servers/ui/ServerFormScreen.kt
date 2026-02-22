package com.matrix.synapse.feature.servers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ServerFormScreen(
    onServerAdded: () -> Unit,
    viewModel: ServerFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    if (state is ServerFormState.Success) {
        onServerAdded()
        viewModel.resetState()
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Add Server", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Server URL") },
            placeholder = { Text("e.g. matrix.example.com") },
            isError = state is ServerFormState.Error,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("server_url_field"),
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name (optional)") },
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

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.addServer(urlInput, displayName) },
            enabled = state !is ServerFormState.Discovering,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_server_button"),
        ) {
            if (state is ServerFormState.Discovering) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Add Server")
            }
        }
    }
}
