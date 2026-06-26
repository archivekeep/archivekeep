package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf

val LocalApplicationServices = staticCompositionLocalOfNotProvided<ApplicationServices>()

inline fun <reified T> staticCompositionLocalOfNotProvided() =
    staticCompositionLocalOf<T> {
        error("CompositionLocal ${T::class.qualifiedName} not provided")
    }
