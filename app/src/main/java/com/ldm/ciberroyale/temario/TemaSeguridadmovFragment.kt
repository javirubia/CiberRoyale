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

class TemaSeguridadmovFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tema_seguridadmov, container, false)
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
                "Cuida tu dispositivo como un tesoro",
                "Tu móvil o tablet tiene fotos, juegos, cuentas... ¡es muy valioso! 💎\n\nSi alguien lo coge sin permiso, puede hacer daño.",
                R.drawable.ic_mobile_lock
            ),
            Triple(
                "Pon una contraseña o patrón de bloqueo",
                "🔒 Usa un patrón, PIN o huella para proteger tu dispositivo.\n📱 Así nadie podrá usarlo sin permiso.",
                R.drawable.ic_pattern_lock
            ),
            Triple(
                "¡Cuidado con lo que instalas!",
                "📥 Solo descarga apps de sitios seguros (como Google Play).\n🧟 Algunas apps falsas pueden tener virus o espiar lo que haces.",
                R.drawable.ic_alerta
            ),
            Triple(
                "WiFi gratis... ¿seguro?",
                "📶 No te conectes a redes WiFi abiertas sin saber si son confiables.\n👀 Alguien podría espiar lo que haces o robar información.",
                R.drawable.ic_wifi_alert
            )
        )
        val btnMarcarLeido: Button = view.findViewById(R.id.btnMarcarLeido)
        btnMarcarLeido.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            ProgresoManager.unlockAchievement(requireContext(), "TEMA4_LEIDO") // <-- MODIFICADO
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
