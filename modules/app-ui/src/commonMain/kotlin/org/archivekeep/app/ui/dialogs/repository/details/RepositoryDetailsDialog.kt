package org.archivekeep.app.ui.dialogs.repository.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

class RepositoryDetailsDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<RepositoryDetailsDialog.State, RepositoryDetailsDialog.VM>(uri) {
    class VM(
        val repository: Repository,
        val uri: RepositoryURI,
        val _onClose: () -> Unit,
    ) : IVM {
        override fun onClose() {
            _onClose()
        }
    }

    data class State(
        val displayName: String,
        val repositoryType: String,
        val fileCount: Int?,
        val totalSize: Long?,
        val filesystemRepoType: String?,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Repository details")
            }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): VM =
        remember {
            VM(repository, uri, onClose)
        }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> {
        val index by vm.repository.indexFlowWithCaching.collectAsState()
        val metadata by vm.repository.metadataFlowWithCaching.collectAsState()
        val information = vm.repository.informationFlow.collectAsLoadable()

        return remember(index, metadata, information) {
            val files = (index as? OptionalLoadable.LoadedAvailable<RepoIndex>)?.value?.files ?: emptyList()
            val repositoryType = vm.uri.driver

            val totalSize = files.sumOf { it.size ?: 0L }

            val accessor = (vm.repository.optionalAccessorFlow.value as? OptionalLoadable.LoadedAvailable<Repo>)?.value
            val filesystemRepoType = accessor?.let { getFilesystemRepoType(it) }

            State(
                displayName = (information as? Loadable.Loaded)?.value?.displayName ?: vm.uri.data,
                repositoryType = repositoryType,
                fileCount = files.size,
                totalSize = totalSize,
                filesystemRepoType = filesystemRepoType,
                onClose = vm::onClose,
            )
        }.let { Loadable.Loaded(it) }
    }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoRow("Name", state.displayName)
            InfoRow("Type", state.repositoryType)
            state.filesystemRepoType?.let {
                InfoRow("Storage", it)
            }
            InfoRow("Files", state.fileCount?.toString() ?: "Unknown")
            InfoRow(
                "Size",
                state.totalSize?.let { formatBytes(it) } ?: "Unknown",
            )
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogButtonContainer {
            DialogDismissButton(
                "Close",
                onClick = state.onClose,
            )
        }
    }

    @Composable
    private fun InfoRow(
        label: String,
        value: String,
    ) {
        Column {
            LabelText(label)
            Text(value)
        }
    }
}

private fun getFilesystemRepoType(repo: Repo): String? {
    val className = repo::class.simpleName ?: return null

    return when {
        className.contains("Encrypted") -> "Encrypted files"
        className.contains("RocksDb") -> "RocksDB index"
        className.contains("Sqlite") -> "SQLite index"
        className.contains("FilesRepo") -> "Plain files with checksum files"
        else -> null
    }
}

private fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_000_000_000_000 -> {
            String.format("%.1f TB", bytes / 1_000_000_000_000.0)
        }

        bytes >= 1_000_000_000 -> {
            String.format("%.1f GB", bytes / 1_000_000_000.0)
        }

        bytes >= 1_000_000 -> {
            String.format("%.1f MB", bytes / 1_000_000.0)
        }

        bytes >= 1_000 -> {
            String.format("%.1f KB", bytes / 1_000.0)
        }

        else -> {
            "$bytes B"
        }
    }
