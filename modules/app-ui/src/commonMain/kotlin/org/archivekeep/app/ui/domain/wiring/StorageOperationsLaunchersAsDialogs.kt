package org.archivekeep.app.ui.domain.wiring

import org.archivekeep.app.ui.dialogs.storages.MarkAsExternalDialog
import org.archivekeep.app.ui.dialogs.storages.MarkAsLocalDialog
import org.archivekeep.app.ui.dialogs.storages.RenameStorageDialog

fun storageOperationsLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer) =
    StorageOperationsLaunchers(
        openRename = dialogRenderer.openFn(::RenameStorageDialog),
        openMarkAsLocal = dialogRenderer.openFn(::MarkAsLocalDialog),
        openMarkAsExternal = dialogRenderer.openFn(::MarkAsExternalDialog),
    )
