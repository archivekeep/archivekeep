package org.archivekeep.app.ui.domain.wiring

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

@OptIn(DelicateCoroutinesApi::class)
fun newServiceWorkExecutorDispatcher() =
    newFixedThreadPoolContext(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
        "Application Services",
    )
