package com.audioeditor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioUiState(
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val audioUri: Uri? = null,
    val statusText: String = "یک فایل انتخاب کنید یا صدا ضبط کنید"
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder by lazy { AndroidAudioRecorder(application) }
    private val player by lazy { AndroidAudioPlayer(application) }

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    private var audioFile: File? = null

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            recorder.stop()
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                audioUri = Uri.fromFile(audioFile),
                statusText = "ضبط متوقف شد. آماده پخش!"
            )
        } else {
            val file = File(getApplication<Application>().cacheDir, "recorded_audio.mp3")
            recorder.start(file)
            audioFile = file
            _uiState.value = _uiState.value.copy(
                isRecording = true,
                statusText = "در حال ضبط صدا..."
            )
        }
    }

    fun togglePlay() {
        val currentUri = _uiState.value.audioUri
        if (currentUri == null) return

        if (_uiState.value.isPlaying) {
            player.stop()
            _uiState.value = _uiState.value.copy(isPlaying = false, statusText = "پخش متوقف شد")
        } else {
            player.playFile(currentUri)
            _uiState.value = _uiState.value.copy(isPlaying = true, statusText = "در حال پخش...")
        }
    }

    fun setAudioUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            audioUri = uri,
            statusText = "فایل صوتی با موفقیت بارگذاری شد"
        )
    }
}
