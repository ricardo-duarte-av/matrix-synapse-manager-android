# Design: Deactivate Button, Media Management, Federation

**Date**: 2026-02-23
**Status**: Approved

## Overview

Three features for the Matrix Synapse Manager Android app:
1. Wire deactivate button into UserDetailScreen (existing UseCase, no new module)
2. Media management — new `:feature:media` module with full admin suite
3. Federation — new `:feature:federation` module with destination monitoring and connection reset

Navigation: "Media" and "Federation" icon buttons added to UserListScreen top bar alongside existing Rooms and Stats.

---

## 1. Deactivate Button

No new module, no new routes, no API changes. UseCase and API already exist.

### ViewModel changes (`UserDetailViewModel`)
- Add `isDeactivating: Boolean` and `isDeactivated: Boolean` to `UserDetailState`
- Add `deactivateUser(serverUrl, userId, deleteMedia, confirmed)` method calling `DeactivateUserUseCase`
- On success: set `isDeactivated = true`, show success snackbar

### UI changes (`UserDetailScreen`)
- Red "Deactivate User" `OutlinedButton` at bottom of action buttons
- Tapping shows confirmation `AlertDialog`:
  - Warning text: action is irreversible
  - "Delete media too" checkbox (maps to `deleteMedia` param)
  - "Cancel" and "Deactivate" buttons
- While deactivating: progress indicator
- After deactivation: disable button, show "User Deactivated" state

---

## 2. Media Management (`:feature:media`)

### Data Layer

**`MediaAdminApi`** endpoints:
- `GET /_synapse/admin/v1/room/{roomId}/media` — list media in a room
- `GET /_synapse/admin/v1/media/{serverName}/{mediaId}` — query media by ID
- `POST /_synapse/admin/v1/media/quarantine/{serverName}/{mediaId}` — quarantine
- `POST /_synapse/admin/v1/media/unquarantine/{serverName}/{mediaId}` — unquarantine
- `POST /_synapse/admin/v1/media/protect/{mediaId}` — protect from quarantine
- `POST /_synapse/admin/v1/media/unprotect/{mediaId}` — unprotect
- `DELETE /_synapse/admin/v1/media/{serverName}/{mediaId}` — delete specific media
- `POST /_synapse/admin/v1/media/delete?before_ts=&size_gt=&keep_profiles=` — bulk delete by date/size
- `POST /_synapse/admin/v1/purge_media_cache?before_ts=` — purge remote media cache

**`MediaRepository`**: wraps API, creates per-server instances via `RetrofitFactory`.

**Models**: `MediaInfo`, `RoomMediaResponse` (local + remote arrays), `QuarantineResponse`, `DeleteMediaResponse`, `PurgeMediaCacheResponse`.

### Screens & ViewModels

**`MediaListScreen`** (standalone, from top bar):
- Two entry modes (no single "list all server media" endpoint):
  - By room: pick room, shows that room's media
  - By user: uses existing `listUserMedia` from `UserAdminApi`
- Each item: media ID, upload date, size, content type, quarantine status
- Per-item actions: quarantine/unquarantine, protect/unprotect, delete
- Top bar actions: "Bulk Delete" (date/size dialog), "Purge Remote Cache" (timestamp dialog)

**`MediaDetailScreen`** (tapped from list):
- Full media info from query endpoint
- Action buttons: quarantine/unquarantine, protect/unprotect, delete (with confirmation)

**Contextual integration**:
- `UserDetailScreen`: "Media" button navigating to media list filtered by user
- `RoomDetailScreen`: "Media" button navigating to media list filtered by room

### Routes
- `MediaList(serverId, serverUrl, filterUserId?, filterRoomId?)`
- `MediaDetail(serverId, serverUrl, serverName, mediaId)`

### Audit Actions
`QUARANTINE_MEDIA`, `UNQUARANTINE_MEDIA`, `PROTECT_MEDIA`, `UNPROTECT_MEDIA`, `DELETE_MEDIA`, `BULK_DELETE_MEDIA`, `PURGE_REMOTE_MEDIA_CACHE`

---

## 3. Federation (`:feature:federation`)

### Data Layer

**`FederationAdminApi`** endpoints:
- `GET /_synapse/admin/v1/federation/destinations?from=&limit=&order_by=&dir=` — list destinations
- `GET /_synapse/admin/v1/federation/destinations/{destination}` — destination details
- `GET /_synapse/admin/v1/federation/destinations/{destination}/rooms?from=&limit=&dir=` — shared rooms
- `POST /_synapse/admin/v1/federation/destinations/{destination}/reset_connection` — reset retry timing

**`FederationRepository`**: wraps API, creates per-server instances via `RetrofitFactory`.

**Models**:
- `FederationDestination`: destination, retryLastTs, retryInterval, failureTs (nullable), lastSuccessfulStreamOrdering (nullable)
- `FederationDestinationsResponse`: destinations list, total, nextToken
- `DestinationRoom`: roomId, streamOrdering
- `DestinationRoomsResponse`: rooms list, total, nextToken

### Screens & ViewModels

**`FederationListScreen`** (from top bar):
- Paginated destination list, default sort by name
- Health indicators: green (healthy, failureTs == null), red (failing, has failureTs), yellow (retrying, retryInterval > 0 with past success)
- Sort options: destination name, last retry, retry interval, failure time

**`FederationDetailScreen`** (tapped from list):
- Header: server name + health indicator
- Timing info: first failure, last retry, retry interval (human-readable relative timestamps)
- "Reset Connection" button with confirmation dialog
- Shared rooms: paginated list, tapping room navigates to RoomDetail

### Routes
- `FederationList(serverId, serverUrl)`
- `FederationDetail(serverId, serverUrl, destination)`

### Audit Actions
`RESET_FEDERATION_CONNECTION`

---

## Module Dependencies

```
:feature:media    -> :core:network, :core:database, :core:model
:feature:federation -> :core:network, :core:database, :core:model
:app              -> :feature:media, :feature:federation (new)
```

## Navigation Additions

- `UserListScreen` top bar: add Media and Federation icon buttons
- `UserDetailScreen`: add "Media" button -> `MediaList(filterUserId=userId)`
- `RoomDetailScreen`: add "Media" button -> `MediaList(filterRoomId=roomId)`
- `FederationDetailScreen` shared rooms: tap room -> `RoomDetail`
