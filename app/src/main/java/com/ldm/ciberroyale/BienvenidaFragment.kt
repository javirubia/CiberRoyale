package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.databinding.FragmentBienvenidaBinding

class BienvenidaFragment : Fragment() {

    private var _binding: FragmentBienvenidaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBienvenidaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegación al menú principal
        binding.btnContinuar.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click)
            findNavController().navigate(R.id.action_bienvenidaFragment_to_menuFragment)
        }
    }
    override fun onResume() {
        super.onResume()
        // La música del menú continúa o empieza aquí
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        // Paramos la música solo si el usuario sale de la app
        SoundManager.stopMusic()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
