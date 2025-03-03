package org.archivekeep.app.desktop.ui.utils

import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun (Builder).appendBoldSpan(value: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(value)
    }
}
