package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.files.performance.OperationStats
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_performance
import org.archivekeep.ui.resources.common_performance_avg
import org.archivekeep.ui.resources.common_performance_count
import org.archivekeep.ui.resources.common_performance_no_stats
import org.archivekeep.ui.resources.common_performance_total
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

@Composable
fun PerformanceStatsView(stats: Map<String, OperationStats>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(0.05f),
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 10.dp, )) {
            LabelText(stringResource(Res.string.common_performance))
            Spacer(Modifier.height(4.dp))

            if (stats.isEmpty()) {
                Text(stringResource(Res.string.common_performance_no_stats), style = MaterialTheme.typography.bodyMedium)
            } else {
                ScrollableLazyColumnWithGuessedWidth(400.dp) {
                    items(stats.values.toList()) { stat ->
                        PerformanceStatRow(stat)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceStatRow(stat: OperationStats) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = stat.operationId,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${stringResource(Res.string.common_performance_count)}: ${stat.count}", style = MaterialTheme.typography.bodySmall)
            Text("${stringResource(Res.string.common_performance_total)}: ${formatDuration(stat.total)}", style = MaterialTheme.typography.bodySmall)
            Text("${stringResource(Res.string.common_performance_avg)}: ${formatDuration(stat.average)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDuration(duration: Duration): String {
    return when {
        duration.inWholeMilliseconds >= 1000 -> {
            val totalMs = duration.inWholeMilliseconds
            val wholeSeconds = totalMs / 1000
            val remainderMs = totalMs % 1000
            val tenths = (remainderMs + 50) / 100
            if (tenths == 10L) {
                "${wholeSeconds + 1}.0s"
            } else {
                "${wholeSeconds}.${tenths}s"
            }
        }
        duration.inWholeMilliseconds > 0 -> {
            val totalMs = duration.inWholeMilliseconds
            val micros = duration.inWholeMicroseconds % 1000
            val tenths = (micros + 500) / 1000
            if (tenths == 10L) {
                "${totalMs + 1}.0ms"
            } else {
                "${totalMs}.${tenths}ms"
            }
        }
        duration.inWholeMicroseconds > 0 -> {
            "${duration.inWholeMicroseconds}µs"
        }
        else -> "0µs"
    }
}
