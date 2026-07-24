package com.audioeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    audioViewModel: AudioViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val amplitudes by audioViewModel.amplitudes.collectAsState()
    val selectionRange by audioViewModel.selectionRange.collectAsState()
    val isProcessing by audioViewModel.isProcessing.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            audioViewModel.extractWaveform(context, it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Audio Editor & Trimmer",
            style = MaterialTheme.typography.headlineMedium
        )

        // نمایش Waveform و دستگیره‌های انتخاب
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                WaveformView(
                    amplitudes = amplitudes,
                    progress = if (isPlaying) 0.5f else 0f,
                    selectionRange = selectionRange,
                    onSelectionChanged = { newRange ->
                        audioViewModel.updateSelection(newRange)
                    }
                )
            }
        }

        Text(
            text = "Start: ${(selectionRange.start * 100).toInt()}%  |  End: ${(selectionRange.endInclusive * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )

        // دکمه‌های کنترلی
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Open Audio File")
            }

            if (selectedUri != null) {
                Button(
                    onClick = {
                        selectedUri?.let { uri ->
                            audioViewModel.trimAudio(context, uri) { trimmedFile ->
                                // فایل جدید آماده شده است
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Trim Selected Portion")
                }
            }
        }
    }
}
