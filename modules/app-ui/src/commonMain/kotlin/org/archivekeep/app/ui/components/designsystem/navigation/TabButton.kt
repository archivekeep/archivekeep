package org.archivekeep.app.ui.components.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TabShape = RoundedCornerShape(8.dp)
private val TabPadding = 12.dp
private val TabHeight = 36.dp

@Composable
fun TabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    badge: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .height(TabHeight)
                .wrapContentWidth(unbounded = true)
                .background(if (isSelected) { selectedBackgroundColor } else { Color.Transparent }, TabShape)
                .then(
                    if (isSelected) {
                        Modifier.padding(horizontal = TabPadding, vertical = 4.dp)
                    } else {
                        Modifier.padding(horizontal = 0.dp, vertical = 4.dp)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                )
                .semantics {
                    role = Role.Tab
                    selected = isSelected
                },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                color = if (isSelected) { selectedTextColor } else { MaterialTheme.colorScheme.onSurfaceVariant },
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 0.75.sp,
            )
            badge?.let { badgeContent ->
                Spacer(Modifier.width(8.dp))
                badgeContent()
            }
        }
    }
}
