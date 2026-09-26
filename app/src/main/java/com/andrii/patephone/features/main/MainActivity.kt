@file:Suppress("FunctionName")

package com.andrii.patephone.features.main

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.window.core.layout.WindowSizeClass
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.andrii.patephone.core.player.MusicServiceConnection
import com.andrii.patephone.core.player.PlayerAction
import com.andrii.patephone.core.ui.theme.ApplicationTheme
import com.andrii.patephone.features.settings.SettingsActivity
import com.andrii.patephone.features.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider as WavySlider3
import java.util.concurrent.TimeUnit

val showTimeAboveSlider = MutableStateFlow(false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                Scaffold { innerPadding ->
                    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
                    val isSmartphone = isSmartphone(windowSizeClass)

                    requestedOrientation =
                        if (isSmartphone) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED


                    if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
                        MainHorizontalContent(
                            Modifier.padding(innerPadding),
                            { openSettings(this) }
                        )
                    }
                    MainContent(
                        Modifier.padding(innerPadding),
                        { openSettings(this) }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicServiceConnection.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        MusicServiceConnection.stopTracking()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            showTimeAboveSlider.value = SettingsManager(applicationContext).showTimeAboveSlider.first()
        }
        MusicServiceConnection.lazyEnrichment(this.applicationContext)
        MusicServiceConnection.startTracking()
    }

    fun openSettings(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun isSmartphone(windowSizeClass: WindowSizeClass): Boolean {
    val isCompactWidth =
        !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isCompactHeight =
        !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    return isCompactWidth || isCompactHeight
}

@Composable
fun MainHorizontalContent(
    modifier: Modifier,
    onSettingsPressed: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    val song = viewModel.song.collectAsStateWithLifecycle().value
    val playerState = viewModel.playerState.collectAsStateWithLifecycle().value

    Row(
        modifier = modifier
            .padding(32.dp, 72.dp, 32.dp, 72.dp)
            .fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ArtworkFrame(
                onActionImport = { uri ->
                    viewModel.onActionImport(
                        uri,
                    )
                },
                song.artworkUri,
                song.artist
            )

            TitleFrame(song.title, onClick = {})
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Slider(
                onAction = { float -> viewModel.onSliderMove(float) },
                currentTime = playerState.currentTime,
                songDuration = playerState.currentSongDuration
            )
            ButtonRow(
                onAction = { action -> viewModel.onAction(action) },
                isPlaying = playerState.isPlaying,
                isShuffleEnabled = playerState.isShuffleEnabled,
                repeatMode = playerState.repeatMode
            )
            Row(
                Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { onSettingsPressed() }) {
                    Icon(Icons.Default.Settings, null)
                }
            }
            Playlist(
                playlist,
                { index ->
                    viewModel.seekToMedia(index)
                }, playerState.currentSongIndex
            )
        }
    }
}

/*
@Preview(showBackground = true)
@Composable
fun HorizontalContent() {
    ApplicationTheme {
        Surface(
            modifier = Modifier.size(1000.dp, 860.dp)
        ) {
            MainHorizontalContent(
                modifier = Modifier,
                onSettingsPressed = {}
            )
        }
    }
}

*/
@Preview(
    showBackground = true,
    widthDp = 840,
    heightDp = 450
)
@Composable
fun VerticalPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp, 72.dp, 32.dp, 72.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            )
            {
                ArtworkFrame(onActionImport = {}, null, "some artist")
                Slider({}, 0, 0)
                ButtonRow(
                    {},
                    isPlaying = false,
                    isShuffleEnabled = true,
                    repeatMode = 0
                )
                TitleFrame("Nothing") {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    modifier: Modifier,
    onSettingsPressed: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    showTimeAboveSlider.value = viewModel.getShowTimeAboveSlider()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    val song = viewModel.song.collectAsStateWithLifecycle().value
    val playerState = viewModel.playerState.collectAsStateWithLifecycle().value

    Column(
        modifier = modifier
            .padding(32.dp, 72.dp, 32.dp, 72.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    )
    {
        ArtworkFrame(
            onActionImport = { uri -> viewModel.onActionImport(uri) },
            song.artworkUri,
            song.artist
        )
        Slider(
            onAction = { float -> viewModel.onSliderMove(float) },
            songDuration = playerState.currentSongDuration,
            currentTime = playerState.currentTime
        )
        ButtonRow(
            onAction = { action -> viewModel.onAction(action) },
            isPlaying = playerState.isPlaying,
            isShuffleEnabled = playerState.isShuffleEnabled,
            repeatMode = playerState.repeatMode
        )
        TitleFrame(song.title, onClick = {
            showBottomSheet = true
        })
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().height(90.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { onSettingsPressed() }) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
                if (playlist.isNotEmpty()) {
                    Playlist(
                        playlist,
                        { index ->
                            viewModel.seekToMedia(index)
                        }, playerState.currentSongIndex
                    )
                }
            }
        }
    }
}


