package com.matrix.synapse.manager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.manager.tabs.TabItemId
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RearrangeTabsScreen(
    onBack: () -> Unit,
    viewModel: RearrangeTabsViewModel = hiltViewModel(),
) {
    val order by viewModel.order.collectAsStateWithLifecycle(initialValue = TabItemId.defaultOrder)

    Scaffold(
        topBar = {
            SynapseTopBar(
                title = "Rearrange tabs",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "First 4 items are main tabs; the rest appear under More. Long-press the ☰ handle and drag to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ReorderableTabList(
                items = order,
                onReorder = viewModel::setOrder,
            )
        }
    }
}

@Composable
private fun ReorderableTabList(
    items: List<TabItemId>,
    onReorder: (List<TabItemId>) -> Unit,
) {
    var listState by remember { mutableStateOf(items.toList()) }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            listState = listState.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { _, _ ->
            onReorder(listState)
        },
    )

    LaunchedEffect(items, reorderableState.draggingItemIndex) {
        if (reorderableState.draggingItemIndex == null) {
            listState = items
        }
    }

    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
            .fillMaxWidth()
            .reorderable(reorderableState),
    ) {
        items(
            items = listState,
            key = { it },
        ) { item ->
            ReorderableItem(
                state = reorderableState,
                key = item,
            ) { isDragging ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    tonalElevation = if (isDragging) 8.dp else 1.dp,
                    shadowElevation = if (isDragging) 8.dp else 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .detectReorderAfterLongPress(reorderableState),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Drag to reorder",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
