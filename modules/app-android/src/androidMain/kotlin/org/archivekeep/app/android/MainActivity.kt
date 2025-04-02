package org.archivekeep.app.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import org.archivekeep.app.desktop.domain.wiring.ApplicationProviders
import org.archivekeep.app.desktop.ui.MainWindowContent

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ApplicationProviders(
                (this.application as MainApplication).services,
            ) {
                MainWindowContent(
                    isFloating = false,
                    windowSizeClass = calculateWindowSizeClass(this),
                    onCloseRequest = { this@MainActivity.onBackPressed() },
                )
            }
        }
    }
}
