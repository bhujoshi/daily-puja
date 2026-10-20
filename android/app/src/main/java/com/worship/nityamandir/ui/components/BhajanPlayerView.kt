package com.worship.nityamandir.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.worship.nityamandir.engine.BhajanPlayer
import com.worship.nityamandir.engine.BhajanSearch
import kotlinx.coroutines.launch

// Authentic YouTube Dark App Palette
private val YouTubeBg = Color(0xFF0F0F0F)
private val YouTubeElevated = Color(0xFF181818)
private val YouTubeCardBg = Color(0xFF1E1E1E)
private val YouTubeChipBg = Color(0xFF272727)
private val YouTubeRed = Color(0xFFFF0033)
private val YouTubeTextPrimary = Color(0xFFF1F1F1)
private val YouTubeTextSecondary = Color(0xFFAAAAAA)
private val HoneyKesariya = Color(0xFFFF9E2C)

/**
 * Authentic YouTube play-button badge icon used on channel avatars and feed items.
 */
@Composable
private fun YouTubeBadgeIcon(
    hindi: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = YouTubeRed,
            modifier = modifier.size(width = 24.dp, height = 17.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (hindi) "यूट्यूब" else "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        return
    }
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = YouTubeChipBg,
        border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.65f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = YouTubeRed,
                modifier = Modifier.size(width = 22.dp, height = 16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (hindi) "यूट्यूब" else "YouTube",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * YouTube-style Miniplayer Bar used on the Mandir screen, Customizer, and Drawer.
 */
@Composable
fun BhajanMiniPlayer(
    player: BhajanPlayer,
    hindi: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    showTrackNavigation: Boolean = true,
    compact: Boolean = false
) {
    val activeTrack = player.current ?: player.catalog.firstOrNull()
    val effectiveDuration = player.duration.takeIf { it > 0L } ?: ((activeTrack?.durationSeconds ?: 0L) * 1000L)
    val progress = if (effectiveDuration > 0L) {
        (player.position.toFloat() / effectiveDuration.toFloat()).takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    } else 0f

    if (compact) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = YouTubeElevated,
            contentColor = YouTubeTextPrimary,
            border = BorderStroke(0.8.dp, Color(0x33FFFFFF)),
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier.clickable(
                    onClickLabel = if (hindi) "संगीत प्लेयर खोलें" else "Open music player",
                    onClick = onExpand
                )
            ) {
                Row(
                    Modifier
                        .heightIn(min = 46.dp, max = 50.dp)
                        .padding(start = 6.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(width = 52.dp, height = 32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(YouTubeBg)
                    ) {
                        if (activeTrack != null) {
                            YouTubeStyleThumbnail(
                                track = activeTrack,
                                showDuration = false,
                                useFixedSize = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        when {
                            player.loading -> if (hindi) "लोड हो रहा है…" else "Loading…"
                            else -> player.title
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = YouTubeTextPrimary
                    )

                    IconButton(onClick = player::toggle, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (player.failed) Icons.Default.Refresh else if (player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (player.failed) {
                                if (hindi) "फिर संगीत चलाएँ" else "Retry music"
                            } else if (player.playing) {
                                if (hindi) "संगीत रोकें" else "Pause music"
                            } else {
                                if (hindi) "संगीत चलाएँ" else "Play music"
                            },
                            tint = YouTubeTextPrimary
                        )
                    }
                    IconButton(onClick = player::remove, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Default.Close,
                            if (hindi) "संगीत हटाएँ" else "Remove music player",
                            tint = YouTubeTextSecondary
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = YouTubeRed,
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = YouTubeElevated,
        contentColor = YouTubeTextPrimary,
        border = BorderStroke(0.8.dp, Color(0x33FFFFFF)),
        shadowElevation = 8.dp
    ) {
        Column(
            Modifier.clickable(
                onClickLabel = if (hindi) "संगीत प्लेयर खोलें" else "Open music player",
                onClick = onExpand
            )
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(width = 68.dp, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YouTubeBg)
                ) {
                    if (activeTrack != null) {
                        YouTubeStyleThumbnail(
                            track = activeTrack,
                            showDuration = false,
                            useFixedSize = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        player.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = YouTubeTextPrimary
                    )
                    Text(
                        if (player.failed) {
                            if (hindi) "फिर प्रयास करें" else "Unable to play · retry"
                        } else if (player.loading) {
                            if (hindi) "लोड हो रहा है…" else "Loading…"
                        } else {
                            val artist = activeTrack?.artist?.replace("• YouTube", "")?.trim().orEmpty()
                            if (artist.isNotBlank()) "$artist • YouTube"
                            else if (hindi) "YouTube भजन" else "YouTube Bhajan"
                        },
                        fontSize = 11.sp,
                        color = YouTubeTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showTrackNavigation) {
                    IconButton(onClick = { player.skip(-1) }, enabled = player.tracks.size > 1 || player.catalog.size > 1) {
                        Icon(Icons.Default.SkipPrevious, if (hindi) "पिछला" else "Previous", tint = YouTubeTextPrimary)
                    }
                }
                IconButton(onClick = player::toggle) {
                    Icon(
                        if (player.failed) Icons.Default.Refresh else if (player.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (hindi) "चलाएँ / रोकें" else "Play / pause / retry",
                        tint = YouTubeTextPrimary
                    )
                }
                if (showTrackNavigation) {
                    IconButton(onClick = { player.skip(1) }, enabled = player.tracks.size > 1 || player.catalog.size > 1) {
                        Icon(Icons.Default.SkipNext, if (hindi) "अगला" else "Next", tint = YouTubeTextPrimary)
                    }
                }
                IconButton(onClick = player::remove) {
                    Icon(Icons.Default.Close, if (hindi) "संगीत हटाएँ" else "Remove music player", tint = YouTubeTextSecondary)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = YouTubeRed,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

/**
 * Clean 16:9 YouTube-style thumbnail with no tags—only the bottom-right duration pill when requested.
 * Avoids decoding multi-megabyte temple background/idol PNGs underneath HTTP thumbnails so opening
 * the player sheet is instantaneous and never triggers OutOfMemoryError.
 */
@Composable
private fun YouTubeStyleThumbnail(
    track: BhajanPlayer.Track,
    showDuration: Boolean = true,
    useFixedSize: Boolean = false,
    modifier: Modifier = Modifier
) {
    val thumbUrl = remember(track.thumbnail, track.youtubeId) {
        if (track.thumbnail.startsWith("https://") || track.thumbnail.startsWith("http://")) {
            track.thumbnail
        } else if (track.youtubeId.isNotBlank()) {
            "https://i.ytimg.com/vi/${track.youtubeId}/hqdefault.jpg"
        } else {
            ""
        }
    }
    Surface(
        modifier = if (useFixedSize) modifier else modifier.aspectRatio(16f / 9f),
        shape = RoundedCornerShape(if (useFixedSize) 6.dp else 12.dp),
        color = YouTubeElevated
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            HoneyKesariya.copy(alpha = 0.22f),
                            YouTubeBg
                        )
                    )
                )
        ) {
            if (thumbUrl.isNotBlank()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                val fallbackBitmap = rememberCachedAssetImage(track.fallbackAssetArt, sampleSize = 4)
                Image(
                    bitmap = fallbackBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                YouTubeBg.copy(alpha = 0.15f),
                                Color.Transparent,
                                YouTubeBg.copy(alpha = 0.55f)
                            )
                        )
                    )
            )

            if (showDuration && track.durationSeconds > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xE0000000)
                ) {
                    Text(
                        text = musicTime(track.durationSeconds * 1000L),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YouTubeTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BhajanPlayerSheet(player: BhajanPlayer, hindi: Boolean, onDismiss: () -> Unit) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    var searchQuery by remember { mutableStateOf("") }
    // `isTypingSearch` is true while the user is typing in the search bar (shows ONLY text suggestions).
    // Once the user clicks a text suggestion or clicks the Search button, `isTypingSearch` becomes false
    // and the screen shows the list of video items.
    var isTypingSearch by remember { mutableStateOf(false) }
    var committedSearchQuery by remember { mutableStateOf("") }
    var isPlayerMinimizedToSide by remember { mutableStateOf(false) }
    var sidePlayerOnLeft by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { player.fetchCatalog() }

    // Fetch live YouTube autocomplete text suggestions while typing
    LaunchedEffect(searchQuery, isTypingSearch) {
        val q = searchQuery.trim()
        if (q.isNotEmpty() && isTypingSearch) {
            player.fetchYoutubeSuggestions(q)
        } else if (q.isEmpty()) {
            player.clearYoutubeSearch()
        }
    }

    val activeTrack = player.current
        ?: player.catalog.firstOrNull { it.isYoutubeOnly }
        ?: player.catalog.firstOrNull()

    // Dynamic video recommendations list for the active song or committed search query.
    // Strictly excludes the currently playing / active song and reshuffles on every track transition.
    val recommendations = remember(
        activeTrack?.id,
        activeTrack?.title,
        player.catalog,
        player.youtubeSearchResults,
        player.relatedYoutubeTracks,
        player.recentHistoryIds,
        player.recommendationSeed,
        committedSearchQuery
    ) {
        val q = committedSearchQuery.trim()
        val ytPool = if (q.isNotEmpty()) {
            player.youtubeSearchResults
        } else {
            player.relatedYoutubeTracks
        }
        val feed = BhajanSearch.youtubeFeedRecommendations(
            anchor = activeTrack,
            catalog = player.catalog,
            youtubeResults = ytPool,
            searchQuery = q,
            categoryFilter = "all",
            idol = player.idol,
            playedTrackIds = player.recentHistoryIds,
            rotationSeed = player.recommendationSeed
        )
        if (q.isEmpty()) {
            feed.filterNot { BhajanSearch.isSameSong(it, activeTrack) }.distinctBy { it.id }
        } else {
            feed.distinctBy { it.id }
        }
    }

    // Scroll video list to the top whenever the song changes, committed search changes, or live search results arrive
    LaunchedEffect(activeTrack?.id, player.recommendationSeed, committedSearchQuery, player.youtubeSearchResults.size) {
        runCatching { gridState.scrollToItem(0) }
    }

    // Text search suggestions shown ONLY while typing
    val textSuggestions = remember(
        searchQuery,
        hindi,
        activeTrack,
        player.catalog,
        player.youtubeSearchResults,
        player.relatedYoutubeTracks,
        player.youtubeSuggestions
    ) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            val predictions = BhajanSearch.autocompletePredictions(
                query = q,
                hindi = hindi,
                catalog = player.catalog,
                youtubeResults = player.youtubeSearchResults + player.relatedYoutubeTracks,
                remoteSuggestions = player.youtubeSuggestions,
                anchor = activeTrack,
                idol = player.idol
            )
            val nextWords = BhajanSearch.recommendedNextWords(
                query = q,
                hindi = hindi,
                anchor = activeTrack,
                catalog = player.catalog,
                youtubeResults = player.youtubeSearchResults + player.relatedYoutubeTracks,
                idol = player.idol
            ).map { nw ->
                val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }
                val last = tokens.lastOrNull().orEmpty()
                if (nw.startsWith(last, ignoreCase = true) && !nw.equals(last, ignoreCase = true)) {
                    (tokens.dropLast(1) + nw).joinToString(" ")
                } else {
                    "$q $nw"
                }
            }
            (listOf(q) + predictions + nextWords)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(10)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = YouTubeRed,
            onPrimary = Color.White,
            surface = YouTubeBg,
            onSurface = YouTubeTextPrimary,
            surfaceVariant = YouTubeCardBg,
            onSurfaceVariant = YouTubeTextSecondary
        )
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            // Read FocusManager, SoftwareKeyboardController, and View INSIDE the Dialog window scope
            // so keyboard dismissal targets the Dialog's own window token and focus tree.
            val dialogFocusManager = LocalFocusManager.current
            val dialogKeyboardController = LocalSoftwareKeyboardController.current
            val dialogView = LocalView.current

            fun dismissDialogKeyboard() {
                dialogFocusManager.clearFocus(force = true)
                dialogKeyboardController?.hide()
                runCatching {
                    val imm = dialogView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(dialogView.windowToken, 0)
                    imm?.hideSoftInputFromWindow(dialogView.rootView.windowToken, 0)
                }
            }

            fun commitSearch(queryText: String) {
                val clean = queryText.trim()
                dismissDialogKeyboard()
                searchQuery = clean
                committedSearchQuery = clean
                isTypingSearch = false
                if (clean.isNotEmpty()) {
                    if (player.current != null) {
                        isPlayerMinimizedToSide = true
                    }
                    player.searchYoutube(clean, autoplayFirst = false)
                } else {
                    player.clearYoutubeSearch()
                }
                coroutineScope.launch {
                    runCatching { gridState.scrollToItem(0) }
                }
            }

            Surface(Modifier.fillMaxSize(), color = YouTubeBg) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // 1. BIG SEARCH BAR + SINGLE CLOSE BUTTON
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { updated ->
                                    searchQuery = updated
                                    if (updated.isNotBlank()) {
                                        isTypingSearch = true
                                    } else {
                                        isTypingSearch = false
                                        committedSearchQuery = ""
                                        player.clearYoutubeSearch()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .testTag("youtube-search-input"),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = YouTubeTextPrimary
                                ),
                                placeholder = {
                                    Text(
                                        tr("भजन, आरती या गायक खोजें…", "Search bhajans, aarti or artist…"),
                                        fontSize = 15.sp,
                                        color = YouTubeTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    committedSearchQuery = ""
                                                    isTypingSearch = false
                                                    player.clearYoutubeSearch()
                                                    dismissDialogKeyboard()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = tr("साफ़ करें", "Clear"),
                                                    tint = YouTubeTextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (searchQuery.isNotBlank()) YouTubeRed else YouTubeChipBg,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    commitSearch(searchQuery)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = tr("खोजें", "Search"),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        commitSearch(searchQuery)
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YouTubeRed,
                                    unfocusedBorderColor = Color(0xFF3A3A3A),
                                    focusedContainerColor = YouTubeElevated,
                                    unfocusedContainerColor = YouTubeElevated,
                                    focusedTextColor = YouTubeTextPrimary,
                                    unfocusedTextColor = YouTubeTextPrimary
                                )
                            )

                            IconButton(
                                onClick = {
                                    dismissDialogKeyboard()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(YouTubeElevated, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, tr("बंद करें", "Close"), tint = YouTubeTextPrimary)
                            }
                        }

                        // 2. WHILE TYPING: SHOW ONLY TEXT SEARCH RESULTS (Click a text result to show video items)
                        if (isTypingSearch && searchQuery.isNotBlank()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("youtube-text-suggestions-list"),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                val cleanInput = searchQuery.trim()
                                items(textSuggestions, key = { it }) { suggestion ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                commitSearch(suggestion)
                                            }
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = YouTubeTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        val annotated = buildAnnotatedString {
                                            if (suggestion.startsWith(cleanInput, ignoreCase = true) && suggestion.length > cleanInput.length) {
                                                withStyle(SpanStyle(color = YouTubeTextSecondary, fontWeight = FontWeight.Normal)) {
                                                    append(suggestion.substring(0, cleanInput.length))
                                                }
                                                withStyle(SpanStyle(color = YouTubeTextPrimary, fontWeight = FontWeight.Bold)) {
                                                    append(suggestion.substring(cleanInput.length))
                                                }
                                            } else {
                                                withStyle(SpanStyle(color = YouTubeTextPrimary, fontWeight = FontWeight.SemiBold)) {
                                                    append(suggestion)
                                                }
                                            }
                                        }
                                        Text(
                                            text = annotated,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Default.NorthWest,
                                            contentDescription = null,
                                            tint = YouTubeTextSecondary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    searchQuery = "$suggestion "
                                                    isTypingSearch = true
                                                    player.fetchYoutubeSuggestions(suggestion)
                                                }
                                        )
                                    }
                                    HorizontalDivider(color = Color(0x18FFFFFF), thickness = 0.5.dp)
                                }
                            }
                        } else {
                            // 3. FULL 16:9 YOUTUBE VIDEO PLAYER (Collapses when minimized to side)
                            AnimatedVisibility(
                                visible = activeTrack != null && !isPlayerMinimizedToSide,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                if (activeTrack != null) {
                                    UnifiedYouTubeStylePlayer(
                                        player = player,
                                        track = activeTrack,
                                        hindi = hindi,
                                        onMinimizeToSide = {
                                            dismissDialogKeyboard()
                                            isPlayerMinimizedToSide = true
                                        }
                                    )
                                }
                            }

                            if (player.catalogLoading || player.youtubeSearching) {
                                LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = YouTubeRed)
                            }
                            if (player.catalogError && recommendations.isEmpty()) {
                                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("संग्रह लोड नहीं हुआ", "Couldn’t load music"), Modifier.weight(1f))
                                    TextButton(onClick = player::fetchCatalog) { Text(tr("फिर प्रयास करें", "Retry")) }
                                }
                            }

                            // 4. LIST OF VIDEO ITEMS (Recommendations / Search Results)
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Adaptive(260.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                                contentPadding = PaddingValues(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 8.dp,
                                    bottom = if (isPlayerMinimizedToSide) 104.dp else 16.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(recommendations, key = { it.id }) { track ->
                                    val isCurrent = player.current?.id == track.id
                                    YouTubeBhajanFeedCard(
                                        track = track,
                                        isNowPlaying = isCurrent,
                                        hindi = hindi,
                                        onClick = {
                                            dismissDialogKeyboard()
                                            if (searchQuery.isNotBlank() || committedSearchQuery.isNotBlank()) {
                                                searchQuery = ""
                                                committedSearchQuery = ""
                                                isTypingSearch = false
                                                player.clearYoutubeSearch()
                                            }
                                            isPlayerMinimizedToSide = false
                                            player.playCatalog(
                                                selected = track,
                                                autoplay = true,
                                                preferYoutube = true
                                            )
                                            coroutineScope.launch {
                                                runCatching { gridState.scrollToItem(0) }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. FLOATING YOUTUBE SIDE MINI-PLAYER (When player is minimized to the side)
                    AnimatedVisibility(
                        visible = activeTrack != null && isPlayerMinimizedToSide && !isTypingSearch,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(if (sidePlayerOnLeft) Alignment.BottomStart else Alignment.BottomEnd)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        if (activeTrack != null) {
                            YouTubeSideMiniPlayer(
                                player = player,
                                track = activeTrack,
                                hindi = hindi,
                                dockedLeft = sidePlayerOnLeft,
                                onToggleSide = { sidePlayerOnLeft = !sidePlayerOnLeft },
                                onExpand = { isPlayerMinimizedToSide = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Floating YouTube Side Mini-Player when the main 16:9 player is minimized to the side.
 * Tapping the card or thumbnail expands it back; includes only Play/Pause, Next, and Close.
 */
@Composable
private fun YouTubeSideMiniPlayer(
    player: BhajanPlayer,
    track: BhajanPlayer.Track,
    hindi: Boolean,
    dockedLeft: Boolean,
    onToggleSide: () -> Unit,
    onExpand: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val effectiveDuration = player.duration.takeIf { it > 0L } ?: (track.durationSeconds * 1000L)
    val progress = if (effectiveDuration > 0L) {
        (player.position.toFloat() / effectiveDuration.toFloat()).takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    } else 0f

    Surface(
        modifier = Modifier
            .widthIn(min = 250.dp, max = 310.dp)
            .shadow(18.dp, RoundedCornerShape(12.dp))
            .pointerInput(dockedLeft) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -24f && !dockedLeft) onToggleSide()
                    else if (dragAmount > 24f && dockedLeft) onToggleSide()
                }
            }
            .clickable(onClick = onExpand)
            .testTag("youtube-side-mini-player"),
        shape = RoundedCornerShape(12.dp),
        color = YouTubeElevated,
        border = BorderStroke(1.dp, Color(0x44FFFFFF))
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(width = 96.dp, height = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YouTubeBg)
                ) {
                    YouTubeStyleThumbnail(
                        track = track,
                        showDuration = false,
                        useFixedSize = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = if (player.current != null) player.title else track.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = YouTubeTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (player.current == null) {
                            player.playCatalog(track, autoplay = true, preferYoutube = true)
                        } else {
                            player.toggle()
                        }
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (player.failed) Icons.Default.Refresh
                        else if (player.playing) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = tr("चलाएँ / रोकें", "Play / pause"),
                        tint = YouTubeTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { player.skip(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = tr("अगला", "Next"),
                        tint = YouTubeTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = player::remove,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = tr("बंद करें", "Close"),
                        tint = YouTubeTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = YouTubeRed,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

/**
 * Clean YouTube video item card with no extra tags or repeated buttons.
 */
@Composable
private fun YouTubeBhajanFeedCard(
    track: BhajanPlayer.Track,
    isNowPlaying: Boolean,
    hindi: Boolean,
    onClick: () -> Unit
) {
    val cleanArtist = track.artist
        .replace("• YouTube", "")
        .trim()
        .ifBlank { "YouTube Bhakti" }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) Color(0xFF261818) else YouTubeCardBg,
        border = if (isNowPlaying) BorderStroke(1.2.dp, YouTubeRed) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = track.url.isNotBlank(), onClick = onClick)
    ) {
        Column {
            YouTubeStyleThumbnail(
                track = track,
                showDuration = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YouTubeBadgeIcon(hindi = hindi)

                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = YouTubeTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "$cleanArtist • YouTube",
                        fontSize = 12.sp,
                        color = YouTubeTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full 16:9 YouTube Watch Player with a single top-left Minimize icon (`⌄`),
 * center transport controls, red seekbar, and clean title row below.
 */
@Composable
private fun UnifiedYouTubeStylePlayer(
    player: BhajanPlayer,
    track: BhajanPlayer.Track,
    hindi: Boolean,
    onMinimizeToSide: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    var dragPosition by remember(track.id) { mutableStateOf<Float?>(null) }
    val effectiveDuration = player.duration.takeIf { it > 0 } ?: (track.durationSeconds * 1000L)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        // 16:9 YouTube Video Stage (tap top-left `⌄` or swipe down to minimize to side)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(YouTubeBg)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 28f) onMinimizeToSide()
                    }
                }
        ) {
            YouTubeStyleThumbnail(
                track = track,
                showDuration = false,
                modifier = Modifier.fillMaxSize()
            )

            // Single top-left Minimize chevron (`⌄`) + single top-right Repeat icon
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinimizeToSide,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x99000000), CircleShape)
                        .testTag("youtube-minimize-chevron")
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = tr("प्लेयर छोटा करें", "Minimize player"),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = { player.toggleRepeat() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(
                        Icons.Default.RepeatOne,
                        contentDescription = tr("दोहराएँ", "Repeat"),
                        tint = if (player.repeat) YouTubeRed else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Center Transport Controls Overlay (SkipPrevious | Play/Pause | SkipNext)
            Row(
                Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { player.skip(-1) },
                    enabled = player.tracks.isNotEmpty() || player.catalog.isNotEmpty(),
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(Icons.Default.SkipPrevious, tr("पिछला", "Previous"), tint = Color.White)
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xB3000000),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable {
                            if (player.current == null) {
                                player.playCatalog(track, autoplay = true, preferYoutube = true)
                            } else {
                                player.toggle()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (player.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = YouTubeRed,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (player.failed) Icons.Default.Refresh
                                else if (player.playing) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = tr("चलाएँ / रोकें", "Play / pause"),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { player.skip(1) },
                    enabled = player.tracks.isNotEmpty() || player.catalog.isNotEmpty(),
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x99000000), CircleShape)
                ) {
                    Icon(Icons.Default.SkipNext, tr("अगला", "Next"), tint = Color.White)
                }
            }

            // Bottom Timestamp + YouTube Red Seekbar
            Column(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xD9000000))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    "${musicTime((dragPosition?.toLong() ?: player.position))} / ${musicTime(effectiveDuration)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                val maxSliderVal = effectiveDuration.coerceAtLeast(1L).toFloat()
                val currentSliderVal = (dragPosition ?: player.position.toFloat())
                    .takeIf { it.isFinite() }
                    ?.coerceIn(0f, maxSliderVal)
                    ?: 0f
                Slider(
                    value = currentSliderVal,
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { player.seek(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..maxSliderVal,
                    enabled = effectiveDuration > 0 && !player.failed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = YouTubeRed,
                        activeTrackColor = YouTubeRed,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Clean YouTube Watch-Page Video Title & Channel Row (No extra written buttons)
        val cleanArtist = track.artist
            .replace("• YouTube", "")
            .trim()
            .ifBlank { "YouTube Bhakti" }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            YouTubeBadgeIcon(hindi = hindi)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (player.current != null) player.title else track.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = YouTubeTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$cleanArtist • YouTube",
                    fontSize = 12.sp,
                    color = YouTubeTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun musicTime(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
