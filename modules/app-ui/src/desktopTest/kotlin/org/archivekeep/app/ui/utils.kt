package org.archivekeep.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun SemanticsNodeInteraction.performClickTextInput(text: String) {
    performClick()
    performTextInput(text)
}
