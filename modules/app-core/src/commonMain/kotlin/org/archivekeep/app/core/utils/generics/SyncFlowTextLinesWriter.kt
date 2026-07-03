package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import java.io.OutputStream
import java.io.PrintWriter

class SyncFlowTextLinesWriter {
    private var mutableTextLines = MutableStateFlow(emptyList<String>())

    val lines = mutableTextLines.asStateFlow()

    val writer: PrintWriter =
        run {
            val os =
                object : OutputStream() {
                    override fun write(p0: Int) {
                        addString(String(charArrayOf(p0.toChar())))
                    }

                    override fun write(b: ByteArray) {
                        addString(String(b))
                    }

                    override fun write(
                        b: ByteArray,
                        off: Int,
                        len: Int,
                    ) {
                        addString(String(b, off, len))
                    }
                }

            PrintWriter(os)
        }

    private fun addString(linesString: String) =
        mutableTextLines.getAndUpdate { v ->
            val lines = linesString.lines()

            if (v.isEmpty()) {
                lines
            } else {
                v.subList(0, v.size - 1) + listOf(v.last() + lines.first()) + lines.subList(1, lines.size)
            }
        }
}
