package com.matrix.synapse.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import com.matrix.synapse.core.resources.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Matches list content horizontal padding so top bar title aligns with search field. */
private val TopBarHorizontalPadding = 24.dp

/**
 * Shared top app bar with consistent styling across the app.
 * Uses Material 3 [TopAppBar] for proper elevation, window insets (status bar / edge-to-edge), and accessibility.
 *
 * **Server context:** On server-scoped screens, use this to show the current server only.
 * Pass the server display name as [title], server URL as [subtitle], and [onTitleClick] to
 * open the server list (manage servers). The title area then acts as a button to manage the server list.
 *
 * For non-server screens (e.g. "Security", "Room Detail"), pass a plain [title] and no [onTitleClick].
 * If [onBack] is set, a back arrow is shown. [actions] slot for overflow menu or other icons.
 *
 * @param titleCentered When true, uses [CenterAlignedTopAppBar] so the title is centered (e.g. for dashboard).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseTopBar(
    title: String,
    subtitle: String? = null,
    onTitleClick: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleCentered: Boolean = false,
) {
    val currentServerTapDesc = stringResource(R.string.current_server_tap)
    val backDesc = stringResource(R.string.back_nav)
    val titleContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TopBarHorizontalPadding),
        ) {
            if (onTitleClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTitleClick)
                        .semantics { contentDescription = currentServerTapDesc }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                Column(modifier = Modifier.weight(1f, fill = true)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = if (titleCentered) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    val navigationIcon = @Composable {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backDesc,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    val actionsContent: @Composable RowScope.() -> Unit = {
        Spacer(Modifier.width(8.dp))
        actions()
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )
    val windowInsets = TopAppBarDefaults.windowInsets

    if (titleCentered) {
        CenterAlignedTopAppBar(
            modifier = Modifier,
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actionsContent,
            colors = colors,
            windowInsets = windowInsets,
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actionsContent,
            colors = colors,
            windowInsets = windowInsets,
        )
    }
}
