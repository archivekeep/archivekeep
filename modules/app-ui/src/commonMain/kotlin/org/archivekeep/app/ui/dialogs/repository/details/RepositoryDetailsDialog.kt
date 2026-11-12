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
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_close
import org.archivekeep.ui.resources.common_unknown
import org.archivekeep.ui.resources.dialog_repo_details_files
import org.archivekeep.ui.resources.dialog_repo_details_name
import org.archivekeep.ui.resources.dialog_repo_details_size
import org.archivekeep.ui.resources.dialog_repo_details_storage
import org.archivekeep.ui.resources.dialog_repo_details_type
import org.archivekeep.ui.resources.dialog_repository_details_title
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

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
    )

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            append(stringResource(Res.string.dialog_repository_details_title))
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
            val filesystemRepoType = accessor?.let { getFilesystemRepoTypeKey(it) }

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
            InfoRow(stringResource(Res.string.dialog_repo_details_name), state.displayName)
            InfoRow(stringResource(Res.string.dialog_repo_details_type), state.repositoryType)
            state.filesystemRepoType?.let {
                InfoRow(stringResource(Res.string.dialog_repo_details_storage), it)
            }
            InfoRow(stringResource(Res.string.dialog_repo_details_files), state.fileCount?.toString() ?: stringResource(Res.string.common_unknown))
            InfoRow(
                stringResource(Res.string.dialog_repo_details_size),
                state.totalSize?.let { formatBytes(it) } ?: stringResource(Res.string.common_unknown),
            )
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogButtonContainer {
            DialogDismissButton(
                stringResource(Res.string.common_close),
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

private fun getFilesystemRepoTypeKey(repo: Repo): String? {
    val className = repo::class.simpleName ?: return null

    return when {
        className.contains("Encrypted") -> "encrypted"
        className.contains("RocksDb") -> "rocksdb"
        className.contains("Sqlite") -> "sqlite"
        className.contains("FilesRepo") -> "plain"
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
