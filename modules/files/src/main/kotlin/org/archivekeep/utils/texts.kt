package org.archivekeep.utils

fun filesAutoPlural(items: Collection<*>): String =
    if (items.size == 1) {
        "file"
    } else {
        "files"
    }
