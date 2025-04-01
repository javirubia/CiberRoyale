package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class JuegoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_juego, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegación a Nivel 1
        val cardNivel1 = view.findViewById<CardView>(R.id.nivel1_card)
        cardNivel1.setOnClickListener {
            findNavController().navigate(R.id.action_juegoFragment_to_nivel1Fragment)
        }

        // Botón atrás
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
