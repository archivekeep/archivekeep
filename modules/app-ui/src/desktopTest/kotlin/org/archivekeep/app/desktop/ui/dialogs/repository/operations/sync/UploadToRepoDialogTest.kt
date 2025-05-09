package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import org.archivekeep.app.desktop.ui.dialogs.testing.makeDialogScreenshot
import org.archivekeep.app.ui.dialogs.repository.operations.sync.UploadToRepoDialogPreview1Contents
import org.junit.Test

class UploadToRepoDialogScreenshotTest {
    @Test
    fun makeScreenshot() {
        makeDialogScreenshot("dialogs/sync/upload-example.png") {
            UploadToRepoDialogPreview1Contents()
        }
    }
}
