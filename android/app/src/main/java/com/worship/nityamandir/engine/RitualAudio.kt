package com.worship.nityamandir.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.worship.nityamandir.R

/** Local audio assets work offline; each channel is stopped on background/temple close. */
class RitualAudio(context: Context) {
    private val players=mutableMapOf<Int,MediaPlayer>()
    init {
        listOf(R.raw.ritual_music,R.raw.water_offering,R.raw.flower_offering,R.raw.bell,R.raw.conch).forEach { id ->
            MediaPlayer.create(context,id)?.let { player ->
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                players[id]=player
            }
        }
    }
    fun music(active: Boolean) {
        val player=players[R.raw.ritual_music] ?: return
        if(active) {player.isLooping=true;player.setVolume(.32f,.32f);if(!player.isPlaying) player.start()}
        else if(player.isPlaying) player.pause()
    }
    fun cue(id: Int) { players[id]?.apply {seekTo(0);start()} }
    fun stopCue(id: Int) {players[id]?.apply {if(isPlaying) pause();seekTo(0)} }
    fun stop() {players.values.forEach {if(it.isPlaying) it.pause();it.seekTo(0)}}
    fun release() {players.values.forEach {it.release()};players.clear()}
}
