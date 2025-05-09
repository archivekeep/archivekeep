package org.archivekeep.app.ui.utils

import java.io.InputStream
import java.util.Properties

class PropertiesApplicationMetadata(
    val propertiesFileName: String = "org/archivekeep/app/core/application.properties",
) : ApplicationMetadata {
    override val version by lazy {
        val properties =
            Properties().also {
                val file: InputStream? = this::class.java.classLoader.getResourceAsStream(propertiesFileName)

                if (file != null) {
                    it.load(file)
                } else {
                    it.setProperty("version", "failed to load")
                }
            }

        properties["version"].toString()
    }
}
