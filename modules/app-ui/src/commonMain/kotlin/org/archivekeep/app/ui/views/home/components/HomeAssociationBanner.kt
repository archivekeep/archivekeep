package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.layout.views.viewHorizontalPadding
import org.archivekeep.app.ui.views.home.model.AssociationBannerState
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_association_banner_both_separate_action
import org.archivekeep.ui.resources.home_association_banner_both_separate_state
import org.archivekeep.ui.resources.home_association_banner_no_association_action
import org.archivekeep.ui.resources.home_association_banner_no_association_state
import org.archivekeep.ui.resources.home_association_banner_remote_only_action
import org.archivekeep.ui.resources.home_association_banner_remote_only_state
import org.archivekeep.ui.resources.home_association_banner_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeAssociationBanner(state: AssociationBannerState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider()
        HomeAssociationBannerContent(state)
        HorizontalDivider()
    }
}

@Composable
fun HomeAssociationBannerContent(state: AssociationBannerState) {
    val (stateText, actionText) =
        when (state) {
            AssociationBannerState.NoAssociation -> {
                Pair(
                    stringResource(Res.string.home_association_banner_no_association_state),
                    stringResource(Res.string.home_association_banner_no_association_action),
                )
            }

            AssociationBannerState.RemoteOnlyAssociated -> {
                Pair(
                    stringResource(Res.string.home_association_banner_remote_only_state),
                    stringResource(Res.string.home_association_banner_remote_only_action),
                )
            }

            AssociationBannerState.BothSeparate -> {
                Pair(
                    stringResource(Res.string.home_association_banner_both_separate_state),
                    stringResource(Res.string.home_association_banner_both_separate_action),
                )
            }
        }

    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().viewHorizontalPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.home_association_banner_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            )
            Text(
                text = stateText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
