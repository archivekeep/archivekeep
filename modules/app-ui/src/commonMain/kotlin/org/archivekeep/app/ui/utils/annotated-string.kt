package org.archivekeep.app.ui.utils

import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

fun (Builder).appendBoldSpan(value: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(value)
    }
}

fun (Builder).appendBoldStrikeThroughSpan(value: String) {
    withStyle(
        SpanStyle(
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.LineThrough,
        ),
    ) {
        append(value)
    }
}
