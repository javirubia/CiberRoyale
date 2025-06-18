package com.ldm.ciberroyale

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

object SoundManager {

    private var musicPlayer: MediaPlayer? = null
    private var jinglePlayer: MediaPlayer? = null

    private lateinit var soundPool: SoundPool
    private val sfxMap = mutableMapOf<Int, Int>()

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Aquí cargamos ÚNICAMENTE los efectos de sonido cortos (SFX)
        loadSfx(context, R.raw.sfx_button_click)
        loadSfx(context, R.raw.sfx_correct_answer)
        loadSfx(context, R.raw.sfx_wrong_answer)
        loadSfx(context, R.raw.sfx_achievement_unlocked)
        loadSfx(context, R.raw.sfx_level_start)
        loadSfx(context, R.raw.sfx_navigate)
        loadSfx(context, R.raw.sfx_switch_off)
        loadSfx(context, R.raw.sfx_switch_on)
        loadSfx(context, R.raw.punch)
    }

    private fun loadSfx(context: Context, resId: Int) {
        val soundId = soundPool.load(context, resId, 1)
        sfxMap[resId] = soundId
    }

    // --- MÚSICA DE FONDO (en bucle) ---
    fun playMusic(context: Context, trackResId: Int) {
        if (!AjustesManager.isSoundEnabled()) return
        stopMusic() // Para cualquier música que estuviera sonando
        musicPlayer = MediaPlayer.create(context, trackResId).apply {
            val volume = AjustesManager.getMusicVolume() / 100f
            setVolume(volume, volume)
            isLooping = true
            start()
        }
    }

    fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer?.release()
        musicPlayer = null
    }

    // --- JINGLES (música corta, no se repite) ---
    fun playJingle(context: Context, trackResId: Int) {
        if (!AjustesManager.isSoundEnabled()) return
        stopMusic() // Para que el jingle suene sin la música de fondo
        jinglePlayer = MediaPlayer.create(context, trackResId).apply {
            val volume = AjustesManager.getMusicVolume() / 100f
            setVolume(volume, volume)
            isLooping = false
            start()
        }
    }

    // --- EFECTOS DE SONIDO (cortos) ---
    fun playSfx(resId: Int) {
        if (!AjustesManager.isSoundEnabled()) return
        val soundId = sfxMap[resId]
        if (soundId != null) {
            val volume = AjustesManager.getSfxVolume() / 100f
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        } else {
            Log.w("SoundManager", "Efecto de sonido no encontrado: $resId")
        }
    }
}