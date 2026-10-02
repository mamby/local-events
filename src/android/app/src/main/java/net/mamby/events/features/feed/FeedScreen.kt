package net.mamby.events.features.feed

import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.getSelectedEndDate
import androidx.compose.material3.getSelectedStartDate
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import net.mamby.androidkit.compose.action.AndroidKitActionFlyout
import net.mamby.androidkit.compose.action.AndroidKitActionFlyoutHorizontalAlignment
import net.mamby.androidkit.compose.form.AndroidKitBottomSheet
import net.mamby.androidkit.compose.form.AndroidKitBottomSheetScrollMode
import net.mamby.events.core.EventFeedItem
import net.mamby.events.core.FeedFilterChip
import net.mamby.events.core.FeedFilterKind
import net.mamby.events.core.FeedPriceFilter
import net.mamby.events.core.MediaType
import net.mamby.events.ui.FeedButton
import net.mamby.events.ui.FeedIconButton
import net.mamby.events.ui.AttendanceSummaryRow
import net.mamby.events.ui.LocalEventsColors
import net.mamby.events.ui.LocalEventsDimens
import net.mamby.events.ui.LocalEventsTypography
import net.mamby.events.ui.RemoteOrAssetImage
import net.mamby.events.ui.SvgIcon
import net.mamby.events.ui.StateMessage
import net.mamby.events.ui.shareText
import net.mamby.androidkit.compose.action.AndroidKitFloatingAction
import coil3.compose.rememberAsyncImagePainter
import net.mamby.events.ui.iconAsset

