package com.audioeditor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class AudioTrimmer(private val context: Context) {

    suspend fun trimAudio(
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, inputUri, null)

            var audioTrackIndex = -1
            var inputFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || inputFormat == null) return@withContext false

            extractor.selectTrack(audioTrackIndex)

            val startUs = startMs * 1000
            val endUs = endMs * 1000
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext false
            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2

            // Decoder setup
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            // Encoder setup (AAC Output)
            val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            var muxerStarted = false

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            var extractorDone = false
            var decoderDone = false
            var encoderDone = false

            var presentationTimeOffsetUs = -1L

            val timeoutUs = 5000L

            while (!encoderDone) {
                // 1. Feed Extractor to Decoder
                if (!extractorDone) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        val sampleTime = extractor.sampleTime

                        if (sampleSize < 0 || sampleTime > endUs) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // 2. Decode & Feed to Encoder
                if (!decoderDone) {
                    val outputBufferIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, timeoutUs)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        val isEos = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (decoderBufferInfo.size > 0 && decoderBufferInfo.presentationTimeUs >= startUs) {
                            if (presentationTimeOffsetUs < 0) {
                                presentationTimeOffsetUs = decoderBufferInfo.presentationTimeUs
                            }

                            val encoderInputIndex = encoder.dequeueInputBuffer(timeoutUs)
                            if (encoderInputIndex >= 0) {
                                val encoderInputBuffer = encoder.getInputBuffer(encoderInputIndex) ?: continue
                                encoderInputBuffer.clear()
                                outputBuffer?.position(decoderBufferInfo.offset)
                                outputBuffer?.limit(decoderBufferInfo.offset + decoderBufferInfo.size)
                                encoderInputBuffer.put(outputBuffer)

                                val pts = decoderBufferInfo.presentationTimeUs - presentationTimeOffsetUs
                                encoder.queueInputBuffer(
                                    encoderInputIndex,
                                    0,
                                    decoderBufferInfo.size,
                                    pts,
                                    if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                )
                            }
                        }

                        decoder.releaseOutputBuffer(outputBufferIndex, false)

                        if (isEos) {
                            decoderDone = true
                        }
                    }
                }

                // 3. Encode & Write to Muxer
                val encoderOutputIndex = encoder.dequeueOutputBuffer(encoderBufferInfo, timeoutUs)
                if (encoderOutputIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderOutputIndex) ?: continue

                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encoderBufferInfo.size = 0
                    }

                    if (encoderBufferInfo.size != 0) {
                        if (!muxerStarted) {
                            val newFormat = encoder.outputFormat
                            muxerTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }

                        encodedData.position(encoderBufferInfo.offset)
                        encodedData.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                        muxer.writeSampleData(muxerTrackIndex, encodedData, encoderBufferInfo)
                    }

                    encoder.releaseOutputBuffer(encoderOutputIndex, false)

                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encoderDone = true
                    }
                } else if (encoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        val newFormat = encoder.outputFormat
                        muxerTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Exception) {}
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
            try {
                if (muxer != null) {
                    muxer.stop()
                    muxer.release()
                }
            } catch (_: Exception) {}
        }
    }
}
