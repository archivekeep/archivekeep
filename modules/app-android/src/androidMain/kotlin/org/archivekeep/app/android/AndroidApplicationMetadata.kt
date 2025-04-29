package org.archivekeep.app.android

import org.archivekeep.app.ui.utils.ApplicationMetadata

class AndroidApplicationMetadata : ApplicationMetadata {
    override val version: String
        get() = BuildConfig.VERSION_NAME
}
