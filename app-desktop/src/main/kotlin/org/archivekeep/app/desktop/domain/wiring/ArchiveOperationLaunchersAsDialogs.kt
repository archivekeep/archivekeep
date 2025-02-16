package org.archivekeep.app.desktop.domain.wiring

import org.archivekeep.app.desktop.ui.dialogs.UnsupportedFeatureDialog
import org.archivekeep.app.desktop.ui.dialogs.addpush.AddAndPushRepoDialog
import org.archivekeep.app.desktop.ui.dialogs.indexupdate.UpdateIndexOperationDialog
import org.archivekeep.app.desktop.ui.dialogs.pushrepo.PushRepoDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.AddFileSystemRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.AddRemoteRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.AssociateRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.ForgetRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.RepositoryUnlockDialog
import org.archivekeep.app.desktop.ui.dialogs.repositories.UnassociateRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.sync.DownloadFromRepoDialog
import org.archivekeep.app.desktop.ui.dialogs.sync.UploadToRepoDialog
import org.archivekeep.app.desktop.ui.dialogs.verify.VerifyOperationDialog

fun archiveOperationLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer) =
    ArchiveOperationLaunchers(
        openAddAndPushOperation = dialogRenderer.openFn(::AddAndPushRepoDialog),
        openIndexUpdateOperation = dialogRenderer.openFn(::UpdateIndexOperationDialog),
        openVerifyOperation = dialogRenderer.openFn(::VerifyOperationDialog),
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
