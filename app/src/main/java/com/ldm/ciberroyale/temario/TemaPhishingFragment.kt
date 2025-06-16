package com.ldm.ciberroyale.temario

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.ldm.ciberroyale.ProgresoManager

import com.ldm.ciberroyale.R

class TemaPhishingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tema_phishing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val containerLayout = view.findViewById<LinearLayout>(R.id.containerLayout)
        val btnVolver = view.findViewById<ImageButton>(R.id.btnBack)
        btnVolver.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val temas = listOf(
            Triple(
                "¿Qué es el Phishing?",
                "Es cuando alguien intenta engañarte con un mensaje falso para que des tu contraseña o tus datos. ¡Parece real, pero es una trampa! 🪝",
                R.drawable.ic_phishing
            ),
            Triple(
                "¿Cómo Reconocer un Mensaje Sospechoso?",
                "🚨 Usa frases como: \"¡Tu cuenta será bloqueada!\"\n📝 Tiene errores o enlaces raros como www.banco-falso123.com\n❌ Pide tu contraseña o número de tarjeta.",
                R.drawable.ic_warning_email
            ),
            Triple(
                "¿Qué Hacer si Recibes un Mensaje Raro?",
                "📵 No hagas clic si no estás seguro\n👨‍👩‍👧 Díselo a un adulto de confianza\n📧 Revisa siempre quién lo envía",
                R.drawable.ic_shield
            ),
            Triple(
                "¿Qué es la Ingeniería Social?",
                "Es cuando alguien intenta ganarse tu confianza para que le des información sin que te des cuenta.\n\nPuede hacerse pasar por un amigo, maestro o técnico. ¡Pero es un engaño! 🎭",
                R.drawable.ic_actor
            )

        )
        val btnMarcarLeido: Button = view.findViewById(R.id.btnMarcarLeido)
        btnMarcarLeido.setOnClickListener {
            // Desbloquea el logro del Tema 2
            ProgresoManager.unlockAchievement("TEMA2_LEIDO")
            // Feedback visual
            Toast.makeText(
                requireContext(),
                "¡Logro desbloqueado: Tema 2 completado!",
                Toast.LENGTH_SHORT
            ).show()

            // Opcional: volver a la pantalla de logros
            // findNavController().popBackStack()
        }


        for ((titulo, descripcion, icono) in temas) {
            val cardView = layoutInflater.inflate(R.layout.item_tarjeta_tema, containerLayout, false)
            cardView.findViewById<TextView>(R.id.tituloTarjeta).text = titulo
            cardView.findViewById<TextView>(R.id.descripcionTarjeta).text = descripcion
            cardView.findViewById<ImageView>(R.id.imagenIcono).setImageResource(icono)

            containerLayout.addView(cardView)
        }
    }
}
