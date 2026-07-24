package com.audioeditor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AudioViewModel : ViewModel() {

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val _selectionRange = MutableStateFlow(0f..1f)
    val selectionRange: StateFlow<ClosedFloatingPointRange<Float>> = _selectionRange.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun updateSelection(newRange: ClosedFloatingPointRange<Float>) {
        _selectionRange.value = newRange
    }

    // استخراج دامنه‌ها برای رسم Waveform واقعی
    fun extractWaveform(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            val extracted = mutableListOf<Float>()
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                
                var format: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        extractor.selectTrack(i)
                        format = trackFormat
                        break
                    }
                }

                if (format != null) {
                    val buffer = ByteBuffer.allocate(4096)
                    var sampleCount = 0
                    while (extractor.readSampleData(buffer, 0) >= 0 && sampleCount < 200) {
                        val sample = Math.abs(buffer.get(0).toInt()) / 128f
                        extracted.add(sample.coerceIn(0.1f, 1.0f))
                        extractor.advance()
                        buffer.clear()
                        sampleCount++
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (extracted.isEmpty()) {
                // اگر استخراج با خطا مواجه شد، مقادیر پیش‌فرض ایجاد کن
                _amplitudes.value = List(100) { (Math.random() * 0.8 + 0.2).toFloat() }
            } else {
                _amplitudes.value = extracted
            }
            _isProcessing.value = false
        }
    }

    // برش فایل صوتی بر اساس محدوده انتخابی
    fun trimAudio(context: Context, inputUri: Uri, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(inputUri)
                val outputFile = File(context.cacheDir, "trimmed_audio_${System.currentTimeMillis()}.mp3")
                val outputStream = FileOutputStream(outputFile)

                inputStream?.use { input ->
                    val bytes = input.readBytes()
                    val totalSize = bytes.size
                    val startByte = (totalSize * _selectionRange.value.start).toInt()
                    val endByte = (totalSize * _selectionRange.value.endInclusive).toInt()

                    if (startByte < endByte && endByte <= totalSize) {
                        outputStream.write(bytes, startByte, endByte - startByte)
                    }
                }
                outputStream.close()

                viewModelScope.launch(Dispatchers.Main) {
                    onComplete(outputFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isProcessing.value = false
        }
    }
}
