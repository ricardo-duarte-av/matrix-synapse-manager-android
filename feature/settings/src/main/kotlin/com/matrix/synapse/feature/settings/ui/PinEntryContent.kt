package com.matrix.synapse.feature.settings.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val PIN_LENGTH = 4

/**
 * PIN entry UI: 4 dots and a numeric keypad.
 *
 * @param title Shown above the dots (e.g. "Enter PIN" or "Confirm PIN").
 * @param wrongPin When true, shows error message and triggers a short shake; reset when user types next digit.
 * @param errorMessage When [wrongPin] is true, this message is shown; default is "Wrong PIN. Try again."
 * @param onComplete Called when the user has entered exactly 4 digits (the PIN string).
 */
@Composable
fun PinEntryContent(
    title: String,
    wrongPin: Boolean = false,
    errorMessage: String? = null,
    onComplete: (pin: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(wrongPin) {
        if (wrongPin) {
            pin = ""
            shakeOffset.animateTo(0f)
            shakeOffset.animateTo(20f, tween(50))
            shakeOffset.animateTo(-20f, tween(50))
            shakeOffset.animateTo(20f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            onComplete(pin)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.value.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (wrongPin) {
            Text(
                text = errorMessage ?: "Wrong PIN. Try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.testTag("pin_dots"),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (index < pin.length)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape,
                        ),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back"),
            ).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    rowKeys.forEach { key ->
                        when {
                            key == "back" -> {
                                IconButton(
                                    onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                                    modifier = Modifier
                                        .size(72.dp)
                                        .testTag("pin_backspace"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            key.isNotEmpty() -> {
                                PinDigitButton(
                                    digit = key,
                                    onClick = {
                                        if (pin.length < PIN_LENGTH) pin += key
                                    },
                                )
                            }
                            else -> Spacer(Modifier.size(72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    digit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
