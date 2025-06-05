package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LogrosFragment : Fragment() {

    private lateinit var rvLogros: RecyclerView
    private lateinit var adapter: LogrosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflamos el layout sin Toolbar
        return inflater.inflate(R.layout.fragment_logros, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 0. Inicializar el botón “Volver” con findViewById, no con binding
        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            // Cambia esta línea según tu acción de navegación:
            // Si quieres simplemente volver al fragmento anterior:
            findNavController().popBackStack()
            // O si quieres navegar a un destino concreto (ej. menuFragment):
            // findNavController().navigate(R.id.action_logrosFragment_to_menuFragment)
        }

        // 1. Inicializar RecyclerView
        rvLogros = view.findViewById(R.id.rvLogros)
        rvLogros.layoutManager = LinearLayoutManager(requireContext())

        // 2. (Opcional) Divider entre tarjetas
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        // Si tienes un drawable personalizado para el divider, úsalo así:
        // divider.setDrawable(resources.getDrawable(R.drawable.divider_gray, null))
        rvLogros.addItemDecoration(divider)

        // 3. Ordenar logros y pasar al adaptador
        val sortedLogros = AchievementRepository.allAchievements.sortedWith(
            compareBy<Achievement> { it.type }
                .thenBy { it.themeNumber ?: it.levelNumber ?: Int.MAX_VALUE }
                .thenBy { it.scoreCategory?.ordinal ?: -1 }
        )
        adapter = LogrosAdapter(sortedLogros)
        rvLogros.adapter = adapter
    }
}
