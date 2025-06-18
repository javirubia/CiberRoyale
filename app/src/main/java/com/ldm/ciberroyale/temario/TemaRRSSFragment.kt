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
import com.ldm.ciberroyale.SoundManager

class TemaRRSSFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tema_r_r_s_s, container, false)
    }
    override fun onResume() {
        super.onResume()
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.stopMusic()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val containerLayout = view.findViewById<LinearLayout>(R.id.containerLayout)
        val btnVolver = view.findViewById<ImageButton>(R.id.btnBack)
        btnVolver.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // Sonido al pulsar el botón
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val temas = listOf(
            Triple(
                "¿Por qué es importante cuidar tu privacidad?",
                "Cuando subes fotos o información, muchas personas pueden verla. Algunos podrían usar eso para hacerte daño o molestarte. 😟\n\n¡Protege tus cosas personales!",
                R.drawable.ic_privacidad
            ),
            Triple(
                "Cosas que nunca deberías compartir",
                "❌ Tu dirección o colegio\n📸 Fotos con tu ubicación\n🧾 Fotos de documentos\n👨‍👩‍👧 Información de tu familia\n\n¡Piénsalo antes de publicar!",
                R.drawable.ic_no_compartir
            ),
            Triple(
                "Cómo configurar tu perfil para estar seguro",
                "🔒 Haz tu cuenta privada\n👀 Elige quién puede ver tus cosas\n🚫 Bloquea a desconocidos\n📲 Activa alertas de inicio de sesión",
                R.drawable.ic_candado
            ),
            Triple(
                "¡Sé un héroe digital!",
                "🤖 Ayuda a tus amigos a configurar su privacidad\n🔍 Revisa tus publicaciones antiguas\n💬 Habla con un adulto si algo te parece raro",
                R.drawable.ic_heroe
            )
        )
        val btnMarcarLeido: Button = view.findViewById(R.id.btnMarcarLeido)
        btnMarcarLeido.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            ProgresoManager.unlockAchievement(requireContext(), "TEMA3_LEIDO") // <-- MODIFICADO
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
