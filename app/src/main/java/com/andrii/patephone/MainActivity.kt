package com.andrii.patephone

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.andrii.patephone.action.MusicServiceConnection
import com.andrii.patephone.action.PlayerAction
import com.andrii.patephone.ui.theme.ApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider as WavySlider3


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    Surface(
                        Modifier
                            .padding(innerPadding)
                    ) {
                        MainColumn()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicServiceConnection.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        musicServiceConnection.stopTracking()
    }

    override fun onResume() {
        super.onResume()
        musicServiceConnection.startTracking()
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    ApplicationTheme {
        Surface(
            modifier = Modifier
                .size(420.dp, 860.dp)
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
                Slider({}, 0.5f)
                ButtonRow(
                    {},
                    isPlaying = false,
                    isShuffleEnabled = true,
                    repeatMode = 0,
                    false
                )
                TitleFrame("Nothing") {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MainColumn(viewModel: MainViewModel = hiltViewModel()) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val playlist by viewModel.playlist.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .padding(32.dp, 72.dp, 32.dp, 72.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    )
    {
        ArtworkFrame(
            onActionImport = { uri -> viewModel.onActionImport(uri) },
            viewModel.artworkUri.collectAsState().value,
            viewModel.artist.collectAsState().value
        )
        Slider(
            onAction = { float -> viewModel.onSliderMove(float) },
            progress = viewModel.progress.collectAsState().value
        )
        ButtonRow(
            onAction = { action -> viewModel.onAction(action) },
            isPlaying = viewModel.isPlaying.collectAsState().value,
            isShuffleEnabled = viewModel.isShuffleEnabled.collectAsState().value,
            repeatMode = viewModel.repeatMode.collectAsState().value,
            false
        )
        TitleFrame(viewModel.title.collectAsState().value, onClick = {
            showBottomSheet =
                playlist.isNotEmpty()
        })
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Playlist(
                playlist,
                { index ->
                    viewModel.seekToMedia(index)
                }, viewModel.currentSongIndex.collectAsState().value
            )
        }
    }

    if (showOptions) {
        AnimatedVisibility(
            visible = showOptions,
            enter = slideInVertically(
                initialOffsetY = { -it }
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it }
            )
        ) {
            Box(Modifier.size(300.dp, 600.dp)) {
                Column {
                    Row(Modifier.clickable(true) { openSettings() }) {
                        Icon(Icons.Default.Settings, null)
                        Text("Settings")
                    }
                    Button({ showOptions = false }) { Text("Close") }
                }
            }
        }
    }
}

fun openSettings() {
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
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.Center
    )
    {
        SubcomposeAsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
        ) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background
                )
            } else {
                SubcomposeAsyncImageContent()
            }
        }
    }
    TextUnderArtwork(onActionImport, artist)
}


@Composable
fun Slider(onAction: (Float) -> Unit, progress: Float) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(progress) }

    val displayValue = if (isDragging) dragValue else progress

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
        waveLength = 32.dp,     // Setting this to 0.dp results in a Slider
        waveHeight = 8.dp,     // Setting this to 0.dp results in a Slider
        waveThickness = 4.dp,   // Defaults to 4.dp irregardless of variant
        trackThickness = 4.dp,  // Defaults to a thickness based on variant
        incremental = false,    // Whether to gradually increase waveHeight
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
    repeatMode: Int,
    isInFavorites: Boolean
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