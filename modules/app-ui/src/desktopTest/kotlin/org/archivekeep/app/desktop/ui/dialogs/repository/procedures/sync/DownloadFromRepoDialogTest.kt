package org.archivekeep.app.desktop.ui.dialogs.repository.procedures.sync

import org.archivekeep.app.desktop.ui.dialogs.testing.makeDialogScreenshot
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.DownloadFromRepoDialogPreview1Contents
import org.junit.Test

class DownloadFromRepoDialogScreenshotTest {
    @Test
    fun makeScreenshot() {
        makeDialogScreenshot("dialogs/sync/download-example.png") {
            DownloadFromRepoDialogPreview1Contents()
        }
    }
}
