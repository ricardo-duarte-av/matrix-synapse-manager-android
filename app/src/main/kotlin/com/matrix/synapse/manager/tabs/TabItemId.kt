package com.matrix.synapse.manager.tabs

/**
 * All items that can appear as a main tab or under More.
 * First 4 in [TabOrderRepository.order] are main tabs; the rest appear under More.
 */
enum class TabItemId(val label: String) {
    Users("Users"),
    Rooms("Rooms"),
    Stats("Stats"),
    Settings("Settings"),
    Federation("Federation"),
    BackgroundJobs("Jobs"),
    EventReports("Reports"),
    ;

    companion object {
        val defaultOrder: List<TabItemId> = listOf(
            Users,
            Rooms,
            Stats,
            Settings,
            Federation,
            BackgroundJobs,
            EventReports,
        )
    }
}
