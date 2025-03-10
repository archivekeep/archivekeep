package org.archivekeep.files.exceptions

data class UnsupportedFeatureException(
    val comment: String = "Unsupported feature",
    val base: Throwable? = null,
) : RuntimeException(base)
