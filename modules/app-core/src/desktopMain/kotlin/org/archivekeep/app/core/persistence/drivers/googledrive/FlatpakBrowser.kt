package org.archivekeep.app.core.persistence.drivers.googledrive

object FlatpakBrowser {
    fun isRunningInFlatpak(): Boolean = System.getenv("FLATPAK_ID") != null

    fun tryOpenUrl(url: String): Boolean {
        if (!isRunningInFlatpak()) {
            return false
        }

        return try {
            val process =
                ProcessBuilder(listOf("xdg-open", url))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()

            val started = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
            if (started) {
                process.exitValue() == 0
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
