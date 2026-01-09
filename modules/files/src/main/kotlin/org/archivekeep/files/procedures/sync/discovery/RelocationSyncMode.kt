package org.archivekeep.files.procedures.sync.discovery

sealed interface RelocationSyncMode {
    data object Disabled : RelocationSyncMode

    data object AdditiveDuplicating : RelocationSyncMode

    data class Move(
        val allowDuplicateIncrease: Boolean,
        val allowDuplicateReduction: Boolean,
    ) : RelocationSyncMode
}
