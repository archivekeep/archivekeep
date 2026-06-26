package org.archivekeep.app.android

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.archivekeep.app.ui.utils.ApplicationMetadata

@Inject
@SingleIn(AppScope::class)
class AndroidApplicationMetadata : ApplicationMetadata {
    override val version: String
        get() = BuildConfig.VERSION_NAME
}
