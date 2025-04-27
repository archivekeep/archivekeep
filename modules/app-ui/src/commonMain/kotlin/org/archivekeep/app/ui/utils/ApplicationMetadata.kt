package org.archivekeep.app.ui.utils

import java.io.InputStream
import java.util.Properties

class ApplicationMetadata {
    companion object {
        val version by lazy {
            val properties =
                Properties().also {
                    val file: InputStream? = this::class.java.classLoader.getResourceAsStream("org/archivekeep/app/desktop/application.properties")

                    if (file != null) {
                        it.load(file)
                    } else {
                        it.setProperty("version", "failed to load")
                    }
                }

            properties["version"].toString()
        }
    }
}
