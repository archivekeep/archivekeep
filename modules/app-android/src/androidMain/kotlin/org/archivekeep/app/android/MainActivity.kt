package org.archivekeep.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.domain.wiring.ApplicationProviders
import org.archivekeep.app.desktop.ui.MainWindowContent

class MainActivity : AppCompatActivity() {
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
                    onCloseRequest = { this@MainActivity.onBackPressed() },
                )
            }
        }
    }
}
