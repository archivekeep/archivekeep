package org.archivekeep.core.repo

import java.nio.file.Path

interface Repo {
    fun contains(filename: String): Boolean
}

interface LocalRepo: Repo {
    fun findAllFiles(globs: List<String>): List<Path>
}

