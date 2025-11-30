package org.archivekeep.app.ui.domain.wiring

import org.archivekeep.app.ui.dialogs.other.UnsupportedFeatureDialog
import org.archivekeep.app.ui.dialogs.repository.access.UnlockRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.management.AssociateRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.management.UnassociateRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.addpush.AddAndPushRepoDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.indexupdate.IndexUpdateProcedureDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.pushrepo.PushRepoDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.DownloadFromRepoDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.UploadToRepoDialog
import org.archivekeep.app.ui.dialogs.repository.registry.AddFileSystemRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.registry.AddRemoteRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.registry.DeinitializeFileSystemRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.registry.ForgetRepositoryDialog

fun archiveOperationLaunchersAsDialogs(dialogRenderer: OverlayDialogRenderer) =
    ArchiveOperationLaunchers(
        openAddAndPushOperation = dialogRenderer.openFn(::AddAndPushRepoDialog),
        openIndexUpdateOperation = dialogRenderer.openFn(::IndexUpdateProcedureDialog),
        openAssociateRepository = dialogRenderer.openFn(::AssociateRepositoryDialog),
        openUnassociateRepository = dialogRenderer.openFn(::UnassociateRepositoryDialog),
        openForgetRepository = dialogRenderer.openFn(::ForgetRepositoryDialog),
        openDeinitializeFilesystemRepository = dialogRenderer.openFn(::DeinitializeFileSystemRepositoryDialog),
        unlockRepository = dialogRenderer.openFn(::UnlockRepositoryDialog),
        pushRepoToAll = dialogRenderer.openFn(::PushRepoDialog),
        openAddFileSystemRepository = dialogRenderer.openFn(::AddFileSystemRepositoryDialog),
        openAddRemoteRepository = dialogRenderer.openFn(::AddRemoteRepositoryDialog),
        pushAllToStorage = { dialogRenderer.openDialog(UnsupportedFeatureDialog()) },
        pullAllFromStorage = { dialogRenderer.openDialog(UnsupportedFeatureDialog()) },
        pushToRepo = dialogRenderer.openFn(::UploadToRepoDialog),
        pullFromRepo = dialogRenderer.openFn(::DownloadFromRepoDialog),
    )
