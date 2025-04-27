package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow

val LocalComposeWindow = staticCompositionLocalOfNotProvided<ComposeWindow>()

val LocalOptionalComposeWindow = staticCompositionLocalOf<ComposeWindow?>(defaultFactory = { null })
