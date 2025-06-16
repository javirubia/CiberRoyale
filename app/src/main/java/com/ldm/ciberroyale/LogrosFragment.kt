package com.ldm.ciberroyale

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.ldm.ciberroyale.databinding.FragmentLogrosBinding

class LogrosFragment : Fragment(R.layout.fragment_logros) {

    private var _binding: FragmentLogrosBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogrosBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rvLogros.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogros.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Ahora esta línea es correcta porque el tipo de 'sortedLogros' es List<Achievement>
        val sortedLogros = ProgresoManager.allAchievements.sortedWith(
            compareBy<Achievement> { it.type.ordinal } // Ordenar por tipo primero
                .thenBy { it.levelNumber ?: Int.MAX_VALUE } // Luego por número de nivel
                .thenBy { it.scoreCategory?.ordinal ?: -1 } // Finalmente por categoría
        )
        binding.rvLogros.adapter = LogrosAdapter(sortedLogros)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}