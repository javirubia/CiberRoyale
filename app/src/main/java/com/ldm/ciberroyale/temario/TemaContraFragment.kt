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
import androidx.appcompat.app.AlertDialog
import com.ldm.ciberroyale.ProgresoManager
import com.ldm.ciberroyale.R

class TemaContraFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tema_contra, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val containerLayout = view.findViewById<LinearLayout>(R.id.containerLayout)
        val btnVolver = view.findViewById<ImageButton>(R.id.btnBack)
        btnVolver.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val temas = listOf(
            Triple("¿Qué es una Contraseña Segura?",
                "Una contraseña segura es como una puerta mágica con muchos candados. Debe tener 12 letras o más, con mayúsculas, números y símbolos. ¡Nada de tu nombre ni fechas!",
                R.drawable.ic_candado
            ),
            Triple("Ejemplos de Contraseñas",
                "🟥 Débiles: 12345, jose1990, contraseña\n" +
                        "🟩 Fuertes: PeRr0AzUl!85, A5!zT&9nQx#\n" +
                        "💡 Consejo: Usa frases locas como 'Pat0\$AzulEstrell4C0che'.",
                R.drawable.ic_claves
            ),
            Triple("Consejos para Gestionarlas",
                "🔐 No repitas contraseñas\n📲 Usa un gestor de contraseñas\n🔁 Cámbialas regularmente\n❗ No las compartas",
                R.drawable.ic_consejo
            ),
            Triple("¿Por qué son importantes?",
                "⚠️ Contraseñas débiles permiten robos de cuentas.\n🚨 Si repites la misma en varias webs, todas se pueden ver comprometidas.\n👾 ¡Los hackers usan robots para adivinar contraseñas!",
                R.drawable.ic_alerta
            )
        )
        val btnMarcarLeido: Button = view.findViewById(R.id.btnMarcarLeido)
        btnMarcarLeido.setOnClickListener {
            // Desbloquear el logro correspondiente (TEMA1_LEIDO)
            ProgresoManager.unlockAchievement("TEMA1_LEIDO")
            // Opcional: mostrar un Toast de confirmación
            Toast.makeText(requireContext(),
                "¡Has desbloqueado el logro Tema 1 completado!",
                Toast.LENGTH_SHORT).show()

        }

        for ((titulo, descripcion, icono) in temas) {
            val cardView = layoutInflater.inflate(R.layout.item_tarjeta_tema, containerLayout, false)
            cardView.findViewById<TextView>(R.id.tituloTarjeta).text = titulo
            cardView.findViewById<TextView>(R.id.descripcionTarjeta).text = descripcion
            cardView.findViewById<ImageView>(R.id.imagenIcono).setImageResource(icono)
            val lineaExtra = cardView.findViewById<TextView>(R.id.lineaExtraInteractiva)

            if (titulo.contains("Consejos")) {
                lineaExtra.visibility = View.VISIBLE
                lineaExtra.text = "🛡️ Activa la verificación en dos pasos (2FA)"
                lineaExtra.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("¿Qué es 2FA?")
                        .setMessage("La verificación en dos pasos es como una doble cerradura.\n\nPrimero pones tu contraseña, y luego un código que te llega al móvil o correo. ¡Así es mucho más difícil que alguien entre sin permiso!")
                        .setPositiveButton("¡Entendido!") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }

            containerLayout.addView(cardView)
        }
    }
}
