package com.ldm.ciberroyale

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.slider.Slider
import com.ldm.ciberroyale.databinding.FragmentAjustesBinding

class AjustesFragment : Fragment(R.layout.fragment_ajustes) {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAjustesBinding.bind(view)

        setupListeners()
        loadCurrentSettings()
    }

    override fun onResume() {
        super.onResume()
        // Mantenemos la música del menú para que no haya silencios incómodos al navegar
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.stopMusic()
    }

    private fun loadCurrentSettings() {
        // 1. Cargar estado del interruptor general
        val soundEnabled = AjustesManager.isSoundEnabled()
        binding.switchSound.isChecked = soundEnabled

        // 2. Cargar valor de los sliders
        binding.sliderMusic.value = AjustesManager.getMusicVolume().toFloat()
        binding.sliderSfx.value = AjustesManager.getSfxVolume().toFloat()

        // 3. Habilitar o deshabilitar los sliders según el interruptor general
        updateSlidersState(soundEnabled)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().popBackStack()
        }

        // Listener para el interruptor general
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            AjustesManager.setSoundEnabled(isChecked)
            updateSlidersState(isChecked)

            // Damos feedback sonoro solo cuando se ACTIVA el sonido
            if (isChecked) {
                SoundManager.playSfx(R.raw.sfx_switch_on) // <-- AÑADIDO
            }
        }

        // Listener para guardar el valor del slider de música mientras se mueve
        binding.sliderMusic.addOnChangeListener { _, value, _ ->
            AjustesManager.setMusicVolume(value.toInt())
        }

        // Listener para el slider de efectos
        binding.sliderSfx.addOnChangeListener { _, value, _ ->
            AjustesManager.setSfxVolume(value.toInt())
        }
    }

    // Función de ayuda para activar/desactivar los sliders visualmente
    private fun updateSlidersState(isEnabled: Boolean) {
        binding.labelMusicVolume.isEnabled = isEnabled
        binding.sliderMusic.isEnabled = isEnabled
        binding.labelSfxVolume.isEnabled = isEnabled
        binding.sliderSfx.isEnabled = isEnabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}