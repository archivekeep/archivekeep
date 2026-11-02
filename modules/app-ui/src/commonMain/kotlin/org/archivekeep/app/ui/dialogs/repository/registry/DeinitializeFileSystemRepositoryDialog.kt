package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize.DeinitializeFileSystemRepositoryByPathUseCase
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize.DeinitializeFileSystemRepositoryByUriUseCase
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize.DeinitializeFileSystemRepositoryPreparation
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCard
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.utils.SingleLaunchGuard
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.utils.loading.Loadable

abstract class DeinitializeFileSystemRepositoryDialog(
    protected open val repositoryURI: RepositoryURI?,
) : Dialog {
    protected abstract suspend fun prepare(
        scope: CoroutineScope,
        operationFactory1: OperationFactory,
    ): DeinitializeFileSystemRepositoryPreparation

    inner class VM(
        val coroutineScope: CoroutineScope,
        val operationFactory: OperationFactory,
    ) : RememberObserver {
        var metadataDestructionConfirmed by mutableStateOf(false)

        private var preparationScope: CoroutineScope = coroutineScope + SupervisorJob()

        var deinitializePreparation by mutableStateOf<Loadable<DeinitializeFileSystemRepositoryPreparation>>(Loadable.Loading)
            private set

        val deinitializeLaunchGuard = SingleLaunchGuard(coroutineScope)

        init {
            preparationScope.launch {
                try {
                    val newOperation = this@DeinitializeFileSystemRepositoryDialog.prepare(preparationScope, operationFactory)
                    ensureActive()
                    deinitializePreparation = Loadable.Loaded(newOperation)
                } catch (e: Throwable) {
                    ensureActive()
                    deinitializePreparation = Loadable.Failed(e)
                }
            }
        }

        override fun onAbandoned() {
            preparationScope.cancel()
        }

        override fun onForgotten() {
            preparationScope.cancel()
        }

        override fun onRemembered() {}
    }

    @Composable
    override fun render(onClose: () -> Unit) {
        val operationFactory = LocalApplicationServices.current.operationFactory
        val coroutineScope = rememberCoroutineScope()

        val vm =
            remember(coroutineScope, operationFactory) {
                VM(coroutineScope, operationFactory)
            }

        DialogOverlay(onDismissRequest = onClose) {
            DialogCard {
                DialogInnerContainer(
                    remember {
                        buildAnnotatedString { append("Deinitialize filesystem repository") }
                    },
                    content = {
                        LoadableGuard(vm.deinitializePreparation) { preparation ->
                            val repoPath =
                                when (preparation) {
                                    is DeinitializeFileSystemRepositoryPreparation.PlainFileSystemRepository -> preparation.path
                                    is DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository -> ""
                                }
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    remember(repoPath) {
                                        buildAnnotatedString {
                                            append("Repository stored in ")
                                            appendBoldSpan(repoPath)
                                            append(" will be deinitialized.")
                                        }
                                    },
                                )
                                WarningAlert {
                                    Text("All archive metadata will be destroyed.")
                                }
                                Text("Data will not be deleted.")
                                Text(
                                    remember(repoPath) {
                                        buildAnnotatedString {
                                            append("Metadata stored in ")
                                            appendBoldSpan(repoPath + "/.archive")
                                            append(" will be ")
                                            appendBoldSpan("completely deleted")
                                            append(".")
                                        }
                                    },
                                )
                                Text("To use it against as an archive repository, reinitialization and reindexing will be needed.")
                                CheckboxWithText(
                                    vm.metadataDestructionConfirmed,
                                    text = "Yes, destroy metadata",
                                    onValueChange = { vm.metadataDestructionConfirmed = it },
                                    enabled = vm.deinitializeLaunchGuard.state == null,
                                )
                                ExecutionErrorIfPresent(vm.deinitializeLaunchGuard.executionOutcome.value)
                                if (vm.deinitializeLaunchGuard.executionOutcome.value is ExecutionOutcome.Success) {
                                    Text("Successfully deinitialized", modifier = Modifier.padding(top = 10.dp))
                                }
                            }
                        }
                    },
                    bottomContent = {
                        DialogButtonContainer {
                            when (val op = vm.deinitializePreparation) {
                                is Loadable.Failed, Loadable.Loading -> {
                                    SimpleActionDialogControlButtons(
                                        "Deinitialize",
                                        onLaunch = {},
                                        onClose = onClose,
                                        canLaunch = false,
                                    )
                                }

                                is Loadable.Loaded<*> -> {
                                    when (val option = op.value) {
                                        is DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository,
                                        -> {
                                            SimpleActionDialogControlButtons(
                                                "Deinitialize",
                                                onLaunch = {},
                                                onClose = onClose,
                                                canLaunch = false,
                                            )
                                        }

                                        is DeinitializeFileSystemRepositoryPreparation.PlainFileSystemRepository -> {
                                            val executionOutcome =
                                                vm.deinitializeLaunchGuard.executionOutcome.value

                                            if (executionOutcome is ExecutionOutcome.Success) {
                                                SimpleActionDialogDoneButtons(onClose)
                                            } else {
                                                SimpleActionDialogControlButtons(
                                                    "Deinitialize",
                                                    onLaunch = {
                                                        vm.deinitializeLaunchGuard.launch {
                                                            option.runDeinitialize()
                                                        }
                                                    },
                                                    onClose = onClose,
                                                    canLaunch =
                                                        vm.metadataDestructionConfirmed && vm.deinitializeLaunchGuard.state !is SingleLaunchGuard.State.Running,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

class DeinitializeFileSystemRepositoryByPathDialog(
    val path: String,
) : DeinitializeFileSystemRepositoryDialog(repositoryURI = null) {
    override suspend fun prepare(
        scope: CoroutineScope,
        operationFactory: OperationFactory,
    ): DeinitializeFileSystemRepositoryPreparation =
        operationFactory
            .get(DeinitializeFileSystemRepositoryByPathUseCase::class.java)
            .prepare(scope, path)
}

class DeinitializeFileSystemRepositoryByUriDialog(
    override val repositoryURI: RepositoryURI,
) : DeinitializeFileSystemRepositoryDialog(repositoryURI = repositoryURI) {
    override suspend fun prepare(
        scope: CoroutineScope,
        operationFactory: OperationFactory,
    ): DeinitializeFileSystemRepositoryPreparation =
        operationFactory
            .get(DeinitializeFileSystemRepositoryByUriUseCase::class.java)
            .prepare(scope, repositoryURI)
}
