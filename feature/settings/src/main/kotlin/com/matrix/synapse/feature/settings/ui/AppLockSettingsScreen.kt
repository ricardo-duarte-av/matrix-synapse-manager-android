package com.matrix.synapse.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.matrix.synapse.core.ui.SynapseTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.resources.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.settings.security.AppLockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
) : ViewModel() {

    val isLockEnabled: StateFlow<Boolean> = appLockManager.isLockEnabled

    private val _showCreatePin = MutableStateFlow(false)
    val showCreatePin: StateFlow<Boolean> = _showCreatePin.asStateFlow()

    private val _showChangePin = MutableStateFlow(false)
    val showChangePin: StateFlow<Boolean> = _showChangePin.asStateFlow()

    fun pinExists(): Boolean = appLockManager.pinExists()

    /** Call when user turns the lock switch ON. If no PIN exists, shows Create PIN; else enables lock. */
    fun requestEnableLock() {
        viewModelScope.launch {
            if (appLockManager.pinExists()) {
                appLockManager.setEnabled(true)
            } else {
                _showCreatePin.value = true
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockManager.setEnabled(enabled) }
    }

    fun onPinCreated(pin: String) {
        viewModelScope.launch {
            appLockManager.setPin(pin)
            appLockManager.setEnabled(true)
            _showCreatePin.value = false
        }
    }

    fun onCreatePinCancel() {
        _showCreatePin.value = false
    }

    fun verifyPin(pin: String): Boolean = appLockManager.verifyPin(pin)

    fun onPinChanged(newPin: String) {
        viewModelScope.launch {
            appLockManager.setPin(newPin)
            _showChangePin.value = false
        }
    }

    fun onChangePinCancel() {
        _showChangePin.value = false
    }

    fun showChangePinFlow() {
        _showChangePin.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSettingsScreen(
    onRearrangeTabs: (() -> Unit)? = null,
    viewModel: AppLockSettingsViewModel = hiltViewModel(),
) {
    val isEnabled by viewModel.isLockEnabled.collectAsStateWithLifecycle()
    val showCreatePin by viewModel.showCreatePin.collectAsStateWithLifecycle()
    val showChangePin by viewModel.showChangePin.collectAsStateWithLifecycle()
    val pinExists = viewModel.pinExists()

    when {
        showCreatePin -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                CreatePinContent(
                    onPinCreated = viewModel::onPinCreated,
                    onCancel = viewModel::onCreatePinCancel,
                )
            }
        }
        showChangePin -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                ChangePinContent(
                    verifyCurrentPin = viewModel::verifyPin,
                    onPinChanged = viewModel::onPinChanged,
                    onCancel = viewModel::onChangePinCancel,
                )
            }
        }
        else -> Scaffold(
            topBar = {
                SynapseTopBar(title = stringResource(R.string.settings))
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            ) {
                onRearrangeTabs?.let { onNavigate ->
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.rearrange_tabs)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate() },
                    )
                    HorizontalDivider()
                }
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    headlineContent = { Text(stringResource(R.string.app_lock)) },
                    supportingContent = {
                        Text(
                            text = "Require app PIN when the app is resumed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                if (checked) viewModel.requestEnableLock()
                                else viewModel.setEnabled(false)
                            },
                            modifier = Modifier.testTag("app_lock_toggle"),
                        )
                    },
                )
                if (pinExists) {
                    HorizontalDivider()
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.change_pin)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showChangePinFlow() },
                    )
                }
            }
        }
    }
}
