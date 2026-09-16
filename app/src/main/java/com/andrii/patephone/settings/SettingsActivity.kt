package com.andrii.patephone.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrii.patephone.ui.theme.ApplicationTheme
import kotlin.math.roundToInt
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider as WavySlider3


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = SettingsManager(applicationContext)
        setContent {
            ApplicationTheme {
                Scaffold { innerPadding ->
                    val useMetadataArtwork by settingsManager.useMetadataArtwork.collectAsStateWithLifecycle(
                        initialValue = true
                    )
                    val recursiveImport by settingsManager.recursiveImport.collectAsStateWithLifecycle(
                        initialValue = false
                    )
                    val recursiveImportDepth by settingsManager.recursiveImportDepth.collectAsStateWithLifecycle(
                        initialValue = 1
                    )
                    val notificationHeader by settingsManager.notificationHeader.collectAsStateWithLifecycle(
                        initialValue = NotificationHeader.SongTitle
                    )
                    LazyColumn(Modifier.padding(innerPadding).padding(60.dp)) {

                        // Use metadata artwork
                        item {
                            ListItem(
                                headlineContent = { Text("Use metadata artwork") },
                                supportingContent = { Text("When off, player uses picture from folder as artwork") },
                                trailingContent = {
                                    Switch(
                                        checked = useMetadataArtwork,
                                        onCheckedChange = { settingsManager.setMetadataArtwork(!useMetadataArtwork) },
                                    )
                                }
                            )
                        }
                        // Recursive import
                        item {
                            ListItem(
                                headlineContent = { Text("Recursive import") },
                                supportingContent = { Text("Seek music in inner directories") },
                                trailingContent = {
                                    Switch(
                                        checked = recursiveImport,
                                        onCheckedChange = { settingsManager.setRecursiveImport(!recursiveImport) }
                                    )
                                }
                            )
                        }
                        if (recursiveImport) item {
                            Column(Modifier.padding(start = 30.dp)) {
                                ListItem(
                                    headlineContent = { Text("Depth") },
                                    supportingContent = { Text("How many dirs will be checked") },
                                )
                                IntSlider(
                                    value = recursiveImportDepth,
                                    onValueChange = { settingsManager.setRecursiveImportDepth(it) },
                                    range = 1..10,
                                )

                                Spacer(Modifier.size(8.dp))
                            }
                        }
                        item {
                            ListItem(
                                headlineContent = { Text("Notification header") },
                                trailingContent = {
                                    DropdownBox(
                                        values = NotificationHeader.entries.map { it.name },
                                        currentValue = notificationHeader.name,
                                        onNewValue = { index ->
                                            settingsManager.setNotificationHeader(NotificationHeader.entries[index])
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        super.onCreate(savedInstanceState)
    }
}

@Composable
fun DropdownBox(
    values: List<String>,
    currentValue: String,
    onNewValue: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        Modifier.background(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ).padding(
            8.dp
        )
    ) {
        Text(
            text = currentValue,
            modifier = Modifier.clickable {
                expanded = true
            },
            fontSize = 14.sp
        )
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        values.forEachIndexed { index, value ->
            Box(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = value,
                    modifier = Modifier.clickable { onNewValue(index) },
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun IntSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedRange<Int>,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableIntStateOf(value) }

    val displayValue = if (isDragging) dragValue else value
    val floatRange = range.start.toFloat()..range.endInclusive.toFloat()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = dragValue.toString(),
            fontSize = 16.sp
        )
        WavySlider3(
            value = displayValue.toFloat(),
            onValueChange = {
                isDragging = true
                dragValue = it.roundToInt()
            },
            onValueChangeFinished = {
                isDragging = false
                onValueChange(dragValue)
            },
            waveLength = 32.dp,
            waveHeight = 8.dp,
            waveThickness = 4.dp,
            trackThickness = 4.dp,
            incremental = false,
            valueRange = floatRange
        )
    }
}
