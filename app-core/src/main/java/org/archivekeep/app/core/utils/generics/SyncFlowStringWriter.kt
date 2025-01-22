package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import java.io.OutputStream
import java.io.PrintWriter

class SyncFlowStringWriter {
    private var mutableString = MutableStateFlow("")

    val string = mutableString.asStateFlow()

    val writer: PrintWriter =
        run {
            val os =
                object : OutputStream() {
                    override fun write(p0: Int) {
                        mutableString.getAndUpdate { v -> v + Char(p0) }
                    }

                    override fun write(b: ByteArray) {
                        mutableString.getAndUpdate { v -> v + String(b) }
                    }

                    override fun write(
                        b: ByteArray,
                        off: Int,
                        len: Int,
                    ) {
                        mutableString.getAndUpdate { v -> v + String(b, off, len) }
                    }
                }

            PrintWriter(os)
        }
}
