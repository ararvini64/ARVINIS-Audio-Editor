package com.audioeditor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AudioViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setAudioUri(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.toggleRecording()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Editor Pro") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // تغییر اصلی اینجاست: پاس دادن startRange و endRange
                    WaveformView(
                        amplitudes = uiState.amplitudes,
                        startRange = uiState.trimStartRange,
                        endRange = uiState.trimEndRange
                    )
                    Text(
                        text = uiState.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    )
                }
            }

            // اسلایدرهای انتخاب محدوده برش
            if (uiState.audioUri != null && uiState.durationMs > 0) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = "محدوده برش: از ${(uiState.trimStartRange * uiState.durationMs / 1000).toInt()}s تا ${(uiState.trimEndRange * uiState.durationMs / 1000).toInt()}s",
                        style = MaterialTheme.typography.labelLarge
                    )
                    RangeSlider(
                        value = uiState.trimStartRange..uiState.trimEndRange,
                        onValueChange = { range ->
                            viewModel.updateTrimRange(range.start, range.endInclusive)
                        },
                        valueRange = 0f..1f
                    )
                }
            }

            Surface(
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { audioPickerLauncher.launch("audio/*") }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open Audio")
                    }

                    IconButton(
                        onClick = {
                            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Record",
                            tint = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlay() },
                        enabled = uiState.audioUri != null && !uiState.isRecording
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play"
                        )
                    }

                    Button(
                        onClick = { viewModel.trimAndSaveAudio() },
                        enabled = uiState.audioUri != null && !uiState.isTrimming && !uiState.isRecording
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Trim")
                        Spacer(Modifier.width(4.dp))
                        Text("برش")
                    }
                }
            }
        }
    }
}
