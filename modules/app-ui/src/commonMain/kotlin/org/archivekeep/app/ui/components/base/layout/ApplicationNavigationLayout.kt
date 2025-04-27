package org.archivekeep.app.ui.components.base.layout

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

enum class ApplicationNavigationLayout {
    TOP_AND_BOTTOM,
    TOP_BAR_EMBEDDED_NAVIGATION,
    TOP_BAR_AND_DRAWER,
    RAIL_BAR,
}

fun (WindowSizeClass).calculateNavigationLocation() =
    when {
        widthSizeClass == WindowWidthSizeClass.Compact && heightSizeClass == WindowHeightSizeClass.Compact ->
            ApplicationNavigationLayout.TOP_BAR_AND_DRAWER

        widthSizeClass == WindowWidthSizeClass.Compact ->
            ApplicationNavigationLayout.TOP_AND_BOTTOM

        heightSizeClass == WindowHeightSizeClass.Compact ->
            ApplicationNavigationLayout.RAIL_BAR

        else ->
            ApplicationNavigationLayout.TOP_BAR_EMBEDDED_NAVIGATION
    }
