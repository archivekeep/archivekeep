package org.archivekeep.files.api.exceptions

data class UnsupportedFeatureException(
    val comment: String = "Unsupported feature",
    val base: Throwable? = null,
) : RuntimeException(base)
