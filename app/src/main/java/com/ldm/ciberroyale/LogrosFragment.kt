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

    // AÑADIDO: Guardamos una referencia al adaptador
    private lateinit var logrosAdapter: LogrosAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogrosBinding.bind(view)

        setupUI()
    }

    // AÑADIDO: Movemos la lógica de la UI a su propia función
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_logrosFragment_to_menuFragment)
        }

        // Ordenamos la lista como ya lo hacías
        val sortedLogros = ProgresoManager.allAchievements.sortedWith(
            compareBy<Achievement> { it.type.ordinal }
                .thenBy { it.levelNumber ?: Int.MAX_VALUE }
                .thenBy { it.scoreCategory?.ordinal ?: -1 }
        )

        // Creamos e inicializamos el adaptador UNA SOLA VEZ
        logrosAdapter = LogrosAdapter(sortedLogros)

        binding.rvLogros.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogros.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvLogros.adapter = logrosAdapter
    }

    // AÑADIDO: Actualizamos el adaptador cuando la pantalla se muestra
    override fun onResume() {
        super.onResume()
        SoundManager.playMusic(requireContext(), R.raw.music_menu)

        if (::logrosAdapter.isInitialized) { // Comprobamos que el adaptador ya fue creado
            logrosAdapter.notifyDataSetChanged()
        }
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