@Composable
fun TextUnderArtwork(onActionImport: (Uri?) -> Unit, artist: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            artist,
            Modifier
                .weight(1f)
                .padding(12.dp, 0.dp, 12.dp, 0.dp),
            maxLines = 1
        )
        ImportButton(onActionImport)
    }
}

@Composable
fun ImportButton(onActionImport: (Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onActionImport(it) }
    }
    Text(
        "Import",
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.clickable {
            launcher.launch(null)
        })
}

@Composable
fun Playlist(
    playlist: Array<String>,
    onAction: (index: Int) -> Unit,
    currentSongIndex: Int
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(playlist) { index, song ->
            Text(
                text = song,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable {
                        onAction(index)
                    }
                    .then(
                        if (index == currentSongIndex) {
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp, 0.dp)
                        } else {
                            Modifier
                        }
                    ),
                color = if (index == currentSongIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
fun ArtworkFrame(onActionImport: (Uri?) -> Unit, artworkUri: Uri?, artist: String) {
    Box(
        Modifier
            .padding(24.dp, 24.dp, 24.dp, 0.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp)),
            contentScale = ContentScale.Crop
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                SubcomposeAsyncImageContent()
            }
        }
    }
    TextUnderArtwork(onActionImport, artist)
}

@Composable
fun SliderTime(duration: Long, time: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Text(
            text = convertTime(time),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = convertTime(duration),
            fontWeight = FontWeight.Bold
        )
    }
}

fun convertTime(millis: Long): String {
    if (millis == 0L) return "0:0"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    if (seconds < 1) return "0:0"

    return if (hours > 0) ("${hours}:${mins}:${seconds}")
    else ("${mins}:${seconds}")

}

@Composable
fun Slider(onAction: (Float) -> Unit, songDuration: Long, currentTime: Long) {
    val progress: Float =
        if (currentTime > 0f) currentTime.toFloat() / songDuration.toFloat() else 0f
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(progress) }
    val displayValue = if (isDragging) dragValue else progress
    val showTimeAboveSlider = showTimeAboveSlider.value

    if (showTimeAboveSlider) {
        SliderTime(songDuration, currentTime)
    }

    WavySlider3(
        value = displayValue,
        onValueChange = {
            isDragging = true
            dragValue = it
        },
        onValueChangeFinished = {
            isDragging = false
            onAction(dragValue)
        },
        waveLength = 32.dp,
        waveHeight = 8.dp,
        waveThickness = 4.dp,
        trackThickness = 4.dp,
        incremental = false,
    )
}

@Composable
fun TitleFrame(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(32.dp, 0.dp, 32.dp, 0.dp)
            .height(48.dp)
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            modifier = Modifier
                .padding(32.dp, 0.dp, 32.dp, 0.dp),
            maxLines = 1
        )
    }
}

@Composable
fun ButtonRow(
    onAction: (PlayerAction) -> Unit,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(
            12.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        // Shuffle, repeat and favourites buttons
        Column(
            modifier = Modifier
                .size(64.dp, 128.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
//            IconButton({ onAction(PlayerAction.AddToFavs) }) {
//                Icon(
//                    if (isInFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                    null,
//                    tint = MaterialTheme.colorScheme.primary
//                )
//            }
            //Shuffle
            Button(
                onClick = {
                    onAction(PlayerAction.ToggleShuffle)
                }, Modifier
                    .height(32.dp)
                    .fillMaxWidth()
            )
            {
                Icon(
                    imageVector = if (isShuffleEnabled) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                    contentDescription = null
                )
            }

            Spacer(Modifier.size(8.dp))
            //Repeat
            Button(
                onClick = {
                    onAction(PlayerAction.ToggleRepeat)
                }, Modifier
                    .height(32.dp)
                    .fillMaxWidth()
            )
            {
                Icon(
                    imageVector = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        Player.REPEAT_MODE_ALL -> Icons.Default.RepeatOn
                        else -> Icons.Default.Repeat
                    }, contentDescription = null
                )
            }
        }

        //Next and previous
        Column(
            modifier = Modifier
                .height(88.dp)
                .widthIn(55.dp, 112.dp)
                .weight(1f)
        ) {
            Button(
                onClick = { onAction(PlayerAction.SkipNext) }, modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            )
            {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = { onAction(PlayerAction.SkipPrevious) }, modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
            )
            {
                Icon(
                    imageVector = Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        //Play
        Button(
            onClick = { onAction(PlayerAction.PlayPause) }, modifier = Modifier
                .size(100.dp, 124.dp)
                .fillMaxHeight()
        ) {
            Icon(
                imageVector = when (isPlaying) {
                    false -> Icons.Default.PlayArrow
                    true -> Icons.Default.Pause
                },
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = null,
            )
        }
    }
}
