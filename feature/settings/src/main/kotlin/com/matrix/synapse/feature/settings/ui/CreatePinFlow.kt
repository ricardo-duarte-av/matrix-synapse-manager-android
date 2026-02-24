package com.matrix.synapse.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matrix.synapse.core.ui.SynapseTopBar

/**
 * Two-step Create PIN flow: enter new PIN, then confirm.
 * Calls [onPinCreated] only when both match; [onCancel] aborts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePinContent(
    onPinCreated: (pin: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }
    var firstPin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SynapseTopBar(
                title = if (step == 0) "Enter pin" else "Re-enter pin",
                titleCentered = true,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (step) {
                    0 -> PinEntryContent(
                        title = "",
                        onComplete = { pin ->
                            firstPin = pin
                            step = 1
                        },
                    )
                    1 -> PinEntryContent(
                        title = "",
                        wrongPin = mismatch,
                        errorMessage = "PINs don't match. Try again.",
                        onComplete = { pin ->
                            if (pin == firstPin) {
                                onPinCreated(firstPin)
                            } else {
                                mismatch = true
                            }
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        when (step) {
                            0 -> onCancel()
                            1 -> { step = 0; firstPin = ""; mismatch = false }
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(if (step == 0) "Cancel" else "Back")
                }
            }
        }
    }
}

/**
 * Change PIN flow: enter current PIN, then create new PIN (enter + confirm).
 * Calls [onPinChanged] with the new PIN only after current is verified and new is confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinContent(
    verifyCurrentPin: (pin: String) -> Boolean,
    onPinChanged: (newPin: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }
    var wrongCurrent by remember { mutableStateOf(false) }
    var firstNewPin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    val topBarTitle = when (step) {
        0 -> "Enter current PIN"
        1 -> "Enter pin"
        2 -> "Re-enter pin"
        else -> ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SynapseTopBar(title = topBarTitle, titleCentered = true)
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (step) {
                    0 -> PinEntryContent(
                        title = "",
                        wrongPin = wrongCurrent,
                        onComplete = { pin ->
                            if (verifyCurrentPin(pin)) {
                                wrongCurrent = false
                                step = 1
                            } else {
                                wrongCurrent = true
                            }
                        },
                    )
                    1 -> PinEntryContent(
                        title = "",
                        onComplete = { pin ->
                            firstNewPin = pin
                            step = 2
                        },
                    )
                    2 -> PinEntryContent(
                        title = "",
                        wrongPin = mismatch,
                        errorMessage = "PINs don't match. Try again.",
                        onComplete = { pin ->
                            if (pin == firstNewPin) {
                                onPinChanged(firstNewPin)
                            } else {
                                mismatch = true
                            }
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        when (step) {
                            0 -> onCancel()
                            1 -> step = 0
                            2 -> { step = 1; firstNewPin = ""; mismatch = false }
                        }
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        when (step) {
                            0 -> "Cancel"
                            else -> "Back"
                        },
                    )
                }
            }
        }
    }
}
