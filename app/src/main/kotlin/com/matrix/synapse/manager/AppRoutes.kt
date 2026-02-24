package com.matrix.synapse.manager

import kotlinx.serialization.Serializable

@Serializable
object ServerSetup

@Serializable
object ServerList

@Serializable
data class ServerEdit(val serverId: String)

@Serializable
data class Login(val serverId: String, val serverUrl: String)

@Serializable
data class UserList(val serverId: String, val serverUrl: String)

@Serializable
data class UserDetail(val serverId: String, val serverUrl: String, val userId: String)

@Serializable
data class UserEdit(val serverUrl: String, val userId: String)

@Serializable
data class DeviceList(val serverUrl: String, val userId: String)

@Serializable
data class Whois(val serverUrl: String, val userId: String)

@Serializable
object AppLockSettings

@Serializable
object RearrangeTabs

@Serializable
data class RoomList(val serverId: String, val serverUrl: String)

@Serializable
data class RoomDetail(val serverId: String, val serverUrl: String, val roomId: String)

@Serializable
data class ServerDashboard(val serverId: String, val serverUrl: String)

@Serializable
data class MediaList(
    val serverId: String,
    val serverUrl: String,
    val filterUserId: String? = null,
    val filterRoomId: String? = null,
)

@Serializable
data class MediaDetail(
    val serverId: String,
    val serverUrl: String,
    val serverName: String,
    val mediaId: String,
)

@Serializable
data class More(val serverId: String = "", val serverUrl: String = "")

@Serializable
data class Settings(val serverId: String, val serverUrl: String)

@Serializable
data class FederationList(val serverId: String, val serverUrl: String)

@Serializable
data class FederationDetail(val serverId: String, val serverUrl: String, val destination: String)

@Serializable
data class BackgroundJobs(val serverId: String, val serverUrl: String)

@Serializable
data class EventReportsList(val serverId: String, val serverUrl: String)

@Serializable
data class EventReportDetail(val serverId: String, val serverUrl: String, val reportId: Long)
