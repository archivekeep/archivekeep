package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

val defaultDialogWidthModifier =
    Modifier
        .widthIn(min = 120.dp, max = 500.dp)
        .defaultMinSize(minWidth = 240.dp, minHeight = 5.dp)

val fullWidthDialogWidthModifier =
    Modifier
        .defaultMinSize(minWidth = 300.dp, minHeight = 5.dp)
        .width(IntrinsicSize.Max)

@Composable
fun DialogInnerContainer(
    title: AnnotatedString,
    widthModifier: Modifier = defaultDialogWidthModifier,
    content: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(DialogContentPadding)
                .then(widthModifier),
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
