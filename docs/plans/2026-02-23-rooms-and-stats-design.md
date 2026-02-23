# Room Management & Server Stats — Design

## Overview

Add two new feature modules: `:feature:rooms` (full CRUD room management) and `:feature:stats` (server dashboard with monitoring).

## Module Structure

```
:feature:rooms
  data/
    RoomAdminApi.kt          — Retrofit interface
    RoomModels.kt            — Request/response data classes
    RoomRepository.kt        — Caches room list, delegates to API
  domain/
    DeleteRoomUseCase.kt     — Async delete with status polling
  ui/
    RoomListViewModel.kt
    RoomListScreen.kt        — Paginated, searchable room list
    RoomDetailViewModel.kt
    RoomDetailScreen.kt      — Room info, members, block/delete/join/make-admin actions

:feature:stats
  data/
    StatsApi.kt              — Retrofit interface
    StatsModels.kt           — Response data classes
  ui/
    ServerDashboardViewModel.kt
    ServerDashboardScreen.kt — Version, totals, DAU/MAU, largest rooms, media usage
```

## Navigation

New routes in AppRoutes.kt:
- `data class RoomList(val serverId: String, val serverUrl: String)`
- `data class RoomDetail(val serverId: String, val serverUrl: String, val roomId: String)`
- `data class ServerDashboard(val serverId: String, val serverUrl: String)`

Entry points from UserListScreen top bar: "Rooms" button and "Dashboard" button.

## API Endpoints

### RoomAdminApi

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/_synapse/admin/v1/rooms` | List/search rooms (params: from, limit, order_by, dir, search_term) |
| GET | `/_synapse/admin/v1/rooms/{roomId}` | Room details |
| GET | `/_synapse/admin/v1/rooms/{roomId}/members` | Room member list |
| DELETE | `/_synapse/admin/v2/rooms/{roomId}` | Async delete room |
| GET | `/_synapse/admin/v2/rooms/{roomId}/delete_status` | Delete task status |
| PUT | `/_synapse/admin/v1/rooms/{roomId}/block` | Block/unblock room |
| GET | `/_synapse/admin/v1/rooms/{roomId}/block` | Check block status |
| POST | `/_synapse/admin/v1/join/{roomId}` | Join user to room |
| POST | `/_synapse/admin/v1/rooms/{roomId}/make_room_admin` | Grant admin power level |

### StatsApi

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/_synapse/admin/v1/server_version` | Server version string |
| GET | `/_synapse/admin/v1/statistics/database/rooms` | Largest rooms by DB size (PostgreSQL only) |
| GET | `/_synapse/admin/v1/statistics/users/media` | Per-user media usage stats |

Total users/rooms derived from existing list endpoints with `limit=1` (using `total` field).
DAU/MAU derived from user list sorted by `last_seen_ts`, counting users within 24h/30d windows.

## Data Models

### Room Models