private val MinimumViewDuration = 1.seconds
private val SearchPillCriterionDisplayDuration = 6.seconds
private const val MenuSwipeDistanceThresholdFraction = 0.25f
private const val SearchHistoryPreviewLimit = 3
internal const val FeedSearchPillTestTag = "feed-search-pill"
internal const val FeedLoadingMorePillTestTag = "feed-loading-more-pill"
internal const val FeedNewItemsPillTestTag = "feed-new-items-pill"

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun FeedScreen(
    language: String,
    mediaAutoplayEnabled: Boolean,
    darkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onOpenFavorites: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var searchSheetVisible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var detailsItem by remember { mutableStateOf<EventFeedItem?>(null) }
    val canOpenMenuWithSwipe = !menuExpanded && !searchSheetVisible && detailsItem == null
    val layoutDirection = LocalLayoutDirection.current
    fun openSearchSheet() {
        viewModel.prepareSearchSheet()
        searchSheetVisible = true
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(language) {
        viewModel.onLanguageChanged(language)
    }

    LaunchedEffect(ui.shouldAutoOpenFirstSearch) {
        if (ui.shouldAutoOpenFirstSearch) {
            openSearchSheet()
            viewModel.markAutoOpenConsumed()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            if (event == FeedEvent.SearchApplied) {
                searchSheetVisible = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        SelectionContainer(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .openMenuOnLogicalSwipe(
                        enabled = canOpenMenuWithSwipe,
                        layoutDirection = layoutDirection,
                        onOpenMenu = { menuExpanded = true }
                    )
            ) {
                when (val state = ui.contentState) {
                    FeedContentState.Loading -> DisableSelection {
                        StateMessage(
                            title = viewModel.string("LoadingLocalEvents"),
                            loading = true
                        )
                    }

                    FeedContentState.FirstSearch -> DisableSelection {
                        StateMessage(
                            title = "",
                            buttonText = viewModel.string("FindEvents"),
                            onButtonClick = ::openSearchSheet
                        )
                    }

                    FeedContentState.Empty -> DisableSelection {
                        StateMessage(
                            title = viewModel.string("NoLocalEventsTitle"),
                            subtitle = viewModel.string("NoLocalEventsSubtitle"),
                            buttonText = viewModel.string("Refresh"),
                            onButtonClick = viewModel::refresh
                        )
                    }

                    is FeedContentState.Error -> DisableSelection {
                        StateMessage(
                            title = viewModel.string("FeedUnavailableTitle"),
                            subtitle = state.message,
                            buttonText = viewModel.string("Retry"),
                            onButtonClick = viewModel::refresh
                        )
                    }

                    FeedContentState.Ready -> {
                        val context = LocalContext.current
                        val feedPlayer = remember(context) {
                            ExoPlayer.Builder(context).build().apply {
                                setAudioAttributes(AudioAttributes.DEFAULT, true)
                                repeatMode = Player.REPEAT_MODE_ONE
                            }
                        }
                        DisposableEffect(feedPlayer) {
                            onDispose {
                                feedPlayer.release()
                            }
                        }
                        val pagerState = rememberPagerState(
                            initialPage = ui.currentIndex.coerceAtLeast(0),
                            pageCount = { ui.items.size }
                        )
                        val itemIds = ui.items.map { it.id }

                        LaunchedEffect(pagerState, itemIds) {
                            snapshotFlow { pagerState.settledPage }.collectLatest { page ->
                                viewModel.updateCurrentIndex(page)
                                val item = ui.items.getOrNull(page) ?: return@collectLatest
                                delay(MinimumViewDuration)
                                if (pagerState.settledPage == page) {
                                    viewModel.recordView(item)
                                }
                            }
                        }

                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            val item = ui.items[page]
                            val isCurrent = page == pagerState.settledPage
                            FeedItemPage(
                                item = item,
                                isCurrent = isCurrent,
                                player = if (isCurrent) feedPlayer else null,
                                mediaAutoplayEnabled = mediaAutoplayEnabled,
                                viewModel = viewModel,
                                onDetails = { detailsItem = item },
                                onFavorite = { viewModel.toggleFavorite(item) },
                                onOpenFavorites = onOpenFavorites
                            )
                        }
                    }
                }

                DisableSelection {
                    FeedTopOverlay(
                        ui = ui,
                        viewModel = viewModel,
                        onSearch = { if (ui.contentState != FeedContentState.Loading) openSearchSheet() },
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = { menuExpanded = it },
                        onOpenFavorites = {
                            menuExpanded = false
                            onOpenFavorites()
                        },
                        onOpenSettings = {
                            menuExpanded = false
                            onOpenSettings()
                        },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }

        SearchSheet(
            visible = searchSheetVisible,
            ui = ui,
            viewModel = viewModel,
            darkTheme = darkTheme,
            onDismiss = {
                if (!ui.isSearchApplying) {
                    viewModel.onSearchSheetDismissed()
                    searchSheetVisible = false
                }
            },
            onApply = {
                scope.launch {
                    viewModel.applySearch()
                }
            }
        )

        DetailsSheet(
            item = detailsItem,
            viewModel = viewModel,
            darkTheme = darkTheme,
            onDismiss = { detailsItem = null }
        )
    }
}

private fun Modifier.openMenuOnLogicalSwipe(
    enabled: Boolean,
    layoutDirection: LayoutDirection,
    onOpenMenu: () -> Unit
): Modifier = pointerInput(enabled, layoutDirection, onOpenMenu) {
    if (!enabled) {
        return@pointerInput
    }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val velocityTracker = VelocityTracker().apply {
            addPosition(down.uptimeMillis, down.position)
        }
        var currentPositionX = down.position.x
        var accepted = false
        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
            val opensMenu = when (layoutDirection) {
                LayoutDirection.Ltr -> overSlop < 0f
                LayoutDirection.Rtl -> overSlop > 0f
            }
            if (opensMenu) {
                accepted = true
                currentPositionX = change.position.x
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
            }
        }

        if (accepted && drag != null) {
            val completed = horizontalDrag(drag.id) { change ->
                currentPositionX = change.position.x
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
            }
            val displacement = currentPositionX - down.position.x
            val menuSwipeDistance = when (layoutDirection) {
                LayoutDirection.Ltr -> -displacement
                LayoutDirection.Rtl -> displacement
            }
            val menuSwipeVelocity = when (layoutDirection) {
                LayoutDirection.Ltr -> -velocityTracker.calculateVelocity().x
                LayoutDirection.Rtl -> velocityTracker.calculateVelocity().x
            }
            val isLongEnough = menuSwipeDistance >= size.width * MenuSwipeDistanceThresholdFraction
            val isFastEnough = menuSwipeVelocity >= viewConfiguration.minimumFlingVelocity

            if (completed && (isLongEnough || isFastEnough)) {
                onOpenMenu()
            }
        }
    }
}

@Composable
private fun FeedTopOverlay(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    onSearch: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val criteria = remember(ui.activeQuery, ui.language) { viewModel.searchPillCriteria() }
    var criterionIndex by remember(criteria) { mutableIntStateOf(0) }

    LaunchedEffect(criteria) {
        criterionIndex = 0
        if (criteria.size < 2) {
            return@LaunchedEffect
        }

        while (true) {
            delay(SearchPillCriterionDisplayDuration)
            criterionIndex = (criterionIndex + 1) % criteria.size
        }
    }

    val displayedCriterion = criteria.getOrElse(criterionIndex) { criteria.first() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = LocalEventsDimens.FeedTopOverlayHorizontalPadding,
                top = LocalEventsDimens.FeedTopOverlayTopPadding,
                end = LocalEventsDimens.FeedTopOverlayEndPadding
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalEventsDimens.FeedTopOverlayControlSpacing)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag(FeedSearchPillTestTag)
                    .clip(RoundedCornerShape(LocalEventsDimens.FeedSearchPillRadius))
                    .background(
                        if (ui.contentState == FeedContentState.FirstSearch || ui.contentState == FeedContentState.Empty) {
                            LocalEventsColors.AppElevatedSurfaceDark
                        } else {
                            LocalEventsColors.FeedSearchPillBackground
                        }
                    )
                    .clickable(
                        onClickLabel = viewModel.string("SearchPillHint"),
                        role = Role.Button,
                        onClick = onSearch
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = displayedCriterion,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {
                            contentDescription = criteria.joinToString(separator = ", ") { it.text }
                        },
                    transitionSpec = {
                        (slideInVertically { height -> -height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> height } + fadeOut())
                    },
                    label = "Feed search criterion"
                ) { criterion ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        criterion.icon?.let { icon ->
                            SvgIcon(
                                name = icon.assetName(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = criterion.text,
                            style = LocalEventsTypography.FeedBadge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                SvgIcon(
                    name = "icon_search.svg",
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            FeedMenu(
                expanded = menuExpanded,
                viewModel = viewModel,
                onExpandedChange = onMenuExpandedChange,
                onOpenFavorites = onOpenFavorites,
                onOpenSettings = onOpenSettings
            )
        }

        if (ui.isLoadingMore) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp)
                    .testTag(FeedLoadingMorePillTestTag)
                    .clip(RoundedCornerShape(18.dp))
                    .background(LocalEventsColors.FeedBadgeBackground)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = LocalEventsColors.FeedAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = viewModel.string("LoadingMore"), style = LocalEventsTypography.FeedBadge)
            }
        }

        if (ui.hasPendingNewResults) {
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 6.dp, end = 68.dp)
                    .testTag(FeedNewItemsPillTestTag)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalEventsColors.FeedAccent)
                    .clickable(
                        onClickLabel = viewModel.string("ReloadNewResultsHint"),
                        role = Role.Button,
                        onClick = viewModel::applyPendingRefresh
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = viewModel.pendingNewResultsText(),
                    style = LocalEventsTypography.FeedBadge.copy(color = LocalEventsColors.FeedOnAccent)
                )
            }
        }
    }
}

private fun FeedSearchCriterionIcon.assetName(): String =
    when (this) {
        FeedSearchCriterionIcon.Location -> "icon_location_pin.svg"
        FeedSearchCriterionIcon.Filter -> "icon_filter.svg"
    }

