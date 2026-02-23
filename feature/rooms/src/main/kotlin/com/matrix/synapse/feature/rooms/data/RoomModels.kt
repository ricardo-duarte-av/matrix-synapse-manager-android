package com.matrix.synapse.feature.rooms.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomSummary(
    @SerialName("room_id") val roomId: String,
    val name: String? = null,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("joined_members") val joinedMembers: Int = 0,
    @SerialName("joined_local_members") val joinedLocalMembers: Int = 0,
    val version: String? = null,
    val creator: String? = null,
    val encryption: String? = null,
    val federatable: Boolean = true,
    @SerialName("public") val isPublic: Boolean = false,
    @SerialName("join_rules") val joinRules: String? = null,
    @SerialName("guest_access") val guestAccess: String? = null,
    @SerialName("history_visibility") val historyVisibility: String? = null,
    @SerialName("state_events") val stateEvents: Int = 0,
    @SerialName("room_type") val roomType: String? = null,
)

@Serializable
data class RoomListResponse(
    val rooms: List<RoomSummary> = emptyList(),
    val offset: Int = 0,
    @SerialName("total_rooms") val totalRooms: Int = 0,
    @SerialName("next_batch") val nextBatch: Int? = null,
    @SerialName("prev_batch") val prevBatch: Int? = null,
)

@Serializable
data class RoomDetailResponse(
    @SerialName("room_id") val roomId: String,
    val name: String? = null,
    val topic: String? = null,
    val avatar: String? = null,
    @SerialName("canonical_alias") val canonicalAlias: String? = null,
    @SerialName("joined_members") val joinedMembers: Int = 0,
    @SerialName("joined_local_members") val joinedLocalMembers: Int = 0,
    @SerialName("joined_local_devices") val joinedLocalDevices: Int = 0,
    val version: String? = null,
    val creator: String? = null,
    val encryption: String? = null,
    val federatable: Boolean = true,
    @SerialName("public") val isPublic: Boolean = false,
    @SerialName("join_rules") val joinRules: String? = null,
    @SerialName("guest_access") val guestAccess: String? = null,
    @SerialName("history_visibility") val historyVisibility: String? = null,
    @SerialName("state_events") val stateEvents: Int = 0,
    @SerialName("room_type") val roomType: String? = null,
    val forgotten: Boolean = false,
)

@Serializable
data class RoomMembersResponse(
    val members: List<String> = emptyList(),
    val total: Int = 0,
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
    val results: List<DeleteStatus> = emptyList(),
)

@Serializable
data class DeleteStatus(
    @SerialName("delete_id") val deleteId: String,
    val status: String,
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
