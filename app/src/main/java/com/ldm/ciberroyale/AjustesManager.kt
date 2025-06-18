package com.ldm.ciberroyale

import android.content.Context
import android.content.SharedPreferences

object AjustesManager {

    private const val PREFS_NAME = "CiberRoyaleAjustes"
    private lateinit var prefs: SharedPreferences

    // Definimos las claves para cada ajuste que vamos a guardar
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_MUSIC_VOLUME = "music_volume"
    private const val KEY_SFX_VOLUME = "sfx_volume"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- Interruptor General ---
    fun setSoundEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, isEnabled).apply()
    }

    fun isSoundEnabled(): Boolean {
        // Por defecto, el sonido estará activado la primera vez que se abre la app
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    // --- Volumen de Música (0-100) ---
    fun setMusicVolume(volume: Int) {
        prefs.edit().putInt(KEY_MUSIC_VOLUME, volume).apply()
    }

    fun getMusicVolume(): Int {
        // Por defecto, el volumen estará al 80%
        return prefs.getInt(KEY_MUSIC_VOLUME, 80)
    }

    // --- Volumen de Efectos de Sonido (SFX) (0-100) ---
    fun setSfxVolume(volume: Int) {
        prefs.edit().putInt(KEY_SFX_VOLUME, volume).apply()
    }

    fun getSfxVolume(): Int {
        // Por defecto, el volumen estará al 100%
        return prefs.getInt(KEY_SFX_VOLUME, 100)
    }
}