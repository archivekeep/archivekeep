package org.archivekeep.app.desktop.domain.wiring

import org.archivekeep.app.desktop.ui.dialogs.storages.MarkAsExternalDialog
import org.archivekeep.app.desktop.ui.dialogs.storages.MarkAsLocalDialog
import org.archivekeep.app.desktop.ui.dialogs.storages.RenameStorageDialog

fun storageOperationsLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer) =
    StorageOperationsLaunchers(
        openRename = dialogRenderer.openFn(::RenameStorageDialog),
        openMarkAsLocal = dialogRenderer.openFn(::MarkAsLocalDialog),
        openMarkAsExternal = dialogRenderer.openFn(::MarkAsExternalDialog),
    )
