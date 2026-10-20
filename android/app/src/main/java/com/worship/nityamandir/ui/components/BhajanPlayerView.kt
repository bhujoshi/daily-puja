package com.worship.nityamandir.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
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
import java.util.Locale

// Authentic YouTube Dark App Palette
private val YouTubeBg = Color(0xFF0F0F0F)
private val YouTubeElevated = Color(0xFF181818)
private val YouTubeCardBg = Color(0xFF1E1E1E)
private val YouTubeChipBg = Color(0xFF272727)
private val YouTubeChipSelected = Color(0xFFF1F1F1)
private val YouTubeRed = Color(0xFFFF0033)
private val YouTubeTextPrimary = Color(0xFFF1F1F1)
private val YouTubeTextSecondary = Color(0xFFAAAAAA)
private val TempleBrass = Color(0xFFD8AF52)
private val HoneyKesariya = Color(0xFFFF9E2C)

/**
 * Authentic YouTube play-button badge icon used across the YouTube player, channel avatars,
 * and recommendation feed.
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
 * Features a side 16:9 thumbnail preview, track title, Play/Pause, Skip Next, Close,
 * and a signature YouTube Red bottom progress bar.
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
        (player.position.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
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
                    // Side 16:9 mini video thumbnail like YouTube Miniplayer
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
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        YouTubeBadgeIcon(
                            hindi = hindi,
                            compact = true,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 18.dp, height = 13.dp)
                        )
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
                    IconButton(onClick = { player.skip(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.SkipNext,
                            if (hindi) "अगला" else "Next",
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
 * Clean 16:9 YouTube-style thumbnail used across the video player, side mini-player, and recommendation cards.
 */
