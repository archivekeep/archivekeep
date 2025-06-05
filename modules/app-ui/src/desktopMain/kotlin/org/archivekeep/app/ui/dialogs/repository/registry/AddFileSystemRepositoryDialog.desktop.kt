package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.Composable

@Composable
actual fun platformSpecificFileSystemRepositoryGuard(): PlatformSpecificPermissionFulfilment = PlatformSpecificPermissionFulfilment.IsFine
