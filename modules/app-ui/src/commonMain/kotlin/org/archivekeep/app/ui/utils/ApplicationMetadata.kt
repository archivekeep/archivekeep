package org.archivekeep.app.ui.utils

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices

interface ApplicationMetadata {
    val version: String

    companion object {
        val version: String
            @Composable
            get() = LocalApplicationServices.current.applicationMetadata.version
    }
}
