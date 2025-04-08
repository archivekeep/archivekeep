package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.desktop.domain.wiring.LocalFileStores
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import java.io.File

@Composable
actual fun filesystemRepositoryDirectoryPicker(onResult: (result: PickResult) -> Unit): () -> Unit {
    val mountPoints = LocalFileStores.current.mountPoints.collectLoadableFlow()

    val pickerLauncher =
        rememberDirectoryPickerLauncher(
            title = "Pick a directory",
            platformSettings = FileKitPlatformSettings(),
        ) { directory ->
            if (directory == null) {
                onResult(PickResult.Failure(RuntimeException("Directory picker returned null directory")))
            } else {
                val path = directory.path

                if (path == null) {
                    onResult(PickResult.Failure(RuntimeException("Path not present for $directory")))
                } else {
                    onResult(
                        PickResult.Success(
                            getPathFromURI(
                                mountPoints.mapIfLoadedOrDefault(emptyList()) { it },
                                directory.uri,
                            ),
                        ),
                    )
                }
            }
        }

    return pickerLauncher::launch
}

@Composable
actual fun platformSpecificFileSystemRepositoryGuard(): PlatformSpecificPermissionFulfilment = storageManagerGuard()

@Composable
fun storageManagerGuard(): PlatformSpecificPermissionFulfilment {
    var isExternalStorageManager by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        isExternalStorageManager = Environment.isExternalStorageManager()
    }

    if (isExternalStorageManager) {
        return PlatformSpecificPermissionFulfilment.IsFine
    } else {
        val context = LocalContext.current

        return PlatformSpecificPermissionFulfilment.NeedsGrant(
            listOf(
                "Filesystem repository functionality requires permission to manage all files on device for efficient access.",
                "If you want to work with filesystem repositories on this Android device, please grant the permission.",
            ),
            "Go to settings",
            onLaunch = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                isExternalStorageManager = Environment.isExternalStorageManager()
            },
        )
    }
}

fun getPathFromURI(
    mountPoints: List<MountedFileSystem.MountPoint>,
    uri: Uri,
): String {
    if (isExternalStorageDocument(uri)) {
        if (uri.pathSegments.size == 2) {
            if (uri.pathSegments[0] == "tree") {
                val (storageID, path) =
                    uri.pathSegments[1]
                        .split(":".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()

                val mountedPathFromPointPoints =
                    mountPoints.firstNotNullOfOrNull {
                        if (it.fsUUID == storageID) {
                            val f = File(it.mountPath).resolve(path)

                            if (f.exists()) {
                                f
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }

                if (mountedPathFromPointPoints != null) {
                    return mountedPathFromPointPoints.absolutePath
                } else if ("primary".equals(storageID, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}/$path"
                } else {
                    val storageFile = File("/storage/$storageID")
                    if (storageFile.exists() && storageFile.isDirectory) {
                        return "$storageFile/$path"
                    }
                }
            }
        }
    }

    throw RuntimeException("URI $uri not supported")
}

fun isExternalStorageDocument(uri: Uri): Boolean = "com.android.externalstorage.documents" == uri.authority
