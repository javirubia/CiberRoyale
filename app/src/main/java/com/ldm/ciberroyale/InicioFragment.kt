package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.databinding.FragmentInicioBinding

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegación al fragmento de bienvenida
        binding.btnComenzar.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click)
            findNavController().navigate(R.id.action_inicioFragment_to_bienvenidaFragment)
        }
    }
    override fun onResume() {
        super.onResume()
        // La música empieza a sonar cuando esta pantalla se muestra
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        // Paramos la música si el usuario sale de la app desde esta pantalla
        SoundManager.stopMusic()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
