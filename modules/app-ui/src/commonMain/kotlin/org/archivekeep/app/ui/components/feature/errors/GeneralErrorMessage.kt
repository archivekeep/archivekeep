package org.archivekeep.app.ui.components.feature.errors

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GeneralErrorMessage(cause: Throwable) {
    SelectionContainer {
        Column {
            Text(cause.toString())

            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .background(Color.Gray.copy(alpha = 0.1f))
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    remember(cause) {
                        buildAnnotatedString {
                            append(cause.stackTraceToString())
                        }
                    },
                    fontSize = 12.sp,
                )
            }
        }
    }
}
