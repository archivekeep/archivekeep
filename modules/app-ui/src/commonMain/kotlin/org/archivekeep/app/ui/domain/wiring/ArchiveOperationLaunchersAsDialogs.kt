package org.archivekeep.app.ui.domain.wiring

import org.archivekeep.app.ui.dialogs.other.UnsupportedFeatureDialog
import org.archivekeep.app.ui.dialogs.repository.access.RepositoryUnlockDialog
import org.archivekeep.app.ui.dialogs.repository.management.AssociateRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.management.UnassociateRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.operations.addpush.AddAndPushRepoDialog
import org.archivekeep.app.ui.dialogs.repository.operations.indexupdate.UpdateIndexOperationDialog
import org.archivekeep.app.ui.dialogs.repository.operations.pushrepo.PushRepoDialog
import org.archivekeep.app.ui.dialogs.repository.operations.sync.DownloadFromRepoDialog
import org.archivekeep.app.ui.dialogs.repository.operations.sync.UploadToRepoDialog
import org.archivekeep.app.ui.dialogs.repository.registry.AddFileSystemRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.registry.AddRemoteRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.registry.ForgetRepositoryDialog

fun archiveOperationLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer) =
    ArchiveOperationLaunchers(
        openAddAndPushOperation = dialogRenderer.openFn(::AddAndPushRepoDialog),
        openIndexUpdateOperation = dialogRenderer.openFn(::UpdateIndexOperationDialog),
        openAssociateRepository = dialogRenderer.openFn(::AssociateRepositoryDialog),
        openUnassociateRepository = dialogRenderer.openFn(::UnassociateRepositoryDialog),
        openForgetRepository = dialogRenderer.openFn(::ForgetRepositoryDialog),
        unlockRepository = dialogRenderer.openFn(::RepositoryUnlockDialog),
        pushRepoToAll = dialogRenderer.openFn(::PushRepoDialog),
        openAddFileSystemRepository = dialogRenderer.openFn(::AddFileSystemRepositoryDialog),
        openAddRemoteRepository = dialogRenderer.openFn(::AddRemoteRepositoryDialog),
        pushAllToStorage = { dialogRenderer.openDialog(UnsupportedFeatureDialog()) },
        pullAllFromStorage = { dialogRenderer.openDialog(UnsupportedFeatureDialog()) },
        pushToRepo = dialogRenderer.openFn(::UploadToRepoDialog),
        pullFromRepo = dialogRenderer.openFn(::DownloadFromRepoDialog),
    )
