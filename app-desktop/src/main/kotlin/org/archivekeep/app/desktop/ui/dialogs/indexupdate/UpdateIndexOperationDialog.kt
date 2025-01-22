package org.archivekeep.app.desktop.ui.dialogs.indexupdate

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogContentContainer
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.core.operations.AddOperation
import org.archivekeep.core.operations.AddOperationTextWriter
import org.archivekeep.utils.Loadable
import java.io.PrintWriter
import java.io.StringWriter

class UpdateIndexOperationDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val repository =
            with(LocalRepoService.current) {
                remember(repositoryURI) {
                    this.getRepository(repositoryURI).informationFlow
                }
            }.collectAsLoadable()

        val repositoryAccess =
            with(LocalRepoService.current) {
                remember(repositoryURI) {
                    getRepository(repositoryURI).accessorFlow
                }
            }

        val scope = rememberCoroutineScope()

        if (repositoryAccess == null) {
            Text("Archive with ID $repositoryURI not found")
            return
        }

        when (repository) {
            is Loadable.Loading -> {
                Text("Loading")
                return
            }

            is Loadable.Failed -> {
                Text("Failed")
                return
            }

            is Loadable.Loaded -> {
                val preparationResult =
                    produceState<AddOperation.PreparationResult?>(initialValue = null, repositoryURI) {
//                    val preparationResult = withContext(Dispatchers.IO) {
//                        AddOperation(
//                            subsetGlobs = listOf("."),
//
//                            disableFilenameCheck = false,
//                            disableMovesCheck = false
//                        ).prepare(repositoryAccess)
//                    }
//
//                    value = preparationResult
                    }

                var executeJob: Job? by remember { mutableStateOf(null) }

                val executeResult = remember { SyncFlowStringWriter() }
                val executeResultString = executeResult.string.collectAsState().value

                val onTriggerExecute =
                    remember {
                        {
                            val preparedResult = preparationResult.value
                            val writter = AddOperationTextWriter(PrintWriter(executeResult.writer, true))

                            if (preparedResult != null) {
                                executeJob =
                                    scope.launch {
//                            try {
//                                preparedResult.executeMovesReindex(
//                                    repositoryAccess,
//                                    writter::onMoveCompleted
//                                )
//                                preparedResult.executeAddNewFiles(
//                                    repositoryAccess,
//                                    writter::onAddCompleted
//                                )
//                            } finally {
//                                executeResult.writer.flush()
//                                executeJob = null
//                            }
                                    }
                            }
                        }
                    }

                Dialog(
                    onDismissRequest = onClose,
                ) {
                    contents(
                        repository.value.displayName,
                        preparationResult.value,
                        executeResult = executeResultString,
                        isExecuting = executeJob != null,
                        onTriggerExecute = onTriggerExecute,
                    )
                }
            }
        }
    }
}

@Composable
private fun contents(
    archiveName: String,
    preparationResult: AddOperation.PreparationResult?,
    isExecuting: Boolean,
    executeResult: String,
    onTriggerExecute: () -> Unit,
) {
    DialogContentContainer(
        title =
            buildAnnotatedString {
                append("Update index of ")

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(archiveName)
                }
            },
    ) {
        if (isExecuting || executeResult.isNotEmpty()) {
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Text(executeResult)
            }
        } else if (preparationResult != null) {
            val preparationResultSummary =
                remember(preparationResult) {
                    StringWriter()
                        .apply {
                            preparationResult.printSummary(
                                PrintWriter(this),
                                indent = " - ",
                            )
                        }.toString()
                }

            Box(Modifier.verticalScroll(rememberScrollState())) {
                Text(preparationResultSummary)
            }

            val fontSize = 12.sp
            val lineHeight = fontSize * 1.5

            val sizeDp = with(LocalDensity.current) { lineHeight.toDp() }

            FilledTonalButton(
                onClick = onTriggerExecute,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(12.dp, 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Start, "", modifier = Modifier.size(sizeDp))
                    Text("Execute index update", fontSize = fontSize, lineHeight = lineHeight)
                }
            }
        } else {
            Text("Preparing...")
        }
    }
}

private val demo_preparation_result =
    AddOperation.PreparationResult(
        newFiles =
            listOf(
                "Documents/Something/There.pdf",
                "Photos/2024/04/photo_09.JPG",
            ),
        moves = emptyList(),
        missingFiles = emptyList(),
    )

@Preview
@Composable
private fun UpdateIndexOperationViewPreview() {
    Box(
        Modifier.fillMaxSize().background(Color.DarkGray).padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            contents(
                archiveName = "Family Stuff",
                preparationResult = null,
                isExecuting = false,
                executeResult = "",
                onTriggerExecute = {},
            )

            contents(
                archiveName = "Family Stuff",
                preparationResult = demo_preparation_result,
                isExecuting = false,
                executeResult = "",
                onTriggerExecute = {},
            )

            contents(
                archiveName = "Family Stuff",
                preparationResult = demo_preparation_result,
                isExecuting = true,
                executeResult = "added: Documents/Something/There.pdf",
                onTriggerExecute = {},
            )

            contents(
                archiveName = "Family Stuff",
                preparationResult = demo_preparation_result,
                isExecuting = true,
                executeResult = "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
                onTriggerExecute = {},
            )
        }
    }
}
