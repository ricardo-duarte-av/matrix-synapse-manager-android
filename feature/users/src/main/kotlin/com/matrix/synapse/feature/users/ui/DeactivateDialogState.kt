package com.matrix.synapse.feature.users.ui

/**
 * State machine for the deactivate-user confirmation dialog.
 *
 * The administrator must type the target [userId] verbatim before the action
 * is enabled, satisfying the plan's "typed confirmation required" requirement.
 */
sealed interface DeactivateDialogState {

    data object Hidden : DeactivateDialogState

    data class Confirming(
        val userId: String,
        val deleteMedia: Boolean = false,
        val typedConfirmation: String = "",
    ) : DeactivateDialogState {
        val isConfirmed: Boolean get() = typedConfirmation == userId
    }

    data class InProgress(val deleteMedia: Boolean) : DeactivateDialogState

    data class Error(val message: String) : DeactivateDialogState

    data object Done : DeactivateDialogState
}
