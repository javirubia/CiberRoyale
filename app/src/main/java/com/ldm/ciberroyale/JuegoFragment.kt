package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.databinding.FragmentJuegoBinding

class JuegoFragment : Fragment() {

    private var _binding: FragmentJuegoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJuegoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Organizamos el código para más claridad
        setupNavigationListeners()
    }
    override fun onResume() {
        super.onResume()
        SoundManager.playMusic(requireContext(), R.raw.music_ingame)
        actualizarEstadoNiveles()
    }
    override fun onPause() {
        super.onPause()
        // Paramos la música al salir de esta pantalla
        SoundManager.stopMusic()
    }
    private fun setupNavigationListeners() {
        // Botón volver del encabezado
        binding.btnBack.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_juegoFragment_to_menuFragment)
        }

        // Navegación a cada nivel
        binding.nivel1Card.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_level_start) // <-- AÑADIDO
            findNavController().navigate(R.id.action_juegoFragment_to_nivel1Fragment)
        }
        binding.nivel2Card.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_level_start) // <-- AÑADIDO
            findNavController().navigate(R.id.action_juegoFragment_to_nivel2Fragment)
        }
        binding.nivel3Card.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_level_start) // <-- AÑADIDO
            findNavController().navigate(R.id.action_juegoFragment_to_nivel3Fragment)
        }
        binding.nivel4Card.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_level_start) // <-- AÑADIDO
            findNavController().navigate(R.id.action_juegoFragment_to_nivel4Fragment)
        }
    }

    /**
     * Esta función comprueba el progreso del jugador y actualiza la UI para
     * bloquear o desbloquear los niveles correspondientes.
     */
    private fun actualizarEstadoNiveles() {

        val nivelMasAltoDesbloqueado = ProgresoManager.getNivelDesbloqueado()

        // Nivel 2: Se desbloquea si el progreso es 2 o más.
        val nivel2Bloqueado = nivelMasAltoDesbloqueado < 2
        binding.nivel2Card.isEnabled = !nivel2Bloqueado
        binding.overlayBloqueo2.isVisible = nivel2Bloqueado
        binding.lockIcon2.isVisible = nivel2Bloqueado

        // Nivel 3: Se desbloquea si el progreso es 3 o más.
        val nivel3Bloqueado = nivelMasAltoDesbloqueado < 3
        binding.nivel3Card.isEnabled = !nivel3Bloqueado
        binding.overlayBloqueo3.isVisible = nivel3Bloqueado
        binding.lockIcon3.isVisible = nivel3Bloqueado

        // Nivel 4: Se desbloquea si el progreso es 4 o más.
        val nivel4Bloqueado = nivelMasAltoDesbloqueado < 4
        binding.nivel4Card.isEnabled = !nivel4Bloqueado
        binding.overlayBloqueo4.isVisible = nivel4Bloqueado
        binding.lockIcon4.isVisible = nivel4Bloqueado
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}