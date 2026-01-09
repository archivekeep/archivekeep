package org.archivekeep.cli.commands.mixins

import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode
import picocli.CommandLine.Option

class SyncOptions {
    @Option(
        names = ["--resolve-moves"],
        description = ["resolves added files against checksums of existing remote files"],
    )
    var resolveMoves: Boolean = false

    @Option(
        names = ["--allow-duplicate-increase"],
        description = ["allow to increase duplication of existing files"],
    )
    var allowDuplicateIncrease: Boolean = false

    @Option(
        names = ["--allow-duplicate-reduction"],
        description = ["allow to decrease duplication of existing files"],
    )
    var allowDuplicateReduction: Boolean = false

    @Option(
        names = ["--additive-duplicating"],
        description = ["push all new local files to remote without resolving moves or checking for duplication increase"],
    )
    var additiveDuplicating: Boolean = false

    internal val syncMode: RelocationSyncMode
        get() =
            when {
                additiveDuplicating -> RelocationSyncMode.AdditiveDuplicating
                resolveMoves ->
                    RelocationSyncMode.Move(
                        allowDuplicateIncrease = allowDuplicateIncrease,
                        allowDuplicateReduction = allowDuplicateReduction,
                    )

                else -> RelocationSyncMode.Disabled
            }
}
