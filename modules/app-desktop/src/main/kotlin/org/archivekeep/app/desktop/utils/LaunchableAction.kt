package org.archivekeep.app.desktop.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LaunchableAction(
    val scope: CoroutineScope,
) {
    var runningJob by mutableStateOf<Job?>(null)

    val isRunning
        get() = runningJob != null

    fun launch(fn: suspend () -> Unit) {
        runningJob =
            scope.launch {
                try {
                    fn()
                } finally {
                    runningJob = null
                }
            }
    }
}

@Composable
fun rememberLaunchableAction(scope: CoroutineScope = rememberCoroutineScope()) =
    remember {
        LaunchableAction(
            scope = scope,
        )
    }
