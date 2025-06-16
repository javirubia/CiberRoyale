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
        // Inicializamos el gestor de progreso aquí. Es crucial hacerlo
        // antes de que cualquier otra parte del fragmento intente usarlo.
        ProgresoManager.init(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Organizamos el código para más claridad
        setupNavigationListeners()
        actualizarEstadoNiveles()
    }

    private fun setupNavigationListeners() {
        // Botón volver del encabezado
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_menuFragment)
        }

        // Navegación a cada nivel
        binding.nivel1Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel1Fragment)
        }
        binding.nivel2Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel2Fragment)
        }
        binding.nivel3Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel3Fragment)
        }
        binding.nivel4Card.setOnClickListener {
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