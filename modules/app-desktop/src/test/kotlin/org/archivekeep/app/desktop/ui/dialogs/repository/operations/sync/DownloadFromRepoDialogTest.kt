package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import org.archivekeep.app.desktop.ui.dialogs.testing.makeDialogScreenshot
import org.junit.Test

class DownloadFromRepoDialogScreenshotTest {
    @Test
    fun makeScreenshot() {
        makeDialogScreenshot("dialogs/sync/download-example.png") {
            DownloadFromRepoDialogPreview1Contents()
        }
    }
}