```kotlin
@Serializable
data class RoomSummary(
    @SerialName("room_id") val roomId: String,
    val name: String? = null,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("joined_members") val joinedMembers: Int,
    @SerialName("joined_local_members") val joinedLocalMembers: Int,
    val version: String? = null,
    val creator: String? = null,
    val encryption: String? = null,
    val federatable: Boolean = true,
    val public: Boolean = false,
    @SerialName("join_rules") val joinRules: String? = null,
    @SerialName("guest_access") val guestAccess: String? = null,
    @SerialName("history_visibility") val historyVisibility: String? = null,
    @SerialName("state_events") val stateEvents: Int = 0,
    @SerialName("room_type") val roomType: String? = null,
)

@Serializable
data class RoomListResponse(
    val rooms: List<RoomSummary>,
    val offset: Int,
    @SerialName("total_rooms") val totalRooms: Int,
    @SerialName("next_batch") val nextBatch: Int? = null,
    @SerialName("prev_batch") val prevBatch: Int? = null,
)

@Serializable
data class RoomDetail(
    @SerialName("room_id") val roomId: String,
    val name: String? = null,
    val topic: String? = null,
    val avatar: String? = null,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("joined_members") val joinedMembers: Int,
    @SerialName("joined_local_members") val joinedLocalMembers: Int,
    @SerialName("joined_local_devices") val joinedLocalDevices: Int = 0,
    val version: String? = null,
    val creator: String? = null,
    val encryption: String? = null,
    val federatable: Boolean = true,
    val public: Boolean = false,
    @SerialName("join_rules") val joinRules: String? = null,
    @SerialName("guest_access") val guestAccess: String? = null,
    @SerialName("history_visibility") val historyVisibility: String? = null,
    @SerialName("state_events") val stateEvents: Int = 0,
    @SerialName("room_type") val roomType: String? = null,
    val forgotten: Boolean = false,
)

@Serializable
data class RoomMembersResponse(
    val members: List<String>,
    val total: Int,
)

@Serializable
data class DeleteRoomRequest(
    val purge: Boolean = true,
    val block: Boolean = false,
    val message: String? = null,
    @SerialName("force_purge") val forcePurge: Boolean = false,
)

@Serializable
data class DeleteRoomResponse(
    @SerialName("delete_id") val deleteId: String,
)

@Serializable
data class DeleteStatusResponse(
    val results: List<DeleteStatus>,
)

@Serializable
data class DeleteStatus(
    @SerialName("delete_id") val deleteId: String,
    val status: String, // scheduled, active, complete, failed
    val error: String? = null,
)

@Serializable
data class BlockRoomRequest(val block: Boolean)

@Serializable
data class BlockRoomResponse(val block: Boolean)

@Serializable
data class JoinRoomRequest(@SerialName("user_id") val userId: String)

@Serializable
data class MakeRoomAdminRequest(@SerialName("user_id") val userId: String? = null)
```

### Stats Models

```kotlin
@Serializable
data class ServerVersionResponse(
    @SerialName("server_version") val serverVersion: String,
)

@Serializable
data class DatabaseRoomStatsResponse(
    val rooms: List<RoomSizeEntry>,
)

@Serializable
data class RoomSizeEntry(
    @SerialName("room_id") val roomId: String,
    @SerialName("estimated_size") val estimatedSize: Long,
)

@Serializable
data class MediaUsageResponse(
    val users: List<UserMediaStats>,
    @SerialName("next_token") val nextToken: Int? = null,
    val total: Int,
)

@Serializable
data class UserMediaStats(
    @SerialName("user_id") val userId: String,
    val displayname: String? = null,
    @SerialName("media_count") val mediaCount: Int,
    @SerialName("media_length") val mediaLength: Long,
)
```

## Screen Details

### RoomListScreen
- Paginated list (infinite scroll, same pattern as UserListScreen)
- Search via `search_term` query param
- Sort dropdown: name, joined_members, state_events
- Each row: room name (fallback to room ID), member count badge, lock icon if encryption set
- Tap navigates to RoomDetailScreen

### RoomDetailScreen
- Header card: name, topic, canonical alias, room ID (copyable)
- Info grid: version, creator, join rules, guest access, history visibility, federation, encryption, state event count, member count
- Members section: expandable/collapsible list of member user IDs
- Action buttons:
  - **Block/Unblock** toggle (fetches current block status on load)
  - **Delete Room** — opens confirmation dialog with: purge checkbox (default true), block checkbox, optional message field. Uses async v2 endpoint.
  - **Make Room Admin** — grants highest power level to current admin user
  - **Join User** — text field for user ID + join button

### ServerDashboardScreen
- **Version card**: server version string
- **Summary cards row**: total users | total rooms (from list API `total` fields)
- **Active users cards**: DAU (last 24h) | MAU (last 30d) — derived from user list `last_seen_ts`
- **Largest rooms table**: room ID, name, estimated DB size — from statistics/database/rooms endpoint. Shows "PostgreSQL required" message on 400/404 error.
- **Top media users table**: user ID, display name, media count, total size — from statistics/users/media endpoint

## Error Handling
- All API calls follow existing pattern: catch HttpException + IOException, return Result
- Delete room status polling: poll every 2 seconds, stop on complete/failed, timeout after 60 seconds
- Database room stats: graceful fallback card when server uses SQLite (endpoint returns error)

## Audit Logging
- Room deletion, blocking/unblocking logged to existing AuditLogger
- No audit logging for read-only stats operations

## Testing
- Unit tests for DeleteRoomUseCase (status polling logic)
- Unit tests for DAU/MAU derivation logic
- ViewModel tests following existing patterns (MockK + Turbine)
