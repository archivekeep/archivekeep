package org.archivekeep.files.performance

import kotlin.time.Duration

data class OperationStats(
    val operationId: String,
    val count: Long,
    val total: Duration,
    val average: Duration,
)