@Composable
private fun YouTubeStyleThumbnail(
    track: BhajanPlayer.Track,
    showDuration: Boolean = true,
    isNowPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(16f / 9f),
        shape = RoundedCornerShape(12.dp),
        color = YouTubeElevated
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = "file:///android_asset/shrine/backgrounds/carved.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.45f
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                HoneyKesariya.copy(alpha = 0.25f),
                                YouTubeBg.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
            AsyncImage(
                model = "file:///android_asset/${track.fallbackAssetArt}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            if (track.thumbnail.startsWith("https://") || track.thumbnail.startsWith("http://")) {
                AsyncImage(
                    model = track.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                YouTubeBg.copy(alpha = 0.2f),
                                Color.Transparent,
                                YouTubeBg.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            if (isNowPlaying) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = YouTubeRed
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "NOW PLAYING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

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

private data class FilterChipOption(val id: String, val labelHi: String, val labelEn: String)

private val youtubeFilterChips = listOf(
    FilterChipOption("all", "सभी (All)", "All"),
    FilterChipOption("up_next", "अगला (Up next)", "Up next"),
    FilterChipOption("hanuman", "हनुमान", "Hanuman"),
    FilterChipOption("shiva", "शिव", "Shiva"),
    FilterChipOption("ganesh", "गणेश", "Ganesh"),
    FilterChipOption("rama", "श्री राम", "Shri Ram"),
    FilterChipOption("krishna", "कृष्ण", "Krishna"),
    FilterChipOption("durga", "माता दुर्गा", "Durga"),
    FilterChipOption("aarti", "आरती", "Aarti"),
    FilterChipOption("chalisa", "चालीसा", "Chalisa")
)

@Composable
fun BhajanPlayerSheet(player: BhajanPlayer, hindi: Boolean, onDismiss: () -> Unit) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var isPlayerMinimizedToSide by remember { mutableStateOf(false) }
    var sidePlayerOnLeft by remember { mutableStateOf(false) }
    var showAutocompleteDropdown by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val localView = LocalView.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    fun dismissKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        runCatching {
            val imm = localView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(localView.windowToken, 0)
        }
    }

    LaunchedEffect(Unit) { player.fetchCatalog() }

    // Live debounced YouTube search + live YouTube autocomplete suggestions when typing
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            player.fetchYoutubeSuggestions(q)
            if (q.length >= 2) {
                kotlinx.coroutines.delay(300)
                player.searchYoutube(q, autoplayFirst = false)
            }
        } else {
            player.clearYoutubeSearch()
        }
    }

    // YouTube-first recommendations for the currently playing song (or active search query).
    // Whenever the next song is played, `player.current` updates and this list immediately
    // recomputes to show YouTube recommendations for the newly playing song.
    val recommendations = remember(
        player.current,
        player.catalog,
        player.youtubeSearchResults,
        player.relatedYoutubeTracks,
        searchQuery,
        selectedCategory
    ) {
        val q = searchQuery.trim()
        val ytPool = if (q.isNotEmpty()) {
            (player.youtubeSearchResults + player.relatedYoutubeTracks).distinctBy { it.id }
        } else {
            player.relatedYoutubeTracks
        }
        BhajanSearch.youtubeFeedRecommendations(
            anchor = player.current,
            catalog = player.catalog,
            youtubeResults = ytPool,
            searchQuery = q,
            categoryFilter = selectedCategory,
            idol = player.idol
        )
    }

    val activeTrack = player.current ?: recommendations.firstOrNull() ?: player.catalog.firstOrNull()
    val nextRecommendedTrack = recommendations.firstOrNull { it.id != activeTrack?.id }

    // Whenever the currently playing song changes (e.g., Next song button clicked, auto-advance, or card tap),
    // scroll the YouTube recommendation feed to the top so fresh YouTube recommendations are visible right away.
    LaunchedEffect(player.current?.id) {
        if (player.current != null) {
            showAutocompleteDropdown = false
            runCatching { gridState.animateScrollToItem(0) }
        }
    }

    // Recommended Next Words (like YouTube search next-word completions)
    val recommendedNextWords = remember(
        searchQuery,
        hindi,
        activeTrack,
        player.catalog,
        player.youtubeSearchResults,
        player.relatedYoutubeTracks
    ) {
        BhajanSearch.recommendedNextWords(
            query = searchQuery,
            hindi = hindi,
            anchor = activeTrack,
            catalog = player.catalog,
            youtubeResults = player.youtubeSearchResults + player.relatedYoutubeTracks,
            idol = player.idol
        )
    }

    // Full YouTube Autocomplete Prediction rows while typing
    val autocompletePredictions = remember(
        searchQuery,
        hindi,
        activeTrack,
        player.catalog,
        player.youtubeSearchResults,
        player.relatedYoutubeTracks,
        player.youtubeSuggestions
    ) {
        BhajanSearch.autocompletePredictions(
            query = searchQuery,
            hindi = hindi,
            catalog = player.catalog,
            youtubeResults = player.youtubeSearchResults + player.relatedYoutubeTracks,
            remoteSuggestions = player.youtubeSuggestions,
            anchor = activeTrack,
            idol = player.idol
        )
    }

    fun applyNextWord(word: String) {
        val currentTrimmed = searchQuery.trim()
        val newQuery = if (currentTrimmed.isEmpty()) {
            word
        } else {
            val tokens = currentTrimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
            val lastToken = tokens.lastOrNull()?.lowercase(Locale.ROOT).orEmpty()
            val wordLower = word.lowercase(Locale.ROOT)
            if (wordLower.startsWith(lastToken) && wordLower != lastToken) {
                (tokens.dropLast(1) + word).joinToString(" ")
            } else if (tokens.any { it.equals(word, ignoreCase = true) }) {
                currentTrimmed
            } else {
                "$currentTrimmed $word"
            }
        }
        searchQuery = newQuery
        showAutocompleteDropdown = true
        player.searchYoutube(newQuery, autoplayFirst = false)
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
            onDismissRequest = {
                dismissKeyboard()
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = YouTubeBg) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // 1. YOUTUBE APP TOP HEADER + SEARCH BAR
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // YouTube Brand Logo / Back button when searching
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        showAutocompleteDropdown = false
                                        player.clearYoutubeSearch()
                                        dismissKeyboard()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = tr("वापस", "Back"),
                                        tint = YouTubeTextPrimary
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isPlayerMinimizedToSide = false
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    YouTubeBadgeIcon(hindi = hindi, compact = true)
                                    Text(
                                        text = "YouTube",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = YouTubeTextPrimary,
                                        letterSpacing = (-0.3).sp
                                    )
                                }
                            }

                            // YouTube Rounded Search Input Box
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    showAutocompleteDropdown = it.isNotBlank()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp),
                                placeholder = {
                                    Text(
                                        tr("YouTube भजन, आरती खोजें…", "Search YouTube bhajans, aarti…"),
                                        fontSize = 13.sp,
                                        color = YouTubeTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    showAutocompleteDropdown = false
                                                    player.clearYoutubeSearch()
                                                    dismissKeyboard()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = tr("साफ़ करें", "Clear"),
                                                    tint = YouTubeTextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                dismissKeyboard()
                                                showAutocompleteDropdown = false
                                                if (searchQuery.isNotBlank()) {
                                                    player.searchYoutube(searchQuery, autoplayFirst = false)
                                                }
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = tr("खोजें", "Search"),
                                                tint = YouTubeTextPrimary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        dismissKeyboard()
                                        showAutocompleteDropdown = false
                                        if (searchQuery.isNotBlank()) {
                                            player.searchYoutube(searchQuery, autoplayFirst = false)
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3EA6FF),
                                    unfocusedBorderColor = Color(0xFF303030),
                                    focusedContainerColor = YouTubeElevated,
                                    unfocusedContainerColor = YouTubeElevated,
                                    focusedTextColor = YouTubeTextPrimary,
                                    unfocusedTextColor = YouTubeTextPrimary
                                )
                            )

                            // Minimize / Expand Player to Side toggle button in header
                            IconButton(
                                onClick = {
                                    dismissKeyboard()
                                    isPlayerMinimizedToSide = !isPlayerMinimizedToSide
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("youtube-minimize-side-toggle")
                            ) {
                                Icon(
                                    imageVector = if (isPlayerMinimizedToSide) Icons.Default.OpenInFull else Icons.Default.PictureInPictureAlt,
                                    contentDescription = if (isPlayerMinimizedToSide) {
                                        tr("प्लेयर बड़ा करें", "Expand player")
                                    } else {
                                        tr("प्लेयर साइड में छोटा करें", "Minimize player to side")
                                    },
                                    tint = if (isPlayerMinimizedToSide) YouTubeRed else YouTubeTextPrimary
                                )
                            }

                            IconButton(
                                onClick = {
                                    dismissKeyboard()
                                    onDismiss()
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Close, tr("बंद करें", "Close"), tint = YouTubeTextPrimary)
                            }
                        }

                        // 2. YOUTUBE RECOMMENDED NEXT WORD BAR (Dynamic next-word suggestions like YouTube)
                        RecommendedNextWordBar(
                            searchQuery = searchQuery,
                            recommendedNextWords = recommendedNextWords,
                            hindi = hindi,
                            onNextWordClick = { word ->
                                applyNextWord(word)
                            }
                        )

                        // 3. YOUTUBE AUTOCOMPLETE PREDICTIONS DROPDOWN (When typing in search bar)
                        AnimatedVisibility(
                            visible = showAutocompleteDropdown && searchQuery.isNotBlank() && autocompletePredictions.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            YouTubeAutocompleteDropdown(
                                currentQuery = searchQuery,
                                predictions = autocompletePredictions,
                                hindi = hindi,
                                onSelectPrediction = { phrase ->
                                    searchQuery = phrase
                                    showAutocompleteDropdown = false
                                    dismissKeyboard()
                                    player.searchYoutube(phrase, autoplayFirst = false)
                                },
                                onInsertPrediction = { phrase ->
                                    searchQuery = "$phrase "
                                    player.fetchYoutubeSuggestions(phrase)
                                    player.searchYoutube(phrase, autoplayFirst = false)
                                }
                            )
                        }

                        // 4. FULL 16:9 YOUTUBE VIDEO PLAYER (Collapses when minimized to side)
                        AnimatedVisibility(
                            visible = activeTrack != null && !isPlayerMinimizedToSide,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            if (activeTrack != null) {
                                UnifiedYouTubeStylePlayer(
                                    player = player,
                                    track = activeTrack,
                                    nextTrack = nextRecommendedTrack,
                                    hindi = hindi,
                                    onMinimizeToSide = {
                                        dismissKeyboard()
                                        isPlayerMinimizedToSide = true
                                    }
                                )
                            }
                        }

                        // 5. YOUTUBE CATEGORY FILTER CHIPS ROW (All, Up next, Hanuman, Shiva, Ganesh, etc.)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            youtubeFilterChips.forEach { chip ->
                                val selected = selectedCategory == chip.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) YouTubeChipSelected else YouTubeChipBg,
                                    contentColor = if (selected) YouTubeBg else YouTubeTextPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedCategory = chip.id
                                            coroutineScope.launch {
                                                runCatching { gridState.scrollToItem(0) }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = if (hindi) chip.labelHi else chip.labelEn,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
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

                        // 6. YOUTUBE RECOMMENDATIONS / UP NEXT FEED
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(260.dp),
                            modifier = Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 4.dp,
                                bottom = if (isPlayerMinimizedToSide) 116.dp else 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // YouTube "Up next" section header
                            if (recommendations.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "youtube_up_next_header") {
                                    YouTubeUpNextHeader(
                                        nextTrack = nextRecommendedTrack,
                                        hindi = hindi,
                                        onPlayNext = {
                                            dismissKeyboard()
                                            showAutocompleteDropdown = false
                                            player.skip(1)
                                        }
                                    )
                                }
                            }

                            itemsIndexed(recommendations, key = { _, track -> track.id }) { index, track ->
                                val isCurrent = player.current?.id == track.id
                                val isUpNextFirst = index == 0 && !isCurrent
                                YouTubeBhajanFeedCard(
                                    track = track,
                                    isNowPlaying = isCurrent,
                                    isUpNext = isUpNextFirst,
                                    hindi = hindi,
                                    onClick = {
                                        dismissKeyboard()
                                        showAutocompleteDropdown = false
                                        if (searchQuery.isNotBlank()) {
                                            searchQuery = ""
                                            player.clearYoutubeSearch()
                                        }
                                        player.playCatalog(
                                            selected = track,
                                            autoplay = true,
                                            preferYoutube = true
                                        )
                                        coroutineScope.launch {
                                            runCatching { gridState.animateScrollToItem(0) }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 7. FLOATING YOUTUBE SIDE MINI-PLAYER (When player is minimized to the side)
                    AnimatedVisibility(
                        visible = activeTrack != null && isPlayerMinimizedToSide,
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
                                nextTrack = nextRecommendedTrack,
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
 * YouTube-style "Recommended Next Word" bar right below the search input.
 * Shows predicted next words as the user types (e.g. "hanuman" -> "+ chalisa", "+ ashtak", "+ aarti")
 * or starter devotional search phrases when the search bar is empty.
 */
@Composable
private fun RecommendedNextWordBar(
    searchQuery: String,
    recommendedNextWords: List<String>,
    hindi: Boolean,
    onNextWordClick: (String) -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    if (recommendedNextWords.isEmpty()) return
    val isTyping = searchQuery.isNotBlank()

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("youtube-next-word-bar"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = YouTubeRed.copy(alpha = 0.16f),
            border = BorderStroke(0.8.dp, YouTubeRed.copy(alpha = 0.45f))
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = YouTubeRed,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isTyping) tr("अगला शब्द:", "Next word:") else tr("सुझाव:", "Suggested:"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = YouTubeTextPrimary
                )
            }
        }

        recommendedNextWords.forEach { word ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = YouTubeChipBg,
                border = BorderStroke(0.8.dp, Color(0x44FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNextWordClick(word) }
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isTyping) Icons.Default.Add else Icons.Default.Search,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = word,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YouTubeTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * YouTube search autocomplete predictions list with bolded recommended next words
 * and the `NorthWest` (`↖`) button to fill the word into the search bar without closing.
 */
@Composable
private fun YouTubeAutocompleteDropdown(
    currentQuery: String,
    predictions: List<String>,
    hindi: Boolean,
    onSelectPrediction: (String) -> Unit,
    onInsertPrediction: (String) -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val cleanQuery = currentQuery.trim()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = YouTubeElevated,
        border = BorderStroke(0.8.dp, Color(0x33FFFFFF)),
        shadowElevation = 10.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            predictions.take(5).forEach { prediction ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrediction(prediction) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = YouTubeTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    val annotated = buildAnnotatedString {
                        if (prediction.startsWith(cleanQuery, ignoreCase = true) && prediction.length > cleanQuery.length) {
                            withStyle(SpanStyle(color = YouTubeTextSecondary, fontWeight = FontWeight.Normal)) {
                                append(prediction.substring(0, cleanQuery.length))
                            }
                            withStyle(SpanStyle(color = YouTubeTextPrimary, fontWeight = FontWeight.Bold)) {
                                append(prediction.substring(cleanQuery.length))
                            }
                        } else {
                            withStyle(SpanStyle(color = YouTubeTextPrimary, fontWeight = FontWeight.SemiBold)) {
                                append(prediction)
                            }
                        }
                    }
                    Text(
                        text = annotated,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onInsertPrediction(prediction) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.NorthWest,
                            contentDescription = tr("शब्द जोड़ें", "Insert recommended word"),
                            tint = YouTubeTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * YouTube "Up next" header bar shown right above the recommendation cards.
 */
@Composable
private fun YouTubeUpNextHeader(
    nextTrack: BhajanPlayer.Track?,
    hindi: Boolean,
    onPlayNext: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            YouTubeBadgeIcon(hindi = hindi, compact = true)
            Column {
                Text(
                    text = tr("अगला भजन · YouTube सुझाव", "Up next · YouTube Recommendations"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YouTubeTextPrimary
                )
                if (nextTrack != null) {
                    Text(
                        text = "${tr("अगला:", "Next:")} ${nextTrack.title}",
                        fontSize = 11.sp,
                        color = YouTubeTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (nextTrack != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = YouTubeChipBg,
                border = BorderStroke(0.8.dp, Color(0x44FFFFFF)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onPlayNext)
                    .testTag("youtube-play-next-button")
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tr("अगला चलाएँ", "Play next"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YouTubeTextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Floating YouTube Side Mini-Player (Picture-in-Picture style) when the main 16:9 player is
 * minimized to the side while browsing recommendations or searching.
 */
@Composable
private fun YouTubeSideMiniPlayer(
    player: BhajanPlayer,
    track: BhajanPlayer.Track,
    nextTrack: BhajanPlayer.Track?,
    hindi: Boolean,
    dockedLeft: Boolean,
    onToggleSide: () -> Unit,
    onExpand: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val effectiveDuration = player.duration.takeIf { it > 0L } ?: (track.durationSeconds * 1000L)
    val progress = if (effectiveDuration > 0L) {
        (player.position.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .widthIn(min = 270.dp, max = 330.dp)
            .shadow(18.dp, RoundedCornerShape(14.dp))
            .pointerInput(dockedLeft) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -24f && !dockedLeft) onToggleSide()
                    else if (dragAmount > 24f && dockedLeft) onToggleSide()
                }
            }
            .testTag("youtube-side-mini-player"),
        shape = RoundedCornerShape(14.dp),
        color = YouTubeElevated,
        border = BorderStroke(1.dp, Color(0x44FFFFFF))
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 16:9 Mini Video Screen on the side
                Box(
                    Modifier
                        .size(width = 112.dp, height = 63.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(YouTubeBg)
                        .clickable(onClick = onExpand)
                ) {
                    YouTubeStyleThumbnail(
                        track = track,
                        showDuration = false,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xAA000000),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = tr("प्लेयर बड़ा करें", "Expand player"),
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    YouTubeBadgeIcon(
                        hindi = hindi,
                        compact = true,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(width = 18.dp, height = 13.dp)
                    )
                }

                // Song info + compact YouTube transport controls
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (player.current != null) player.title else track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = YouTubeTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onExpand)
                    )
                    if (nextTrack != null) {
                        Text(
                            text = "${tr("अगला:", "Next:")} ${nextTrack.title}",
                            fontSize = 10.sp,
                            color = YouTubeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(onClick = onToggleSide, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = tr("साइड बदलें", "Switch side"),
                                tint = YouTubeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (player.current == null) {
                                    player.playCatalog(track, autoplay = true, preferYoutube = true)
                                } else {
                                    player.toggle()
                                }
                            },
                            modifier = Modifier.size(32.dp)
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
                            onClick = onExpand,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = tr("बड़ा करें", "Expand"),
                                tint = YouTubeTextPrimary,
                                modifier = Modifier.size(16.dp)
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
 * Authentic YouTube recommendation card in the watch feed.
 * Single click plays the song and refreshes the YouTube recommendation feed.
 */
@Composable
private fun YouTubeBhajanFeedCard(
    track: BhajanPlayer.Track,
    isNowPlaying: Boolean,
    isUpNext: Boolean,
    hindi: Boolean,
    onClick: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val cleanArtist = track.artist
        .replace("• YouTube", "")
        .trim()
        .ifBlank { "YouTube Bhakti" }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isNowPlaying) Color(0xFF261818) else YouTubeCardBg,
        border = if (isNowPlaying) BorderStroke(1.2.dp, YouTubeRed)
        else if (isUpNext) BorderStroke(0.8.dp, Color(0x44FFFFFF))
        else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = track.url.isNotBlank(), onClick = onClick)
    ) {
        Column {
            Box {
                YouTubeStyleThumbnail(
                    track = track,
                    showDuration = true,
                    isNowPlaying = isNowPlaying,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isUpNext && !isNowPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC0F0F0F),
                        border = BorderStroke(0.6.dp, YouTubeRed.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = tr("अगला भजन · UP NEXT", "UP NEXT"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YouTubeTextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

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
 * Full 16:9 YouTube Watch Player with top Minimize-to-Side controls, center transport controls,
 * red YouTube seekbar, and watch-page action pills below the video.
 */
@Composable
private fun UnifiedYouTubeStylePlayer(
    player: BhajanPlayer,
    track: BhajanPlayer.Track,
    nextTrack: BhajanPlayer.Track?,
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
        // 16:9 YouTube Video Stage (supports swipe-down to minimize to side like YouTube app)
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

            // Top YouTube Overlay Bar: Minimize Chevron (left) + Autoplay & Side PiP (right)
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
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = tr("प्लेयर साइड में छोटा करें", "Minimize player to side"),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x99000000),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onMinimizeToSide)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureInPictureAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tr("साइड प्लेयर", "Miniplayer"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${musicTime((dragPosition?.toLong() ?: player.position))} / ${musicTime(effectiveDuration)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    if (nextTrack != null) {
                        Text(
                            text = "${tr("अगला:", "Up next:")} ${nextTrack.title}",
                            fontSize = 10.sp,
                            color = YouTubeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clickable { player.skip(1) }
                        )
                    }
                }
                Slider(
                    value = (dragPosition ?: player.position.toFloat()).coerceIn(
                        0f,
                        effectiveDuration.coerceAtLeast(1L).toFloat()
                    ),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { player.seek(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..effectiveDuration.coerceAtLeast(1L).toFloat(),
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

        // YouTube Watch-Page Video Title, Channel & Action Pills Row
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

            // Quick YouTube Action Pill: Minimize to Side
            Surface(
                shape = RoundedCornerShape(50),
                color = YouTubeChipBg,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onMinimizeToSide)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PictureInPictureAlt,
                        contentDescription = null,
                        tint = YouTubeTextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = tr("साइड में करें", "Minimize"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YouTubeTextPrimary
                    )
                }
            }
        }
    }
}

private fun musicTime(ms: Long) = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
