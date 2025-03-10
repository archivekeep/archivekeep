package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogContentContainer(
    title: AnnotatedString,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.padding(24.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Column(
            Modifier.padding(12.dp),
        ) {
            Text(text = title, fontSize = 18.sp, lineHeight = 18.sp)

            HorizontalDivider(Modifier.padding(0.dp, 4.dp, 0.dp, 8.dp))

            content()
        }
    }
}

@Preview
@Composable
private fun DialogContentContainerPreview() {
    Box(
        Modifier.fillMaxSize().background(Color.DarkGray),
        contentAlignment = Alignment.Center,
    ) {
        DialogContentContainer(
            title =
                buildAnnotatedString {
                    append("Dialog for ")

                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("DEMO")
                    }

                    append(" presentation")
                },
        ) {
            Text("Just DEMO")
        }
    }
}
