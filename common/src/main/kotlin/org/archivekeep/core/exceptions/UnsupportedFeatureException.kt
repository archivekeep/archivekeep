package org.archivekeep.core.exceptions

data class UnsupportedFeatureException(
    val comment: String = "Unsupported feature",
    val base: Throwable? = null,
) : RuntimeException(base)
