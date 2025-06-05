package org.archivekeep.app.ui.dialogs.repository.registry

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
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import java.io.File

@Composable
actual fun platformSpecificFileSystemRepositoryGuard(): PlatformSpecificPermissionFulfilment = storageManagerGuard()

@Composable
fun storageManagerGuard(): PlatformSpecificPermissionFulfilment {
    var isExternalStorageManager by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val previous = isExternalStorageManager
        val new = Environment.isExternalStorageManager()

        if (!previous && new) {
            // TODO: find less intrusive way to apply effect, or at least inform user
            // https://issuetracker.google.com/issues/203692313

            context.startActivity(
                Intent.makeRestartActivityTask(
                    context.packageManager.getLaunchIntentForPackage(context.packageName)!!.component,
                ),
            )

            Runtime.getRuntime().exit(0)
        } else {
            isExternalStorageManager = new
        }
    }

    if (isExternalStorageManager) {
        return PlatformSpecificPermissionFulfilment.IsFine
    } else {
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
): String =
    if (isExternalStorageDocument(uri)) {
        getFromExternalStorage(mountPoints, uri)
    } else {
        throw RuntimeException("URI $uri not supported")
    }

fun getFromExternalStorage(
    mountPoints: List<MountedFileSystem.MountPoint>,
    uri: Uri,
): String =
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
                            throw RuntimeException("No access to path $f in corresponding storage $it: $uri")
                        }
                    } else {
                        null
                    }
                }

            if (mountedPathFromPointPoints != null) {
                mountedPathFromPointPoints.absolutePath
            } else if ("primary".equals(storageID, ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory()}/$path"
            } else {
                val storageFile = File("/storage/$storageID")
                if (storageFile.exists() && storageFile.isDirectory) {
                    "$storageFile/$path"
                } else {
                    throw RuntimeException("Path $storageFile doesn't exist: $uri")
                }
            }
        } else {
            throw RuntimeException("Invalid external storage URI: $uri")
        }
    } else {
        throw RuntimeException("Invalid external storage URI: $uri")
    }

fun isExternalStorageDocument(uri: Uri): Boolean = "com.android.externalstorage.documents" == uri.authority
