package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.GridScope
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun SectionCardGrid(content: @Composable GridScope.() -> Unit) {
    VerticalGrid(
        columns = SimpleGridCells.Adaptive(minSize = 250.dp),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingHorizontal),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingVertical),
        content = content,
    )
}
