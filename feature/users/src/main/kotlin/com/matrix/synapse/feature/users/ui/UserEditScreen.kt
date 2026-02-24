package com.matrix.synapse.feature.users.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen for creating a new Synapse user.
 *
 * For editing an existing user, pass [existingUserId]; the password field is then hidden
 * since passwords are not required for updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditScreen(
    serverUrl: String,
    existingUserId: String? = null,
    onSaved: (userId: String) -> Unit,
    viewModel: UserEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedUserId) {
        state.savedUserId?.let { onSaved(it) }
    }

    LaunchedEffect(serverUrl) {
        if (existingUserId == null) viewModel.loadServerName(serverUrl)
    }

    val title = if (existingUserId == null) "Create User" else "Edit User"
    val serverName = state.serverName ?: remember(serverUrl) { serverNameFromUrl(serverUrl) }
    Scaffold(
        topBar = {
            SynapseTopBar(title = title)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            if (existingUserId == null) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> c != '@' && c != ':' } },
                    label = { Text("Username") },
                    supportingText = {
                        val preview = username.trim()
                        Text(if (preview.isNotBlank()) "@$preview:$serverName" else " ")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_user_id"),
                )
            } else {
                OutlinedTextField(
                    value = existingUserId,
                    onValueChange = { },
                    label = { Text("User ID") },
                    enabled = false,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_user_id"),
                )
            }

        if (existingUserId == null) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().testTag("edit_password"),
            )
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("edit_display_name"),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isAdmin,
                onCheckedChange = { isAdmin = it },
                modifier = Modifier.testTag("edit_admin_checkbox"),
            )
            Text("Server admin")
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("edit_error"),
            )
        }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (existingUserId == null) {
                        val fullUserId = "@${username.trim()}:$serverName"
                        viewModel.createUser(
                            serverUrl = serverUrl,
                            userId = fullUserId,
                            password = password,
                            displayName = displayName.ifBlank { null },
                            admin = isAdmin,
                        )
                    } else {
                        viewModel.updateUser(
                            serverUrl = serverUrl,
                            userId = existingUserId,
                            displayName = displayName.ifBlank { null },
                            admin = if (isAdmin) true else null,
                        )
                    }
                },
                enabled = !state.isSaving && (existingUserId != null || username.isNotBlank()),
                modifier = Modifier.fillMaxWidth().testTag("edit_save_button"),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(if (existingUserId == null) "Create" else "Save")
                }
            }
        }
    }
}

private fun serverNameFromUrl(serverUrl: String): String {
    return try {
        val uri = java.net.URI(serverUrl)
        var host = uri.host ?: return serverUrl
        // Use domain for Matrix ID (e.g. matrix.myserver.com -> myserver.com)
        if (host.startsWith("matrix.")) host = host.removePrefix("matrix.")
        val port = uri.port
        if (port > 0 && port != 80 && port != 443) "$host:$port" else host
    } catch (_: Exception) {
        val fallback = serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (fallback.startsWith("matrix.")) fallback.removePrefix("matrix.") else fallback
    }
}
