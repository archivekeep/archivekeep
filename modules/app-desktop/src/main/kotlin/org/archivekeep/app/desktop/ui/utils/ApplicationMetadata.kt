package org.archivekeep.app.desktop.ui.utils

import java.util.Properties

class ApplicationMetadata {
    companion object {
        val version by lazy {
            val properties =
                Properties().also {
                    val file = this::class.java.classLoader.getResourceAsStream("org/archivekeep/app/desktop/application.properties")
                    it.load(file)
                }

            properties["version"].toString()
        }
    }
}
