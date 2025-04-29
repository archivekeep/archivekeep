package org.archivekeep.app.ui.utils

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.domain.wiring.staticCompositionLocalOfNotProvided

interface ApplicationMetadata {
    val version: String

    companion object {
        val version: String
            @Composable
            get() = LocalApplicationMetadata.current.version
    }
}

val LocalApplicationMetadata = staticCompositionLocalOfNotProvided<ApplicationMetadata>()
