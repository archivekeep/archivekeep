package org.archivekeep.files.utils

import kotlinx.coroutines.CoroutineScope

interface GenericTestScope : CoroutineScope {
    val backgroundScope: CoroutineScope
}