@Composable
private fun FeedMenu(
    expanded: Boolean,
    viewModel: FeedViewModel,
    onExpandedChange: (Boolean) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val favoriteIcon = rememberAsyncImagePainter(iconAsset("icon_favorite_off.svg"))
    val settingsIcon = rememberAsyncImagePainter(iconAsset("icon_settings.svg"))
    Box {
        FeedIconButton(
            iconName = "icon_menu.svg",
            contentDescription = viewModel.string("MenuButtonDescription"),
            onClick = { onExpandedChange(!expanded) },
            iconSize = LocalEventsDimens.FeedSettingsIconSize
        )
        AndroidKitActionFlyout(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            horizontalAlignment = AndroidKitActionFlyoutHorizontalAlignment.End
        ) {
            item(
                label = viewModel.string("FavoritesTitle"),
                onClick = onOpenFavorites,
                icon = favoriteIcon
            )
            item(
                label = viewModel.string("SettingsTitle"),
                onClick = onOpenSettings,
                icon = settingsIcon
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FeedItemPage(
    item: EventFeedItem,
    isCurrent: Boolean,
    player: Player?,
    mediaAutoplayEnabled: Boolean,
    viewModel: FeedViewModel,
    onDetails: () -> Unit,
    onFavorite: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var playbackActive by remember(item.id) { mutableStateOf(false) }
    var playbackInitialized by remember(item.id) { mutableStateOf(false) }

    val playbackUrl = item.media.playbackUrl
    LaunchedEffect(player, playbackUrl, playbackInitialized) {
        if (player == null) {
            return@LaunchedEffect
        }

        if (playbackInitialized && item.media.hasPlayback && !playbackUrl.isNullOrBlank()) {
            player.setMediaItem(MediaItem.fromUri(playbackUrl.toUri()))
            player.prepare()
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    FeedPlaybackLifecycleEffect(
        player = player,
        shouldPlay = isCurrent && playbackActive
    )

    LaunchedEffect(isCurrent, item.id, mediaAutoplayEnabled) {
        if (isCurrent) {
            playbackActive = item.media.hasPlayback && mediaAutoplayEnabled
            playbackInitialized = playbackActive
        } else {
            playbackActive = false
            playbackInitialized = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        RemoteOrAssetImage(
            source = item.media.displayUrl,
            contentDescription = viewModel.titleText(item),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (player != null && playbackInitialized && item.media.type == MediaType.Video) {
            PlayerSurface(
                player = player,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (isCurrent) {
                        Modifier.pointerInput(item.id, hapticFeedback, onDetails) {
                            detectTapGestures(
                                onLongPress = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDetails()
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(LocalEventsColors.FeedOverlayBrush)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = LocalEventsDimens.FeedOverlayHorizontalPadding,
                    top = LocalEventsDimens.FeedOverlayTopPadding,
                    end = LocalEventsDimens.FeedActionColumnEndPadding,
                    bottom = LocalEventsDimens.FeedOverlayBottomPadding
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            if (isCurrent) {
                FeedItemText(
                    item = item,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            } else {
                DisableSelection {
                    FeedItemText(
                        item = item,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(0.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FeedIconButton(
                    iconName = "icon_details.svg",
                    contentDescription = viewModel.string("EventDetailsDescription"),
                    onClick = onDetails
                )
                FeedIconButton(
                    iconName = "icon_share.svg",
                    contentDescription = viewModel.string("ShareEventDescription"),
                    onClick = {
                        viewModel.recordShareIntent(item)
                        shareText(context, viewModel.titleText(item), viewModel.fullDetailsText(item))
                    }
                )
                FeedIconButton(
                    iconName = if (item.isFavorited) "icon_favorite_on.svg" else "icon_favorite_off.svg",
                    contentDescription = viewModel.string(if (item.isFavorited) "RemoveFavoriteDescription" else "AddFavoriteDescription"),
                    onClick = onFavorite,
                    onLongClick = onOpenFavorites,
                    onLongClickLabel = viewModel.string("FavoritesCardHint")
                )
                FeedIconButton(
                    iconName = if (playbackActive) "icon_pause.svg" else "icon_play.svg",
                    contentDescription = viewModel.string(
                        when {
                            !item.media.hasPlayback -> "PlaybackUnavailableDescription"
                            playbackActive -> "PausePlaybackDescription"
                            else -> "PlayPlaybackDescription"
                        }
                    ),
                    onClick = {
                        if (item.media.hasPlayback) {
                            playbackInitialized = true
                            playbackActive = !playbackActive
                        }
                    },
                    enabled = item.media.hasPlayback,
                    modifier = Modifier.alpha(if (item.media.hasPlayback) 1f else 0.35f)
                )
            }
        }
    }
}

@Composable
internal fun FeedPlaybackLifecycleEffect(
    player: Player?,
    shouldPlay: Boolean
) {
    LifecycleResumeEffect(player, shouldPlay) {
        player?.playWhenReady = shouldPlay
        onPauseOrDispose {
            player?.pause()
        }
    }
}

@Composable
private fun FeedItemText(
    item: EventFeedItem,
    viewModel: FeedViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = viewModel.categoryText(item),
            style = LocalEventsTypography.FeedMeta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = viewModel.titleText(item),
            style = LocalEventsTypography.FeedTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = viewModel.descriptionText(item),
            style = LocalEventsTypography.FeedDescription,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        AttendanceSummaryRow(
            summary = viewModel.attendanceSummary(item),
            style = LocalEventsTypography.FeedDetail,
            modifier = Modifier.fillMaxWidth()
        )
        FeedDatePriceRow(item = item, viewModel = viewModel)
    }
}

@Composable
private fun FeedDatePriceRow(
    item: EventFeedItem,
    viewModel: FeedViewModel
) {
    val priceSummary = viewModel.priceSummary(item)

    Row(
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = "${viewModel.feedDateText(item)}, ${priceSummary.contentDescription}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = viewModel.feedDateText(item),
            style = LocalEventsTypography.FeedSchedule,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "·",
            style = LocalEventsTypography.FeedSchedule,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = priceSummary.primaryText,
            style = LocalEventsTypography.FeedSchedule,
            maxLines = 1
        )
        if (priceSummary.hasAdditionalDetails) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "+",
                style = LocalEventsTypography.FeedSchedule,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchSheet(
    visible: Boolean,
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val searchSummaryScrollState = rememberScrollState()
    val termEditorScrollState = rememberScrollState()
    val locationEditorScrollState = rememberScrollState()
    val filterEditorScrollState = rememberScrollState()
    val recentSearchesScrollState = rememberScrollState()
    val sheetDismissGesturesEnabled = when (ui.searchSheetPage) {
        FeedSearchSheetPage.Search -> searchSummaryScrollState.value == 0
        FeedSearchSheetPage.Terms -> termEditorScrollState.value == 0
        FeedSearchSheetPage.Locations -> locationEditorScrollState.value == 0
        FeedSearchSheetPage.Filters -> filterEditorScrollState.value == 0
        FeedSearchSheetPage.Recent -> recentSearchesScrollState.value == 0
    }

    AndroidKitBottomSheet(
        visible = visible,
        title = viewModel.string(
            when (ui.searchSheetPage) {
                FeedSearchSheetPage.Search -> "FindEvents"
                FeedSearchSheetPage.Terms -> "SearchTermsTitle"
                FeedSearchSheetPage.Locations -> "SearchLocationsTitle"
                FeedSearchSheetPage.Filters -> "FiltersLabel"
                FeedSearchSheetPage.Recent -> "RecentSearchesTitle"
            }
        ),
        onDismiss = onDismiss,
        onBack = (viewModel::returnToSearchSummary).takeIf {
            ui.searchSheetPage != FeedSearchSheetPage.Search && !ui.isSearchApplying
        },
        backContentDescription = viewModel.string("BackDescription"),
        maxHeightFraction = LocalEventsDimens.FeedSheetMaxHeightFraction,
        scrollMode = AndroidKitBottomSheetScrollMode.ContentManaged,
        gesturesEnabled = !ui.isSearchApplying,
        dismissGesturesEnabled = sheetDismissGesturesEnabled,
        closeContentDescription = viewModel.string("Close"),
        floatingAction = AndroidKitFloatingAction.Bar {
                when (ui.searchSheetPage) {
                    FeedSearchSheetPage.Search -> {
                        text(
                            label = viewModel.string("Clear"),
                            onClick = viewModel::clearSearchDraft,
                            enabled = ui.hasCommittedSearchCriteria
                        )
                        text(
                            label = viewModel.string("ShowResults"),
                            onClick = onApply,
                            enabled = ui.canSubmitSearch
                        )
                    }

                    FeedSearchSheetPage.Terms,
                    FeedSearchSheetPage.Locations -> text(
                        label = viewModel.string("Done"),
                        onClick = viewModel::returnToSearchSummary
                    )

                    FeedSearchSheetPage.Filters -> {
                        text(
                            label = viewModel.string("ResetFilters"),
                            onClick = viewModel::clearOptionalFilters
                        )
                        text(
                            label = viewModel.string("Done"),
                            onClick = viewModel::returnToSearchSummary
                        )
                    }

                    FeedSearchSheetPage.Recent -> text(
                        label = viewModel.string("ClearAllRecentSearches"),
                        onClick = viewModel::clearSearchHistory,
                        enabled = ui.searchHistory.isNotEmpty()
                    )
                }
            }
    ) { managedContentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (ui.searchSheetPage) {
                FeedSearchSheetPage.Search -> SearchSummaryBody(
                    ui = ui,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onApply = onApply,
                    scrollState = searchSummaryScrollState,
                    contentPadding = managedContentPadding,
                    modifier = Modifier.fillMaxSize()
                )

                FeedSearchSheetPage.Terms -> TermEditorBody(
                    ui = ui,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    scrollState = termEditorScrollState,
                    contentPadding = managedContentPadding,
                    modifier = Modifier.fillMaxSize()
                )

                FeedSearchSheetPage.Locations -> LocationEditorBody(
                    ui = ui,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    scrollState = locationEditorScrollState,
                    contentPadding = managedContentPadding,
                    modifier = Modifier.fillMaxSize()
                )

                FeedSearchSheetPage.Filters -> FilterEditorBody(
                    ui = ui,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    scrollState = filterEditorScrollState,
                    contentPadding = managedContentPadding,
                    modifier = Modifier.fillMaxSize()
                )

                FeedSearchSheetPage.Recent -> RecentSearchesBody(
                    ui = ui,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    scrollState = recentSearchesScrollState,
                    contentPadding = managedContentPadding,
                    modifier = Modifier.fillMaxSize()
                )
            }

        }
    }
}

@Composable
private fun SearchSummaryBody(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    onApply: () -> Unit,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            ui.isSearchApplying -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalEventsColors.FeedAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = viewModel.string("LoadingResults"), style = sheetControlTextStyle(darkTheme))
            }

            ui.hasSearchError -> Text(
                text = ui.searchErrorText,
                style = sheetControlTextStyle(darkTheme, sheetErrorTextColor(darkTheme))
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchSectionLabel(viewModel.string("SearchTermsLabel"), darkTheme)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.searchTerms.forEach { term ->
                    RemovableCriterionChip(
                        label = term,
                        removeContentDescription = viewModel.formatString("RemoveSearchTermFormat", term),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeSearchTerm(term) }
                    )
                }
                SuggestionChip(
                    onClick = viewModel::openTermEditor,
                    label = { Text(viewModel.string("AddSearchTerm")) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchSectionLabel(viewModel.string("LocationsLabel"), darkTheme)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.selectedLocationScopes.forEach { location ->
                    RemovableCriterionChip(
                        label = location.displayPath,
                        removeContentDescription = viewModel.formatString("RemoveLocationFormat", location.displayPath),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeLocationScope(location) }
                    )
                }
                ui.customLocations.forEach { location ->
                    RemovableCriterionChip(
                        label = location,
                        removeContentDescription = viewModel.formatString("RemoveLocationFormat", location),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeCustomLocation(location) }
                    )
                }
                SuggestionChip(
                    onClick = viewModel::openLocationEditor,
                    label = { Text(viewModel.string("AddLocation")) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SearchSectionLabel(viewModel.string("FiltersLabel"), darkTheme)
            FilterChip(
                selected = ui.hasAppliedFilters,
                onClick = viewModel::openFilterEditor,
                label = {
                    Text(
                        text = if (ui.optionalFilterCount > 0) {
                            viewModel.formatString("FiltersCountFormat", ui.optionalFilterCount)
                        } else {
                            viewModel.string("EditFilters")
                        }
                    )
                }
            )
        }

        if (ui.hasAppliedFilters) {
            AppliedFilterRow(
                filters = ui.appliedFilters,
                darkTheme = darkTheme,
                onRemove = viewModel::removeAppliedFilter
            )
        }

        if (ui.hasSuggestedFilters) {
            SearchSectionLabel(viewModel.string("SuggestedLabel"), darkTheme)
            SuggestedFilterRow(filters = ui.suggestedFilters, onSelect = viewModel::acceptSuggestedFilter)
        }

        if (ui.canShowSearchHistory) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchSectionLabel(viewModel.string("RecentLabel"), darkTheme)
                SearchHistoryList(
                    historyItems = ui.searchHistory.take(SearchHistoryPreviewLimit),
                    viewModel = viewModel,
                    darkTheme = darkTheme
                )
                if (ui.searchHistory.size > SearchHistoryPreviewLimit) {
                    TextButton(
                        onClick = viewModel::openSearchHistory,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = viewModel.formatString(
                                "SeeAllRecentSearchesFormat",
                                ui.searchHistory.size
                            ),
                            color = LocalEventsColors.FeedAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TermEditorBody(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ui.searchTerms.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.searchTerms.forEach { term ->
                    RemovableCriterionChip(
                        label = term,
                        removeContentDescription = viewModel.formatString("RemoveSearchTermFormat", term),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeSearchTerm(term) }
                    )
                }
            }
        }

        SearchTextField(
            value = ui.termSearchText,
            label = viewModel.string("SearchTermsLabel"),
            placeholder = viewModel.string("SearchTermsPlaceholder"),
            onValueChange = viewModel::onTermSearchTextChanged,
            darkTheme = darkTheme
        )
        SearchSuggestionStatus(
            isLoading = ui.isTermSearchLoading,
            errorText = ui.termSearchErrorText,
            loadingText = viewModel.string("LoadingSuggestions"),
            darkTheme = darkTheme
        )
        ui.termSuggestions.forEach { suggestion ->
            SearchSuggestionRow(
                title = suggestion.label,
                subtitle = null,
                darkTheme = darkTheme,
                onClickLabel = viewModel.string("AddSearchTermHint"),
                onClick = { viewModel.selectTermSuggestion(suggestion) }
            )
        }
        if (ui.canAddCustomTerm) {
            SearchSuggestionRow(
                title = viewModel.formatString("AddCustomCriterionFormat", ui.termSearchText.trim()),
                subtitle = null,
                darkTheme = darkTheme,
                onClickLabel = viewModel.string("AddSearchTermHint"),
                onClick = viewModel::addCustomTerm
            )
        } else if (
            ui.termSearchText.trim().length >= 2 &&
            !ui.isTermSearchLoading &&
            ui.termSuggestions.isEmpty() &&
            ui.termSearchErrorText.isBlank()
        ) {
            Text(text = viewModel.string("NoSearchTermsFound"), style = sheetControlTextStyle(darkTheme))
        }
    }
}

@Composable
private fun LocationEditorBody(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ui.selectedLocationScopes.isNotEmpty() || ui.customLocations.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.selectedLocationScopes.forEach { location ->
                    RemovableCriterionChip(
                        label = location.displayPath,
                        removeContentDescription = viewModel.formatString("RemoveLocationFormat", location.displayPath),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeLocationScope(location) }
                    )
                }
                ui.customLocations.forEach { location ->
                    RemovableCriterionChip(
                        label = location,
                        removeContentDescription = viewModel.formatString("RemoveLocationFormat", location),
                        darkTheme = darkTheme,
                        onRemove = { viewModel.removeCustomLocation(location) }
                    )
                }
            }
        }

        SearchTextField(
            value = ui.locationSearchText,
            label = viewModel.string("LocationsLabel"),
            placeholder = viewModel.string("SearchLocationsPlaceholder"),
            onValueChange = viewModel::onLocationSearchTextChanged,
            darkTheme = darkTheme
        )
        SearchSuggestionStatus(
            isLoading = ui.isLocationSearchLoading,
            errorText = ui.locationSearchErrorText,
            loadingText = viewModel.string("LoadingSuggestions"),
            darkTheme = darkTheme
        )
        LocationSuggestions(ui, viewModel, darkTheme)
        if (ui.canAddCustomLocation) {
            SearchSuggestionRow(
                title = viewModel.formatString("AddCustomCriterionFormat", ui.locationSearchText.trim()),
                subtitle = null,
                darkTheme = darkTheme,
                onClickLabel = viewModel.string("AddLocationHint"),
                onClick = viewModel::addCustomLocation
            )
        } else if (
            ui.locationSearchText.trim().length >= 2 &&
            !ui.isLocationSearchLoading &&
            ui.locationSuggestions.isEmpty() &&
            ui.locationSearchErrorText.isBlank()
        ) {
            Text(text = viewModel.string("NoLocationsFound"), style = sheetControlTextStyle(darkTheme))
        }
    }
}

@Composable
private fun FilterEditorBody(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val appliedCategory = ui.appliedFilters.firstOrNull { it.kind == FeedFilterKind.Category }
    val appliedDate = ui.appliedFilters.firstOrNull { it.kind == FeedFilterKind.DateRange }
    val dateOptions = viewModel.dateFilterOptions()
    val hasCustomDate = appliedDate != null && dateOptions.none { option -> appliedDate.sameValueAs(option) }
    val appliedPrice = ui.appliedFilters.firstOrNull { it.kind == FeedFilterKind.Price }?.query?.priceFilter
        ?: FeedPriceFilter.Any
    val appliedAge = ui.appliedFilters.firstOrNull { it.kind == FeedFilterKind.AgeRestriction }?.query?.accessibleForAge
    val householdMinimumAge = ui.householdChildAges.minOrNull()
    var showCustomDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (ui.hasAppliedFilters) {
            SearchSectionLabel(viewModel.string("AppliedLabel"), darkTheme)
            AppliedFilterRow(
                filters = ui.appliedFilters,
                darkTheme = darkTheme,
                onRemove = viewModel::removeAppliedFilter
            )
        }

        SearchSectionLabel(viewModel.string("DateLabel"), darkTheme)
        SelectableFilterRow(
            emptyLabel = viewModel.string("AnyDate"),
            options = dateOptions,
            selected = appliedDate,
            customLabel = appliedDate?.label?.takeIf { hasCustomDate } ?: viewModel.string("ChooseDates"),
            customSelected = hasCustomDate,
            onClear = { viewModel.selectDateFilter(null) },
            onSelect = viewModel::selectDateFilter,
            onCustomSelect = { showCustomDatePicker = true }
        )

        SearchSectionLabel(viewModel.string("PriceLabel"), darkTheme)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FeedPriceFilter.entries) { price ->
                FilterChip(
                    selected = appliedPrice == price,
                    onClick = { viewModel.selectPriceFilter(price) },
                    label = {
                        Text(
                            text = when (price) {
                                FeedPriceFilter.Any -> viewModel.string("AnyPrice")
                                FeedPriceFilter.Free -> viewModel.string("FilterFree")
                                FeedPriceFilter.Paid -> viewModel.string("FilterPaid")
                            }
                        )
                    }
                )
            }
        }

        SearchSectionLabel(viewModel.string("AccessibilityLabel"), darkTheme)
        SearchTextField(
            value = ui.accessibilitySearchText,
            label = viewModel.string("AccessibilityLabel"),
            placeholder = viewModel.string("AccessibilitySearchPlaceholder"),
            onValueChange = viewModel::onAccessibilitySearchTextChanged,
            darkTheme = darkTheme
        )

        SearchSectionLabel(viewModel.string("AgeRestrictionLabel"), darkTheme)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = appliedAge == null,
                    onClick = viewModel::clearAccessibleAgeFilter,
                    label = { Text(viewModel.string("AnyAgeRestriction")) }
                )
            }
            if (householdMinimumAge != null) {
                item {
                    FilterChip(
                        selected = appliedAge == householdMinimumAge,
                        onClick = viewModel::selectHouseholdAgeFilter,
                        label = { Text(viewModel.formatString("MyHouseholdFilterFormat", householdMinimumAge)) }
                    )
                }
            }
        }
        AgeFilterTextField(
            value = ui.accessibleAgeText,
            label = viewModel.string("YoungestParticipantAgeLabel"),
            placeholder = viewModel.string("AgeYearsPlaceholder"),
            onValueChange = viewModel::onAccessibleAgeTextChanged,
            darkTheme = darkTheme,
            isError = ui.hasAccessibleAgeError,
            supportingText = if (ui.hasAccessibleAgeError) viewModel.string("AgeInputError") else null
        )

        SearchSectionLabel(viewModel.string("CategoryLabel"), darkTheme)
        SearchTextField(
            value = ui.categorySearchText,
            label = viewModel.string("SearchCategoriesPlaceholder"),
            onValueChange = viewModel::onCategorySearchTextChanged,
            darkTheme = darkTheme,
            isError = ui.hasCategorySearchError,
            supportingText = if (ui.hasCategorySearchError) ui.categorySearchErrorText else null
        )
        FilterChip(
            selected = appliedCategory == null,
            onClick = { viewModel.selectCategoryOption(null) },
            label = { Text(viewModel.string("AnyCategory")) }
        )
        when {
            ui.isCategorySearchLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalEventsColors.FeedAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = viewModel.string("LoadingCategories"), style = sheetControlTextStyle(darkTheme))
            }

            ui.hasCategorySearchError -> FeedButton(
                text = viewModel.string("Retry"),
                onClick = { viewModel.onCategorySearchTextChanged(ui.categorySearchText) },
                backgroundColor = sheetFieldContainerColor(darkTheme),
                textColor = sheetPrimaryTextColor(darkTheme)
            )

            ui.categoryOptions.isEmpty() -> Text(
                text = viewModel.string("NoCategoriesFound"),
                style = sheetControlTextStyle(darkTheme)
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.categoryOptions.forEach { option ->
                    FilterChip(
                        selected = appliedCategory?.query?.category.equals(option.value, ignoreCase = true),
                        onClick = { viewModel.selectCategoryOption(option) },
                        label = { Text(option.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showCustomDatePicker) {
        CustomDateRangeDialog(
            language = ui.language,
            selectedFilter = appliedDate,
            title = viewModel.string("ChooseDatesTitle"),
            confirmLabel = viewModel.string("Apply"),
            dismissLabel = viewModel.string("Cancel"),
            onConfirm = { startDate, endDate ->
                viewModel.selectCustomDateRange(startDate, endDate)
                showCustomDatePicker = false
            },
            onDismiss = { showCustomDatePicker = false }
        )
    }
}

@Composable
private fun LocationSuggestions(ui: FeedUiState, viewModel: FeedViewModel, darkTheme: Boolean) {
    ui.locationSuggestions.forEach { location ->
        SearchSuggestionRow(
            title = location.name,
            subtitle = location.displayPath,
            darkTheme = darkTheme,
            onClickLabel = viewModel.string("AddLocationHint"),
            onClick = { viewModel.selectLocationSuggestion(location) }
        )
    }
}

@Composable
private fun SearchSuggestionStatus(
    isLoading: Boolean,
    errorText: String,
    loadingText: String,
    darkTheme: Boolean
) {
    when {
        isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalEventsColors.FeedAccent,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = loadingText, style = sheetControlTextStyle(darkTheme))
        }

        errorText.isNotBlank() -> Text(
            text = errorText,
            style = sheetControlTextStyle(darkTheme, sheetErrorTextColor(darkTheme))
        )
    }
}

@Composable
private fun SearchSuggestionRow(
    title: String,
    subtitle: String?,
    darkTheme: Boolean,
    onClickLabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(sheetFieldContainerColor(darkTheme))
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = sheetControlTextStyle(
                darkTheme = darkTheme,
                color = sheetPrimaryTextColor(darkTheme),
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                style = sheetControlTextStyle(darkTheme),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchHistoryList(
    historyItems: List<FeedSearchHistoryItem>,
    viewModel: FeedViewModel,
    darkTheme: Boolean
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        historyItems.forEach { historyItem ->
            InputChip(
                selected = false,
                onClick = { viewModel.restoreSearchHistory(historyItem) },
                label = {
                    Text(
                        text = listOf(historyItem.title, historyItem.subtitle)
                            .filter(String::isNotBlank)
                            .joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.removeSearchHistory(historyItem) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        SvgIcon(
                            name = "icon_close.svg",
                            contentDescription = viewModel.formatString("RemoveRecentSearchFormat", historyItem.title),
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(sheetSecondaryTextColor(darkTheme))
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun RecentSearchesBody(
    ui: FeedUiState,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (ui.searchHistory.isEmpty()) {
            Text(
                text = viewModel.string("NoRecentSearches"),
                style = sheetControlTextStyle(darkTheme)
            )
        } else {
            ui.searchHistory.forEach { historyItem ->
                RecentSearchRow(
                    historyItem = historyItem,
                    viewModel = viewModel,
                    darkTheme = darkTheme
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    historyItem: FeedSearchHistoryItem,
    viewModel: FeedViewModel,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(sheetFieldContainerColor(darkTheme)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClickLabel = viewModel.string("RestoreSearchDraftHint"),
                    role = Role.Button,
                    onClick = { viewModel.restoreSearchHistory(historyItem) }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = historyItem.title,
                style = sheetControlTextStyle(
                    darkTheme = darkTheme,
                    color = sheetPrimaryTextColor(darkTheme),
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            historyItem.subtitle.takeIf(String::isNotBlank)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = sheetControlTextStyle(darkTheme),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = { viewModel.removeSearchHistory(historyItem) },
            modifier = Modifier.size(48.dp)
        ) {
            SvgIcon(
                name = "icon_close.svg",
                contentDescription = viewModel.formatString("RemoveRecentSearchFormat", historyItem.title),
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(sheetSecondaryTextColor(darkTheme))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun RemovableCriterionChip(
    label: String,
    removeContentDescription: String,
    darkTheme: Boolean,
    onRemove: () -> Unit
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingIcon = {
            SvgIcon(
                name = "icon_close.svg",
                contentDescription = removeContentDescription,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(sheetSecondaryTextColor(darkTheme))
            )
        }
    )
}

@Composable
private fun SearchTextField(
    value: String,
    label: String,
    placeholder: String? = null,
    onValueChange: (String) -> Unit,
    darkTheme: Boolean,
    onSearch: (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = sheetControlTextStyle(darkTheme, sheetPrimaryTextColor(darkTheme)),
        label = {
            Text(
                text = label,
                style = sheetControlTextStyle(darkTheme)
            )
        },
        placeholder = placeholder?.let { placeholderText ->
            {
                Text(
                    text = placeholderText,
                    style = sheetControlTextStyle(darkTheme)
                )
            }
        },
        leadingIcon = {
            SvgIcon(
                name = "icon_search.svg",
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(sheetSecondaryTextColor(darkTheme))
            )
        },
        isError = isError,
        supportingText = supportingText?.takeIf(String::isNotBlank)?.let { text ->
            {
                Text(
                    text = text,
                    style = sheetControlTextStyle(
                        darkTheme = darkTheme,
                        color = if (isError) sheetErrorTextColor(darkTheme) else sheetSecondaryTextColor(darkTheme)
                    )
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LocalEventsColors.FeedAccent,
            unfocusedBorderColor = sheetFieldContainerColor(darkTheme),
            focusedContainerColor = sheetFieldContainerColor(darkTheme),
            unfocusedContainerColor = sheetFieldContainerColor(darkTheme),
            cursorColor = LocalEventsColors.FeedAccent
        )
    )
}

@Composable
private fun AgeFilterTextField(
    value: String,
    label: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    darkTheme: Boolean,
    isError: Boolean,
    supportingText: String?
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = sheetControlTextStyle(darkTheme, sheetPrimaryTextColor(darkTheme)),
        label = { Text(text = label, style = sheetControlTextStyle(darkTheme)) },
        placeholder = { Text(text = placeholder, style = sheetControlTextStyle(darkTheme)) },
        isError = isError,
        supportingText = supportingText?.let { text ->
            { Text(text = text, style = sheetControlTextStyle(darkTheme, sheetErrorTextColor(darkTheme))) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LocalEventsColors.FeedAccent,
            unfocusedBorderColor = sheetFieldContainerColor(darkTheme),
            focusedContainerColor = sheetFieldContainerColor(darkTheme),
            unfocusedContainerColor = sheetFieldContainerColor(darkTheme),
            cursorColor = LocalEventsColors.FeedAccent
        )
    )
}

@Composable
private fun SearchSectionLabel(text: String, darkTheme: Boolean) {
    Text(
        text = text,
        style = LocalEventsTypography.EventDetailsMeta.copy(color = sheetAccentTextColor(darkTheme))
    )
}

@Composable
private fun AppliedFilterRow(
    filters: List<FeedFilterChip>,
    darkTheme: Boolean,
    onRemove: (FeedFilterChip) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            InputChip(
                selected = true,
                onClick = { onRemove(filter) },
                label = { Text(filter.label) },
                trailingIcon = {
                    SvgIcon(
                        name = "icon_close.svg",
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(sheetSecondaryTextColor(darkTheme))
                    )
                }
            )
        }
    }
}

@Composable
private fun SuggestedFilterRow(
    filters: List<FeedFilterChip>,
    onSelect: (FeedFilterChip) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            SuggestionChip(
                onClick = { onSelect(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
private fun SelectableFilterRow(
    emptyLabel: String,
    options: List<FeedFilterChip>,
    selected: FeedFilterChip?,
    customLabel: String,
    customSelected: Boolean,
    onClear: () -> Unit,
    onSelect: (FeedFilterChip) -> Unit,
    onCustomSelect: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = onClear,
                label = { Text(emptyLabel) }
            )
        }
        item {
            FilterChip(
                selected = customSelected,
                onClick = onCustomSelect,
                label = { Text(customLabel) }
            )
        }
        items(options) { option ->
            FilterChip(
                selected = selected?.sameValueAs(option) == true,
                onClick = { onSelect(option) },
                label = { Text(option.label) }
            )
        }
    }
}

private fun FeedFilterChip.sameValueAs(other: FeedFilterChip): Boolean =
    kind == other.kind && query.normalize() == other.query.normalize()

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    language: String,
    selectedFilter: FeedFilterChip?,
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (LocalDate, LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialStartDate = selectedFilter?.query?.dateFrom.toLocalDateOrNull()
    val initialEndDate = selectedFilter?.query?.dateTo.toLocalDateOrNull()
        ?.takeIf { initialStartDate != null && !it.isBefore(initialStartDate) }
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDate = initialStartDate,
        initialSelectedEndDate = initialEndDate
    )
    val baseConfiguration = LocalConfiguration.current
    val context = LocalContext.current
    val pickerConfiguration = remember(baseConfiguration, language) {
        Configuration(baseConfiguration).apply {
            setLocales(LocaleList.forLanguageTags(language))
        }
    }
    val pickerContext = remember(context, pickerConfiguration) {
        context.createConfigurationContext(pickerConfiguration)
    }

    CompositionLocalProvider(
        LocalContext provides pickerContext,
        LocalConfiguration provides pickerConfiguration
    ) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                val startDate = pickerState.getSelectedStartDate()
                TextButton(
                    enabled = startDate != null,
                    onClick = {
                        startDate?.let { selectedStart ->
                            onConfirm(selectedStart, pickerState.getSelectedEndDate())
                        }
                    }
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 64.dp, end = 12.dp)
                    )
                }
            )
        }
    }
}

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

@Composable
private fun DetailsSheet(
    item: EventFeedItem?,
    viewModel: FeedViewModel,
    darkTheme: Boolean,
    onDismiss: () -> Unit
) {
    AndroidKitBottomSheet(
        visible = item != null,
        title = viewModel.string("EventDetailsDescription"),
        onDismiss = onDismiss,
        maxHeightFraction = 0.85f,
        closeContentDescription = viewModel.string("Close")
    ) { _ ->
        if (item == null) {
            return@AndroidKitBottomSheet
        }

        val accessibility = viewModel.accessibilityText(item)
        val recommendedAge = viewModel.recommendedAgeText(item)

        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = viewModel.categoryText(item),
                        style = LocalEventsTypography.EventDetailsMeta.copy(color = sheetAccentTextColor(darkTheme))
                    )
                    Text(
                        text = viewModel.titleText(item),
                        style = LocalEventsTypography.EventDetailsTitle.copy(color = sheetPrimaryTextColor(darkTheme)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisableSelection {
                        SearchSectionLabel(viewModel.string("DateLabel"), darkTheme)
                    }
                    viewModel.occurrenceDetails(item).forEach { occurrence ->
                        Text(
                            text = occurrence,
                            style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisableSelection {
                        SearchSectionLabel(viewModel.string("PriceLabel"), darkTheme)
                    }
                    viewModel.priceDetails(item).forEach { price ->
                        Text(
                            text = price,
                            style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                        )
                    }
                }

                Text(
                    text = viewModel.longDescriptionText(item),
                    style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisableSelection {
                        SearchSectionLabel(viewModel.string("AttendanceOptionsLabel"), darkTheme)
                    }
                    viewModel.attendanceDetails(item).forEach { detail ->
                        Text(
                            text = detail,
                            style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DisableSelection {
                        SearchSectionLabel(viewModel.string("AgeInformationLabel"), darkTheme)
                    }
                    Text(
                        text = viewModel.ageRestrictionText(item),
                        style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                    )
                    if (recommendedAge.isNotBlank()) {
                        Text(
                            text = recommendedAge,
                            style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                        )
                    }
                }

                if (accessibility.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DisableSelection {
                            SearchSectionLabel(viewModel.string("AccessibilityLabel"), darkTheme)
                        }
                        Text(
                            text = accessibility,
                            style = LocalEventsTypography.EventDetailsBody.copy(color = sheetBodyTextColor(darkTheme))
                        )
                    }
                }
            }
        }
    }
}

private fun sheetControlTextStyle(
    darkTheme: Boolean,
    color: Color = sheetSecondaryTextColor(darkTheme),
    fontWeight: FontWeight? = null
) =
    LocalEventsTypography.FormControl.copy(
        color = color,
        fontWeight = fontWeight
    )

private fun sheetPrimaryTextColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.FeedTextPrimary else LocalEventsColors.EventDetailsTextPrimaryLight

private fun sheetBodyTextColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.FeedTextSecondary else LocalEventsColors.EventDetailsTextSecondaryLight

private fun sheetSecondaryTextColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.FeedTextMuted else LocalEventsColors.EventDetailsTextMutedLight

private fun sheetAccentTextColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.FeedAccentSoft else LocalEventsColors.FeedAccentTextLight

private fun sheetErrorTextColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.FeedAccent else LocalEventsColors.FeedAccentTextLight

private fun sheetFieldContainerColor(darkTheme: Boolean): Color =
    if (darkTheme) LocalEventsColors.AppElevatedSurfaceDark else LocalEventsColors.AppPageBackgroundLight
