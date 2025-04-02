package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import org.archivekeep.app.desktop.domain.wiring.LocalComposeWindow

@Composable
actual fun fileKitPlatformSettings(): FileKitPlatformSettings =
    FileKitPlatformSettings(
        parentWindow = LocalComposeWindow.current,
    )
