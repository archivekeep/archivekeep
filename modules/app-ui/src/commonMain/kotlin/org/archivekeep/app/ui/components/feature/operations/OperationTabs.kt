package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.procedures.utils.BaseJobState
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent

private const val TAB_SUMMARY = "summary"
private const val TAB_LOG = "log"
private const val TAB_ERROR_LOG = "error-log"

private val tabOptions =
    listOf(
        TAB_SUMMARY to "Summary",
        TAB_LOG to "Log",
        TAB_ERROR_LOG to "Errors",
    )

@Composable
fun <S : BaseJobState> OperationProgressTabs(
    summaryContent: @Composable (state: S) -> Unit,
    state: S,
    errorLog: State<List<String>>? = null,
) {
    var selectedTab by remember { mutableStateOf(TAB_SUMMARY) }

    val validTabOptions = tabOptions.filter { it.first != TAB_ERROR_LOG || errorLog != null }

    SingleChoiceSegmentedButtonRow {
        validTabOptions.forEachIndexed { index, tab ->
            SegmentedButton(
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = validTabOptions.size,
                    ),
                onClick = { selectedTab = tab.first },
                selected = selectedTab == tab.first,
                label = {
                    Row {
                        Text(tab.second)

                        if (tab.first == TAB_ERROR_LOG && errorLog?.value?.isNotEmpty() == true) {
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Red,
                                text = errorLog.value.size.toString(),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    when (selectedTab) {
        TAB_SUMMARY -> {
            summaryContent(state)
            ExecutionErrorIfPresent(state.executionState)
        }

        TAB_LOG -> {
            ScrollableLogTextInDialog(state.progressLog.collectAsState().value)
        }

        TAB_ERROR_LOG -> {
            ScrollableLogTextInDialog(errorLog?.value ?: emptyList())
        }
    }
}
