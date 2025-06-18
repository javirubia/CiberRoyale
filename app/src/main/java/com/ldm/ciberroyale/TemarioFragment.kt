package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.databinding.FragmentTemarioBinding

class TemarioFragment : Fragment() {

    private var _binding: FragmentTemarioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_temarioFragment_to_menuFragment)
        }

        binding.opcionContrasenas.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_temarioFragment_to_temaContraFragment)
        }

        binding.opcionPhishing.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_temarioFragment_to_temaPhishingFragment)
        }

        binding.opcionPrivacidad.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_temarioFragment_to_temaRRSSFragment)
        }

        binding.opcionSeguridadMov.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_temarioFragment_to_temaSeguridadmovFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Mantenemos la música del menú
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.stopMusic()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}