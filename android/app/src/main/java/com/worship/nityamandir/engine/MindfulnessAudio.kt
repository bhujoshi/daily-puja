package com.worship.nityamandir.engine

import android.content.Context
import android.media.MediaPlayer
import com.worship.nityamandir.R

/** Quiet, bundled meditation cues, owned only by the mindfulness screen. */
class MindfulnessAudio(context: Context) {
    private val flute = MediaPlayer.create(context, R.raw.mindfulness_flute)
    private val note = MediaPlayer.create(context, R.raw.mindfulness_note)
    private val players = listOfNotNull(flute, note)

    init {
        players.forEach { player -> player.setVolume(0.65f, 0.65f) }
    }

    fun cue() = play(flute)
    fun mantraTap() = play(note)

    private fun play(player: MediaPlayer?) {
        stop()
        player?.start()
    }

    fun stop() {
        players.forEach { player ->
            if (player.isPlaying) player.pause()
            player.seekTo(0)
        }
    }

    fun release() = players.forEach { it.release() }
}
