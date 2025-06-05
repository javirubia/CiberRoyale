package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // Botón volver del encabezado
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nivel 1 → Juego / Fragmento Nivel1
        binding.nivel1Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel1Fragment)
        }

        // Nivel 2 → Fragmento Nivel2
        binding.nivel2Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel2Fragment)
        }

        // Nivel 3 → Fragmento Nivel3
        binding.nivel3Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel3Fragment)
        }

        // Nivel 4 → Fragmento Nivel4
        binding.nivel4Card.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel4Fragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
