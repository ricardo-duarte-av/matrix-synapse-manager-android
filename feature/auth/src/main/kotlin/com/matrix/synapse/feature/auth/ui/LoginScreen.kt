package com.matrix.synapse.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    serverUrl: String,
    serverId: String,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (state is LoginState.Success) {
        onLoginSuccess()
        viewModel.resetState()
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Admin Login", style = MaterialTheme.typography.headlineSmall)
        Text(serverUrl, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_username"),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag("login_password"),
        )

        if (state is LoginState.Error) {
            Text(
                text = (state as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("login_error"),
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.login(serverUrl, serverId, username, password) },
            enabled = state !is LoginState.Loading,
            modifier = Modifier.fillMaxWidth().testTag("login_button"),
        ) {
            if (state is LoginState.Loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Sign In")
            }
        }
    }
}
