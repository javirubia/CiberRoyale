package com.ldm.ciberroyale

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    // Views de cada fase
    private lateinit var pantallaIntro: View
    private lateinit var pantallaDetec: View
    private lateinit var pantallaSimul: View
    private lateinit var pantallaCrear: View
    private lateinit var pantallaQuiz: View
    private lateinit var pantallaRecomp: View

    // Fase 2 – Detección
    private lateinit var tvDetecMensaje: TextView
    private lateinit var tvDetecProgreso: TextView
    private lateinit var btnSeguro: Button
    private lateinit var btnPhishing: Button
    private val ejemplos = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to true,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña para actualizar sistema" to false,
        "Notificación de tu UPS: tu paquete está en camino. Con enlace a tracking oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private var idxDetec = 0
    private var puntosDetec = 0

    // Fase 3 – Simulación
    private lateinit var btnSimulClick: Button
    private lateinit var btnSimulNoClick: Button
    private lateinit var tvSimulFeedback: TextView
    private lateinit var btnSimulSiguiente: Button
    private var simulElegido = false

    // Fase 4 – Creación de mensaje seguro
    private lateinit var etAsunto: EditText
    private lateinit var etCuerpo: EditText
    private lateinit var btnCrearSiguiente: Button
    private lateinit var tvCrearFeedback: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Bind de pantallas
        pantallaIntro  = view.findViewById(R.id.pantalla_intro)
        pantallaDetec  = view.findViewById(R.id.pantalla_detec)
        pantallaSimul  = view.findViewById(R.id.pantalla_simul)
        pantallaCrear  = view.findViewById(R.id.pantalla_crear)
        pantallaQuiz   = view.findViewById(R.id.pantalla_quiz)
        pantallaRecomp = view.findViewById(R.id.pantalla_recomp)

        // 2) Bind Fase 2 – Detección
        tvDetecMensaje  = view.findViewById(R.id.tvDetecMensaje)
        tvDetecProgreso = view.findViewById(R.id.tvDetecProgreso)
        btnSeguro       = view.findViewById(R.id.btnSeguro)
        btnPhishing     = view.findViewById(R.id.btnPhishing)
        actualizarDetec()
        btnSeguro.setOnClickListener   { evaluarDetec(true) }
        btnPhishing.setOnClickListener { evaluarDetec(false) }

        // 3) Bind Fase 3 – Simulación
        btnSimulClick     = view.findViewById(R.id.btnSimulClick)
        btnSimulNoClick   = view.findViewById(R.id.btnSimulNoClick)
        tvSimulFeedback   = view.findViewById(R.id.tvSimulFeedback)
        btnSimulSiguiente = view.findViewById(R.id.btnSimulSiguiente)

        btnSimulClick.setOnClickListener {
            if (!simulElegido) mostrarFeedbackSimul(clicked = true)
        }
        btnSimulNoClick.setOnClickListener {
            if (!simulElegido) mostrarFeedbackSimul(clicked = false)
        }
        btnSimulSiguiente.setOnClickListener {
            mostrarPaso(PasoPhishing.CREAR)
        }

        // 4) Bind Fase 4 – Creación de mensaje
        etAsunto          = view.findViewById(R.id.etAsunto)
        etCuerpo          = view.findViewById(R.id.etCuerpo)
        btnCrearSiguiente = view.findViewById(R.id.btnCrearSiguiente)
        // Creamos dinámicamente el TextView de feedback
        tvCrearFeedback = TextView(requireContext()).apply {
            textSize = 14f
            setTextColor(Color.RED)
            visibility = View.GONE
        }
        // Lo insertamos justo después del cuerpo en el layout de la fase 4
        (view.findViewById<LinearLayout>(R.id.pantalla_crear))
            .addView(tvCrearFeedback, 3)

        btnCrearSiguiente.isEnabled = false

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = validarCrear()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etAsunto.addTextChangedListener(watcher)
        etCuerpo.addTextChangedListener(watcher)
        btnCrearSiguiente.setOnClickListener {
            mostrarPaso(PasoPhishing.QUIZ)
        }

        // 5) Arrancamos en la intro
        mostrarPaso(PasoPhishing.INTRO)

        // 6) Listeners de avance genérico
        view.findViewById<Button>(R.id.btnIntroSiguiente)
            .setOnClickListener { mostrarPaso(PasoPhishing.DETEC) }
        view.findViewById<Button>(R.id.btnDetecSiguiente)
            .setOnClickListener { mostrarPaso(PasoPhishing.SIMUL) }
        view.findViewById<Button>(R.id.btnQuizSiguiente)
            .setOnClickListener { mostrarPaso(PasoPhishing.RECOMP) }
        view.findViewById<Button>(R.id.btnRecompContinuar)
            .setOnClickListener {
                findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
            }
    }

    /** Enum con las fases del nivel */
    private enum class PasoPhishing {
        INTRO, DETEC, SIMUL, CREAR, QUIZ, RECOMP
    }

    /** Fase 2: actualiza el mensaje y progreso */
    private fun actualizarDetec() {
        val (texto, _) = ejemplos[idxDetec]
        tvDetecMensaje.text  = texto
        tvDetecProgreso.text = "${idxDetec + 1}/${ejemplos.size}"
    }

    /** Fase 2: evalúa y avanza o habilita botón “Siguiente” */
    private fun evaluarDetec(seleccionSeguro: Boolean) {
        if (seleccionSeguro == ejemplos[idxDetec].second) puntosDetec++
        idxDetec++
        if (idxDetec < ejemplos.size) {
            actualizarDetec()
        } else {
            Toast.makeText(
                requireContext(),
                "Has acertado $puntosDetec de ${ejemplos.size}",
                Toast.LENGTH_SHORT
            ).show()
            view?.findViewById<Button>(R.id.btnDetecSiguiente)?.isEnabled = true
        }
    }

    /** Fase 3: muestra feedback y habilita botón “Siguiente” */
    private fun mostrarFeedbackSimul(clicked: Boolean) {
        simulElegido = true
        tvSimulFeedback.visibility = View.VISIBLE
        if (clicked) {
            tvSimulFeedback.text = "¡Ay! Era phishing: han intentado robarte datos."
        } else {
            tvSimulFeedback.text = "¡Bien! Has evitado un posible robo de datos."
        }
        btnSimulClick.isEnabled     = false
        btnSimulNoClick.isEnabled   = false
        btnSimulSiguiente.isEnabled = true
    }

    /** Fase 4: mini-linter para asunto y cuerpo */
    private fun validarCrear() {
        val asunto = etAsunto.text.toString().trim()
        val cuerpo = etCuerpo.text.toString().trim()

        if (asunto.isEmpty() || cuerpo.isEmpty()) {
            tvCrearFeedback.visibility = View.GONE
            btnCrearSiguiente.isEnabled = false
            return
        }

        val linkPattern = Regex("http[s]?://[^\\s]+")
        val todoMayus = asunto == asunto.uppercase() || cuerpo == cuerpo.uppercase()

        when {
            linkPattern.containsMatchIn(asunto) || linkPattern.containsMatchIn(cuerpo) -> {
                tvCrearFeedback.text = "⚠️ No incluyas enlaces directos."
                tvCrearFeedback.visibility = View.VISIBLE
                btnCrearSiguiente.isEnabled = false
            }
            todoMayus -> {
                tvCrearFeedback.text = "⚠️ Evita escribir todo en mayúsculas."
                tvCrearFeedback.visibility = View.VISIBLE
                btnCrearSiguiente.isEnabled = false
            }
            else -> {
                tvCrearFeedback.visibility = View.GONE
                btnCrearSiguiente.isEnabled = true
            }
        }
    }

    /** Oculta todas las pantallas y muestra solo la actual */
    private fun mostrarPaso(paso: PasoPhishing) {
        listOf(
            pantallaIntro,
            pantallaDetec,
            pantallaSimul,
            pantallaCrear,
            pantallaQuiz,
            pantallaRecomp
        ).forEach { it.visibility = View.GONE }

        when (paso) {
            PasoPhishing.INTRO  -> pantallaIntro.visibility  = View.VISIBLE
            PasoPhishing.DETEC  -> pantallaDetec.visibility  = View.VISIBLE
            PasoPhishing.SIMUL  -> pantallaSimul.visibility  = View.VISIBLE
            PasoPhishing.CREAR  -> pantallaCrear.visibility  = View.VISIBLE
            PasoPhishing.QUIZ   -> pantallaQuiz.visibility   = View.VISIBLE
            PasoPhishing.RECOMP -> pantallaRecomp.visibility = View.VISIBLE
        }
    }
}
