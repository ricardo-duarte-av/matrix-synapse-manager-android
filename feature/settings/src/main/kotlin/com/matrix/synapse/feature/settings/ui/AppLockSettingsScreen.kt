package com.matrix.synapse.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.settings.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockManager.setEnabled(enabled) }
    }
}

@Composable
fun AppLockSettingsScreen(
    viewModel: AppLockSettingsViewModel = hiltViewModel(),
) {
    val isEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Security",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App lock",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Require biometric or PIN authentication when the app is resumed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { viewModel.setEnabled(it) },
                modifier = Modifier.testTag("app_lock_toggle"),
            )
        }
    }
}
