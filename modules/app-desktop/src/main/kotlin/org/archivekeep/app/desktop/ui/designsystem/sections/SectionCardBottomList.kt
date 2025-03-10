package org.archivekeep.app.desktop.ui.designsystem.sections

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
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors

@Composable
fun <T> SectionCardBottomList(
    items: List<T>,
    noItemsText: String = "No items",
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
