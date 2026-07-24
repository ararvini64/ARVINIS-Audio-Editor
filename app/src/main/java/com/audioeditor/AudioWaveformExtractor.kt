package com.audioeditor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class AudioWaveformExtractor(private val context: Context) {

    suspend fun extractAmplitudes(audioUri: Uri, sampleCount: Int = 100): List<Float> = withContext(Dispatchers.IO) {
        val amplitudes = mutableListOf<Float>()
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(context, audioUri, null)
            var trackIndex = -1
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    trackIndex = i
                    break
                }
            }

            if (trackIndex >= 0) {
                extractor.selectTrack(trackIndex)
                val buffer = ByteBuffer.allocate(1024 * 8)
                val rawAmplitudes = mutableListOf<Float>()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    var sum = 0L
                    for (i in 0 until sampleSize step 2) {
                        if (i + 1 < sampleSize) {
                            val sample = (buffer.get(i).toInt() or (buffer.get(i + 1).toInt() shl 8)).toShort()
                            sum += Math.abs(sample.toInt())
                        }
                    }
                    val avg = sum.toFloat() / (sampleSize / 2)
                    rawAmplitudes.add(avg)
                    extractor.advance()
                }

                if (rawAmplitudes.isNotEmpty()) {
                    val step = (rawAmplitudes.size / sampleCount.toFloat()).coerceAtLeast(1f)
                    val maxVal = rawAmplitudes.maxOrNull() ?: 1f

                    for (i in 0 until sampleCount) {
                        val index = (i * step).toInt().coerceAtMost(rawAmplitudes.size - 1)
                        amplitudes.add((rawAmplitudes[index] / maxVal).coerceIn(0.1f, 1f))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            extractor.release()
        }

        if (amplitudes.isEmpty()) {
            List(sampleCount) { 0.2f }
        } else {
            amplitudes
        }
    }
}
