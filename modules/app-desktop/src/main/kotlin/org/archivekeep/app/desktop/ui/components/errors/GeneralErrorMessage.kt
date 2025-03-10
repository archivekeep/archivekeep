package org.archivekeep.app.desktop.ui.components.errors

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeneralErrorMessage(cause: Throwable) {
    Text(cause.toString())

    Spacer(Modifier.height(8.dp))

    Text(
        remember(cause) {
            cause.stackTraceToString()
        },
        softWrap = true,
        fontSize = 10.sp,
    )
}
