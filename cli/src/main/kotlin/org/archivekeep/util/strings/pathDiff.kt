package org.archivekeep.util.strings

fun pathDiff(
    old: String,
    new: String,
    colorFunc: (s: String) -> String,
): String {
    val (oldDir, oldName) = splitDirName(old)
    val (newDir, newName) = splitDirName(new)

    val dirDiffPart: String =
        if (oldDir != "" || newDir != "") {
            if (oldDir == newDir) {
                oldDir
            } else {
                val dirLCP = oldDir.commonPrefixWith(newDir)
                val remainingOldDir = oldDir.removePrefix(dirLCP)
                val remainingNewDir = newDir.removePrefix(dirLCP)

                val dirLCS = remainingOldDir.commonSuffixWith(remainingNewDir)

                val diffMiddlePart = "{${remainingOldDir.removeSuffix(dirLCS)} -> ${remainingNewDir.removeSuffix(dirLCS)}}"

                "${dirLCP}${colorFunc(diffMiddlePart)}$dirLCS"
            }
        } else {
            ""
        }

    return if (oldName == newName) {
        "${dirDiffPart}$oldName"
    } else {
        if (dirDiffPart == "") {
            colorFunc("$oldName -> $newName")
        } else {
            "${dirDiffPart}${colorFunc("{$oldName -> $newName}")}"
        }
    }
}

fun splitDirName(fileName: String): Pair<String, String> {
    val idx = fileName.lastIndexOf("/")

    return if (idx == -1) {
        Pair("", fileName)
    } else {
        Pair(fileName.substring(0, idx + 1), fileName.substring(idx + 1))
    }
}
