package org.archivekeep.utils

fun filesAutoPlural(
    items: Collection<*>,
    prefix: String = "",
): String = filesAutoPlural(items.size, prefix)

fun filesAutoPlural(
    count: Int,
    prefix: String = "",
): String =
    if (count == 1) {
        "1 ${prefix}file"
    } else {
        "$count ${prefix}files"
    }
