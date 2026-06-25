package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import org.archivekeep.app.ui.domain.services.RepositoryOpenService

val LocalApplicationServices = staticCompositionLocalOfNotProvided<ApplicationServices>()

val LocalRepositoryOpenService = staticCompositionLocalOfNotProvided<RepositoryOpenService>()

inline fun <reified T> staticCompositionLocalOfNotProvided() =
    staticCompositionLocalOf<T> {
        error("CompositionLocal ${T::class.qualifiedName} not provided")
    }
