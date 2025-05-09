package org.archivekeep.app.ui.components.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAppDimens =
    staticCompositionLocalOf {
        largeDimensions
    }

@Composable
fun AppTheme(
    small: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val dimensions = if (small) smallDimensions else largeDimensions

    CompositionLocalProvider(
        LocalAppDimens provides dimensions,
        LocalMinimumInteractiveComponentSize provides 20.dp,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            content = content,
        )
    }
}

object AppTheme {
    val dimens: AppDimensions
        @Composable
        get() = LocalAppDimens.current
}
