package com.audioeditor

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AndroidAudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playFile(uri: Uri, startMs: Long = 0L) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            if (startMs > 0 && startMs < duration) {
                seekTo(startMs.toInt())
            }
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }
}
