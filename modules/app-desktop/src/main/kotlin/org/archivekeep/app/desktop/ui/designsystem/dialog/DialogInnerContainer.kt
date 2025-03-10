package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun DialogInnerContainer(
    title: AnnotatedString,
    content: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(DialogContentPadding)
                .width(IntrinsicSize.Min)
                .defaultMinSize(minWidth = 500.dp, minHeight = 5.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                title,
                modifier = Modifier.padding(top = DialogTitleTopPadding, bottom = DialogTitleContentSpacing),
                style = DialogTitleStyle,
            )

            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                content()
            }
        }

        bottomContent()
    }
}
