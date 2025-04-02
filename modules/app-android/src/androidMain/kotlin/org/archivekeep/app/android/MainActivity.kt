package org.archivekeep.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.domain.wiring.ApplicationProviders
import org.archivekeep.app.desktop.ui.MainWindowContent

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val environment: (scope: CoroutineScope) -> Environment =
                remember {
                    { scope -> DemoEnvironment(scope + Dispatchers.Default) }
                }

            ApplicationProviders(environment) {
                MainWindowContent(
                    isFloating = false,
                    windowSizeClass = calculateWindowSizeClass(this),
                    onCloseRequest = { this@MainActivity.onBackPressed() },
                )
            }
        }
    }
}
