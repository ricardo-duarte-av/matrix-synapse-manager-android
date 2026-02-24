package com.matrix.synapse.manager.tabs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "tab_order_prefs"
private const val KEY_ORDER = "order"

@Singleton
class DefaultTabOrderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : TabOrderRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _order = MutableStateFlow(loadOrder())

    override val order: Flow<List<TabItemId>> = _order.asStateFlow()

    override suspend fun setOrder(order: List<TabItemId>) {
        val valid = order.filter { it in TabItemId.entries }
        if (valid.size != TabItemId.entries.size) return
        val raw = valid.joinToString(",") { it.name }
        prefs.edit().putString(KEY_ORDER, raw).apply()
        _order.value = valid
    }

    private fun loadOrder(): List<TabItemId> {
        val raw = prefs.getString(KEY_ORDER, null) ?: return TabItemId.defaultOrder
        return raw.split(",").mapNotNull { part ->
            TabItemId.entries.firstOrNull { it.name == part.trim() }
        }.let { parsed ->
            val missing = TabItemId.entries - parsed.toSet()
            if (missing.isEmpty()) parsed else TabItemId.defaultOrder
        }
    }
}
