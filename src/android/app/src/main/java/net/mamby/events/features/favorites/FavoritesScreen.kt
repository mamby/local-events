package net.mamby.events.features.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mamby.androidkit.compose.layout.AndroidKitPage
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.EventFeedItem
import net.mamby.events.ui.AttendanceSummaryRow
import net.mamby.events.ui.LocalEventsColors
import net.mamby.events.ui.LocalEventsDimens
import net.mamby.events.ui.LocalEventsTypography
import net.mamby.events.ui.RemoteOrAssetImage

@Composable
fun FavoritesScreen(
    language: String,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val darkTheme = when (ui.settings.themePreference) {
        AppThemePreference.Light -> false
        AppThemePreference.Dark -> true
        AppThemePreference.System -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    AndroidKitPage(
        title = viewModel.string("FavoritesTitle"),
        onBack = onBack,
        modifier = Modifier.fillMaxSize()
    ) { pagePadding ->
        if (ui.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pagePadding),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = viewModel.string("NoFavoritesTitle"),
                    style = LocalEventsTypography.SecondaryTitle.copy(
                        color = if (darkTheme) LocalEventsColors.FeedTextPrimary else Color(0xFF11161E)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = viewModel.string("NoFavoritesSubtitle"),
                    style = LocalEventsTypography.EventDetailsHint.copy(
                        color = if (darkTheme) LocalEventsColors.AppMutedTextDark else LocalEventsColors.AppMutedTextLight
                    )
                )
            }
            return@AndroidKitPage
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = pagePadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ui.items, key = { it.id }) { item ->
                FavoriteCard(item = item, darkTheme = darkTheme, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    item: EventFeedItem,
    darkTheme: Boolean,
    viewModel: FavoritesViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LocalEventsDimens.FavoritesCardHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(if (darkTheme) LocalEventsColors.AppElevatedSurfaceDark else LocalEventsColors.AppElevatedSurfaceLight)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteOrAssetImage(
                source = item.media.displayUrl,
                contentDescription = viewModel.titleText(item),
                modifier = Modifier
                    .size(LocalEventsDimens.FavoritesImageWidth, LocalEventsDimens.FavoritesImageHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(LocalEventsColors.FeedBackground)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = viewModel.categoryText(item),
                    style = LocalEventsTypography.EventDetailsMeta.copy(
                        color = if (darkTheme) LocalEventsColors.FeedAccentSoft else LocalEventsColors.FeedAccentTextLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = viewModel.titleText(item),
                    style = LocalEventsTypography.FeedSchedule.copy(
                        fontSize = 17.sp,
                        color = if (darkTheme) LocalEventsColors.FeedTextPrimary else Color(0xFF11161E)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = viewModel.scheduleAndPrice(item),
                    style = LocalEventsTypography.FeedDetail.copy(
                        color = if (darkTheme) LocalEventsColors.AppContentTextDark else LocalEventsColors.AppContentTextLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AttendanceSummaryRow(
                    summary = viewModel.attendanceSummary(item),
                    style = LocalEventsTypography.FeedDetail.copy(
                        color = if (darkTheme) LocalEventsColors.AppMutedTextDark else LocalEventsColors.AppMutedTextLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = viewModel.ageRestrictionText(item),
                    style = LocalEventsTypography.FeedDetail.copy(
                        color = if (darkTheme) LocalEventsColors.AppMutedTextDark else LocalEventsColors.AppMutedTextLight
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                viewModel.recommendedAgeText(item).takeIf(String::isNotBlank)?.let { recommendedAge ->
                    Text(
                        text = recommendedAge,
                        style = LocalEventsTypography.FeedDetail.copy(
                            color = if (darkTheme) LocalEventsColors.AppMutedTextDark else LocalEventsColors.AppMutedTextLight
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
