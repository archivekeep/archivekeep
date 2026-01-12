package org.archivekeep.utils.io

import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.deleteIfExists

class AutomaticFileCleanup(
    val files: MutableList<Path> = mutableListOf(),
) {
    init {
        add(this)
    }

    fun cancel() {
        remove(this)
        files.clear()
    }

    private fun run() {
        files.forEach { file ->
            file.deleteIfExists()
        }
    }

    fun runPremature() {
        run()
        remove(this)
    }

    companion object {
        val lock = ReentrantLock()
        var registeredCleanups = mutableSetOf<AutomaticFileCleanup>()

        init {
            val cleanupThread =
                Thread {
                    registeredCleanups.forEach { entry ->
                        entry.run()
                    }
                }

            Runtime.getRuntime().addShutdownHook(cleanupThread)
        }

        private fun add(entry: AutomaticFileCleanup) {
            lock.withLock {
                registeredCleanups.add(entry)
            }
        }

        private fun remove(entry: AutomaticFileCleanup) {
            lock.withLock {
                registeredCleanups.remove(entry)
            }
        }
    }
}
