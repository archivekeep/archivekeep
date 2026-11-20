package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import kotlinx.coroutines.launch
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.onboarding_button_back
import org.archivekeep.ui.resources.onboarding_intro_button
import org.archivekeep.ui.resources.onboarding_intro_paragraph_1
import org.archivekeep.ui.resources.onboarding_intro_paragraph_2
import org.archivekeep.ui.resources.onboarding_intro_paragraph_3
import org.archivekeep.ui.resources.onboarding_intro_paragraph_4
import org.archivekeep.ui.resources.onboarding_intro_title
import org.archivekeep.ui.resources.onboarding_local_button
import org.archivekeep.ui.resources.onboarding_local_button_next
import org.archivekeep.ui.resources.onboarding_local_description_1
import org.archivekeep.ui.resources.onboarding_local_description_2
import org.archivekeep.ui.resources.onboarding_local_title
import org.archivekeep.ui.resources.onboarding_remote_button_filesystem
import org.archivekeep.ui.resources.onboarding_remote_button_remote
import org.archivekeep.ui.resources.onboarding_remote_description_1
import org.archivekeep.ui.resources.onboarding_remote_description_2
import org.archivekeep.ui.resources.onboarding_remote_title
import org.jetbrains.compose.resources.stringResource

enum class OnboardingPage {
    Intro,
    LocalRepository,
    RemoteStorage,
}

@Composable
fun OnboardingCarousel(currentPage: OnboardingPage) {
    val launchers = LocalArchiveOperationLaunchers.current
    val scope = rememberCoroutineScope()
    val pageCount = OnboardingPage.entries.size

    val state =
        rememberPagerState(
            pageCount = { pageCount },
            initialPage = currentPage.ordinal,
        )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                when (OnboardingPage.entries[page]) {
                    OnboardingPage.Intro -> {
                        OnboardingIntroPage {
                            scope.launch { state.animateScrollToPage(1) }
                        }
                    }

                    OnboardingPage.LocalRepository -> {
                        OnboardingLocalRepoPage(
                            onAddLocal = {
                                launchers.openAddFileSystemRepository()
                            },
                            onNext = {
                                scope.launch { state.animateScrollToPage(2) }
                            },
                        )
                    }

                    OnboardingPage.RemoteStorage -> {
                        OnboardingRemoteStoragePage(
                            onAddFilesystem = {
                                launchers.openAddFileSystemRepository()
                            },
                            onAddRemote = {
                                launchers.openAddRemoteRepository()
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.currentPage > 0) {
                IconButton(
                    onClick = {
                        scope.launch { state.animateScrollToPage(state.currentPage - 1) }
                    },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(40.dp),
                ) {
                    Icon(
                        imageVector = TablerIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.onboarding_button_back),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { index ->
                    val isActive = state.currentPage == index
                    Box(
                        modifier =
                            Modifier
                                .size(if (isActive) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray,
                                ),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingIntroPage(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_intro_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp, bottom = 24.dp),
        )
        HorizontalDivider()
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            ScrollableColumn(
                Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.onboarding_intro_paragraph_1),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 24.dp).widthIn(max = 420.dp),
                )
                Text(
                    text = stringResource(Res.string.onboarding_intro_paragraph_2),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 24.dp).widthIn(max = 480.dp),
                )
                Text(
                    text = stringResource(Res.string.onboarding_intro_paragraph_3),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 24.dp).widthIn(max = 480.dp),
                )
                Text(
                    text = stringResource(Res.string.onboarding_intro_paragraph_4),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 24.dp).widthIn(max = 420.dp),
                )
                Button(
                    onClick = onNext,
                ) {
                    Text(stringResource(Res.string.onboarding_intro_button))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun OnboardingLocalRepoPage(
    onAddLocal: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_local_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp, bottom = 24.dp),
        )
        HorizontalDivider()
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            ScrollableColumn(
                Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.onboarding_local_description_1),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 12.dp).widthIn(max = 450.dp),
                )
                Text(
                    text = stringResource(Res.string.onboarding_local_description_2),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 32.dp).widthIn(max = 510.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = onAddLocal,
                    ) {
                        Text(stringResource(Res.string.onboarding_local_button))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = onNext,
                        ) {
                            Text(stringResource(Res.string.onboarding_local_button_next))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun OnboardingRemoteStoragePage(
    onAddFilesystem: () -> Unit,
    onAddRemote: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.onboarding_remote_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 40.dp, bottom = 24.dp),
        )
        HorizontalDivider()
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            ScrollableColumn(
                Modifier.fillMaxWidth(),
                columnModifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.onboarding_remote_description_1),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 12.dp).widthIn(max = 420.dp),
                )
                Text(
                    text = stringResource(Res.string.onboarding_remote_description_2),
                    color = Color.DarkGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 32.dp).widthIn(max = 420.dp),
                )
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = onAddFilesystem,
                    ) {
                        Text(stringResource(Res.string.onboarding_remote_button_filesystem))
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onAddRemote,
                    ) {
                        Text(stringResource(Res.string.onboarding_remote_button_remote))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        HorizontalDivider()
    }
}
