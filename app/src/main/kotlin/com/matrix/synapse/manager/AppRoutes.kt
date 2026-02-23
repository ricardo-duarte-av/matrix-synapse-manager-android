package com.matrix.synapse.manager

import kotlinx.serialization.Serializable

@Serializable
object ServerSetup

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
data class AuditLog(val serverId: String)

@Serializable
object AppLockSettings

@Serializable
data class RoomList(val serverId: String, val serverUrl: String)

@Serializable
data class RoomDetail(val serverId: String, val serverUrl: String, val roomId: String)

@Serializable
data class ServerDashboard(val serverId: String, val serverUrl: String)
