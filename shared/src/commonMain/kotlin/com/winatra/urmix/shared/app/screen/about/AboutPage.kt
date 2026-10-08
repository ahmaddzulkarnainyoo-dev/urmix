/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.winatra.urmix.shared.app.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import com.winatra.urmix.shared.app.BuildConfig
import com.winatra.urmix.shared.app.Constants
import com.winatra.urmix.shared.app.composable.about.LinkListItem
import com.winatra.urmix.shared.app.model.Link
import com.winatra.urmix.shared.app.platform.ShareHandler
import com.winatra.urmix.shared.app.preview.ThemePreviewProvider
import com.winatra.urmix.shared.app.theme.iconTVDPI
import com.winatra.urmix.shared.app.theme.logoBackground
import com.winatra.urmix.shared.app.theme.spaceLarge
import com.winatra.urmix.shared.app.theme.spaceXSmall
import com.winatra.urmix.shared.app.theme.spaceXXSmall
import com.winatra.urmix.shared.generated.resources.Res
import com.winatra.urmix.shared.generated.resources.app_description
import com.winatra.urmix.shared.generated.resources.contribution_encouragement
import com.winatra.urmix.shared.generated.resources.contribution_title
import com.winatra.urmix.shared.generated.resources.donation_encouragement
import com.winatra.urmix.shared.generated.resources.donation_title
import com.winatra.urmix.shared.generated.resources.github
import com.winatra.urmix.shared.generated.resources.give_back
import com.winatra.urmix.shared.generated.resources.ic_foreground
import com.winatra.urmix.shared.generated.resources.instagram
import com.winatra.urmix.shared.generated.resources.social_github_title
import com.winatra.urmix.shared.generated.resources.social_instagram_title
import com.winatra.urmix.shared.generated.resources.urmix_about_banner
import com.winatra.urmix.shared.generated.resources.urmix_attribution
import com.winatra.urmix.shared.generated.resources.urmix_team_badge
import com.winatra.urmix.shared.generated.resources.view_on_github
import com.winatra.urmix.shared.generated.resources.winatra_member_ahmddzlkrn
import com.winatra.urmix.shared.generated.resources.winatra_member_imamyahyaaaaa
import com.winatra.urmix.shared.generated.resources.winatra_team_members
import com.winatra.urmix.shared.generated.resources.winatra_team_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AboutPage(shareHandler: ShareHandler = koinInject()) {
    AboutPageContent(
        onOpenUrl = { url -> shareHandler.openUrlInBrowser(url) }
    )
}

@Composable
fun AboutPageContent(
    links: List<Link> = defaultLinks(),
    onOpenUrl: (url: String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(spaceXXSmall)
    ) {
        item {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                painter = painterResource(Res.drawable.urmix_about_banner),
                contentDescription = BuildConfig.APP_NAME,
                contentScale = ContentScale.Crop
            )
        }

        // Page Header
        item {
            Column(
                modifier = Modifier
                    .padding(spaceLarge)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .requiredSize(iconTVDPI)
                        .clip(CircleShape)
                        .background(color = logoBackground),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = BuildConfig.APP_NAME
                )
                Spacer(modifier = Modifier.height(spaceXSmall))
                Text(
                    text = BuildConfig.APP_NAME,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.app_description),
                    textAlign = TextAlign.Center
                )
            }
        }

        // URMIX transparent attribution (blueprint v2 §8)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spaceLarge),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    modifier = Modifier.padding(spaceLarge),
                    text = stringResource(Res.string.urmix_attribution),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // URMIX WINATRA team header (blueprint v1.0.2 §8)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spaceLarge),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(spaceLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.winatra_team_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spaceXSmall))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.winatra_team_members),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(spaceXSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TeamMemberAvatar(
                            initial = "A",
                            handle = stringResource(Res.string.winatra_member_ahmddzlkrn)
                        )
                        TeamMemberAvatar(
                            initial = "I",
                            handle = stringResource(Res.string.winatra_member_imamyahyaaaaa)
                        )
                    }
                    Spacer(modifier = Modifier.height(spaceXSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onOpenUrl(Constants.URL_INSTAGRAM) }) {
                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(Res.drawable.instagram),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(spaceXSmall))
                            Text(text = stringResource(Res.string.social_instagram_title))
                        }
                        TextButton(onClick = { onOpenUrl(Constants.URL_GITHUB) }) {
                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(Res.drawable.github),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(spaceXSmall))
                            Text(text = stringResource(Res.string.social_github_title))
                        }
                    }
                }
            }
        }

        // Additional URMIX links
        items(items = links, key = { link -> link.url }) { link ->
            LinkListItem(
                link = link,
                onAction = { onOpenUrl(link.url) }
            )
        }
    }
}

@Composable
private fun TeamMemberAvatar(initial: String, handle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.requiredSize(108.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(Res.drawable.urmix_team_badge),
                contentDescription = null,
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(
            text = handle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun defaultLinks() = listOf(
    // v1.0.2: tombol WEBSITE/FAQ/PRIVACY di-hide — URL masih placeholder winatra.com.
    Link(
        title = stringResource(Res.string.contribution_title),
        description = stringResource(Res.string.contribution_encouragement),
        action = stringResource(Res.string.view_on_github),
        url = Constants.URL_GITHUB
    ),
    Link(
        title = stringResource(Res.string.donation_title),
        description = stringResource(Res.string.donation_encouragement),
        action = stringResource(Res.string.give_back),
        url = Constants.URL_DONATION
    )
)

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun AboutPagePreview() {
    AboutPageContent()
}
