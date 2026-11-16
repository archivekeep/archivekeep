package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_no_items
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> SectionCardBottomList(
    items: List<T>,
    noItemsText: String = stringResource(Res.string.common_no_items),
    renderItem: @Composable ColumnScope.(item: T) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .background(CColors.cardOtherRposBG)
                .padding(vertical = 2.dp),
    ) {
        if (items.isNotEmpty()) {
            items.map { renderItem(it) }
        } else {
            Text(
                noItemsText,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(1f).padding(horizontal = sectionCardHorizontalPadding, vertical = 6.dp).alpha(0.6f),
            )
        }
    }
}
