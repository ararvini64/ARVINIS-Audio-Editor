package com.audioeditor

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class AudioUiState(
    val isRecording: Boolean = false,
    val isPlaying: Boolean = false,
    val isTrimming: Boolean = false,
    val audioUri: Uri? = null,
    val amplitudes: List<Float> = emptyList(),
    val durationMs: Long = 0L,
    val trimStartRange: Float = 0f,
    val trimEndRange: Float = 1f,
    val statusText: String = "یک فایل انتخاب کنید یا صدا ضبط کنید"
)

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorder by lazy { AndroidAudioRecorder(application) }
    private val player by lazy { AndroidAudioPlayer(application) }
    private val extractor by lazy { AudioWaveformExtractor(application) }
    private val trimmer by lazy { AudioTrimmer(application) }

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    private var audioFile: File? = null

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            recorder.stop()
            val uri = Uri.fromFile(audioFile)
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                audioUri = uri,
                statusText = "ضبط متوقف شد. در حال آنالیز..."
            )
            uri?.let { processAudioWaveform(it) }
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
        val currentUri = _uiState.value.audioUri ?: return

        if (_uiState.value.isPlaying) {
            player.stop()
            _uiState.value = _uiState.value.copy(isPlaying = false, statusText = "پخش متوقف شد")
        } else {
            // شروع پخش دقیقاً از زمان مارکر شروع (هایلایت)
            val startMs = (_uiState.value.trimStartRange * _uiState.value.durationMs).toLong()
            player.playFile(currentUri, startMs)
            _uiState.value = _uiState.value.copy(isPlaying = true, statusText = "در حال پخش از محدوده انتخاب شده...")
        }
    }

    fun setAudioUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            audioUri = uri,
            trimStartRange = 0f,
            trimEndRange = 1f,
            statusText = "در حال پردازش..."
        )
        processAudioWaveform(uri)
    }

    fun updateTrimRange(start: Float, end: Float) {
        _uiState.value = _uiState.value.copy(
            trimStartRange = start,
            trimEndRange = end
        )
    }

    fun trimAndSaveAudio() {
        val uri = _uiState.value.audioUri ?: return
        val totalDuration = _uiState.value.durationMs
        if (totalDuration <= 0) return

        val startMs = (_uiState.value.trimStartRange * totalDuration).toLong()
        val endMs = (_uiState.value.trimEndRange * totalDuration).toLong()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTrimming = true, statusText = "در حال برش فایل...")
            val outputFile = File(getApplication<Application>().cacheDir, "trimmed_${System.currentTimeMillis()}.m4a")
            
            val success = trimmer.trimAudio(uri, outputFile, startMs, endMs)
            if (success) {
                val newUri = Uri.fromFile(outputFile)
                setAudioUri(newUri)
                _uiState.value = _uiState.value.copy(isTrimming = false, statusText = "برش با موفقیت انجام شد!")
            } else {
                _uiState.value = _uiState.value.copy(isTrimming = false, statusText = "خطا در برش فایل صوتی")
            }
        }
    }

    private fun processAudioWaveform(uri: Uri) {
        viewModelScope.launch {
            val amplitudes = extractor.extractAmplitudes(uri)
            val duration = getAudioDuration(uri)
            _uiState.value = _uiState.value.copy(
                amplitudes = amplitudes,
                durationMs = duration,
                statusText = "آماده ادیت (طول فایل: ${duration / 1000} ثانیه)"
            )
        }
    }

    private fun getAudioDuration(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getApplication(), uri)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
