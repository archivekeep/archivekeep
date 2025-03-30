package org.archivekeep.utils

fun filesAutoPlural(items: Collection<*>): String = filesAutoPlural(items.size)

fun filesAutoPlural(count: Int): String =
    if (count == 1) {
        "file"
    } else {
        "files"
    }
