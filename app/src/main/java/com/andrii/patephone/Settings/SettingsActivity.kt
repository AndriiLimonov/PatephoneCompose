package com.andrii.patephone.Settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.serialization.saved
import com.andrii.patephone.ui.theme.ApplicationTheme


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = SettingsManager(applicationContext)
        setContent {
            ApplicationTheme {
                Scaffold { innerPadding ->
                    Column(Modifier.padding(innerPadding).padding(60.dp)) {
                        val useMetadataArtwork by settingsManager.useMetadataArtwork.collectAsStateWithLifecycle(
                            initialValue = true
                        )
                        // Use metadata artwork
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
                }
            }
        }
        super.onCreate(savedInstanceState)
    }
}

