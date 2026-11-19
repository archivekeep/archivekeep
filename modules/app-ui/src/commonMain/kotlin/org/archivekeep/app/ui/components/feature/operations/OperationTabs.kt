package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import org.archivekeep.app.ui.components.designsystem.navigation.TabButton
import org.archivekeep.app.ui.components.designsystem.navigation.TabRow
import org.archivekeep.app.core.procedures.utils.BaseJobState
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_errors
import org.archivekeep.ui.resources.common_log
import org.archivekeep.ui.resources.common_performance
import org.archivekeep.ui.resources.common_summary
import org.jetbrains.compose.resources.stringResource
import kotlin.to

private const val TAB_SUMMARY = "summary"
private const val TAB_LOG = "log"
private const val TAB_ERROR_LOG = "error-log"
private const val TAB_PERFORMANCE = "performance"

@Composable
private fun getTabOptions(
    trackerPresentAndNotEmpty: Boolean,
): List<Pair<String, @Composable () -> String>> = listOfNotNull(
    TAB_SUMMARY to { stringResource(Res.string.common_summary) },
    TAB_LOG to { stringResource(Res.string.common_log) },
    if (trackerPresentAndNotEmpty) TAB_PERFORMANCE to { stringResource(Res.string.common_performance) } else null,
    TAB_ERROR_LOG to { stringResource(Res.string.common_errors) },
)

@Composable
fun <S : BaseJobState> OperationProgressTabs(
    summaryContent: @Composable (state: S) -> Unit,
    state: S,
    errorLog: State<List<String>>? = null,
) {
    var selectedTab by remember { mutableStateOf(TAB_SUMMARY) }

    val statsSnapshot = state.performanceTracker?.statsSnapshot?.collectAsState()
    val tabOptions = getTabOptions(statsSnapshot?.value?.isNotEmpty() == true)
    val validTabOptions = tabOptions.filter { it.first != TAB_ERROR_LOG || errorLog != null }

    TabRow {
        validTabOptions.forEach { tab ->
            TabButton(
                label = tab.second(),
                isSelected = selectedTab == tab.first,
                onClick = { selectedTab = tab.first },
                badge = if (tab.first == TAB_ERROR_LOG && errorLog?.value?.isNotEmpty() == true) {
                    {
                        Text(
                            color = Color.Red,
                            text = errorLog.value.size.toString(),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    null
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

        TAB_PERFORMANCE -> {
            state.performanceTracker?.let { tracker ->
                val stats = tracker.statsSnapshot.collectAsState().value
                PerformanceStatsView(stats)
            }
        }

        TAB_ERROR_LOG -> {
            ScrollableLogTextInDialog(errorLog?.value ?: emptyList())
        }
    }
}
