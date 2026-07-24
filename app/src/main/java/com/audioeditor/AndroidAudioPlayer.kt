package com.audioeditor

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AndroidAudioPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playFile(fileUri: Uri) {
        MediaPlayer.create(context, fileUri)?.apply {
            player = this
            start()
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    val isPlaying: Boolean
        get() = player?.isPlaying ?: false
}
