package org.archivekeep.app.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import org.archivekeep.app.android.components.StatusBarProtection
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            SystemBarStyle.dark(Color.TRANSPARENT),
            SystemBarStyle.dark(Color.argb(40, 0, 0, 0)),
        )

        val applicationMetadata = AndroidApplicationMetadata()

        setContent {
            ApplicationProviders(
                (this.application as MainApplication).services,
                applicationMetadata,
            ) {
                val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

                AppTheme(
                    small = windowSizeClass.widthSizeClass < WindowWidthSizeClass.Expanded,
                ) {
                    MainWindowContent(
                        isFloating = false,
                        windowSizeClass = windowSizeClass,
                        onCloseRequest = null,
                    )
                }
            }

            StatusBarProtection()
        }
    }
}
