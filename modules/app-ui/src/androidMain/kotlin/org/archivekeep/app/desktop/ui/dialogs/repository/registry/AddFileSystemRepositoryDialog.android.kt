package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.core.FileKitPlatformSettings

@Composable
actual fun fileKitPlatformSettings(): FileKitPlatformSettings = FileKitPlatformSettings()
