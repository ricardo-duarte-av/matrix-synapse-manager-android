package com.matrix.synapse.manager.tabs

import kotlinx.coroutines.flow.Flow

interface TabOrderRepository {
    val order: Flow<List<TabItemId>>
    suspend fun setOrder(order: List<TabItemId>)
}
