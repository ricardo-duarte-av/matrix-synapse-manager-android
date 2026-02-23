package com.matrix.synapse.database

enum class AuditAction {
    LOGIN,
    CREATE_USER,
    UPDATE_USER,
    LOCK_USER,
    UNLOCK_USER,
    SUSPEND_USER,
    UNSUSPEND_USER,
    DEACTIVATE_USER,
    DELETE_DEVICE,
    EXPORT_LOG,
    DELETE_ROOM,
    BLOCK_ROOM,
    UNBLOCK_ROOM,
    JOIN_USER_TO_ROOM,
    MAKE_ROOM_ADMIN,
}
