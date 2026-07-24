package com.audioeditor

import android.content.Context
import android.net.Uri
import com.ffmpegkit.FFmpegKit
import com.ffmpegkit.FFmpegKitConfig
import com.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AudioTrimmer(private val context: Context) {

    suspend fun trimAudio(
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val startSec = startMs / 1000.0
            val durationSec = (endMs - startMs) / 1000.0

            // تبدیل Uri اندروید به مسیر قابل فهم برای FFmpeg
            val inputPath = FFmpegKitConfig.getSafParameterForRead(context, inputUri)

            // دستور FFmpeg برای برش بسیار سریع و بدون افت کیفیت
            val cmd = "-ss $startSec -i $inputPath -t $durationSec -c copy \"${outputFile.absolutePath}\" -y"

            val session = FFmpegKit.execute(cmd)

            ReturnCode.isSuccess(session.returnCode)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
