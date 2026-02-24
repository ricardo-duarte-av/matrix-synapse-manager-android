package com.matrix.synapse.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.manager.tabs.TabItemId
import com.matrix.synapse.manager.tabs.TabOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RearrangeTabsViewModel @Inject constructor(
    private val tabOrderRepository: TabOrderRepository,
) : ViewModel() {

    val order: StateFlow<List<TabItemId>> = tabOrderRepository.order
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = TabItemId.defaultOrder,
        )

    fun setOrder(newOrder: List<TabItemId>) {
        viewModelScope.launch {
            tabOrderRepository.setOrder(newOrder)
        }
    }
}
