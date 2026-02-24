package com.matrix.synapse.manager.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForTabItem(id: TabItemId): ImageVector = when (id) {
    TabItemId.Users -> Icons.Filled.Person
    TabItemId.Rooms -> Icons.Filled.Home
    TabItemId.Stats -> Icons.Filled.Info
    TabItemId.Settings -> Icons.Filled.Settings
    TabItemId.Federation -> Icons.Filled.Public
    TabItemId.BackgroundJobs -> Icons.Filled.Schedule
    TabItemId.EventReports -> Icons.Filled.Report
    TabItemId.AuditLogs -> Icons.Filled.History
}
