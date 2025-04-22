package com.ldm.ciberroyale

import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class Nivel1Fragment : Fragment() {

    private lateinit var pantallaAtaque: View
    private lateinit var pantallaByte: View
    private lateinit var pantallaContexto: View
    private lateinit var pantallaQuiz: View
    private lateinit var pantallaMejora: View
    private lateinit var pantallaCombate: View
    private lateinit var barraJugador: ProgressBar
    private lateinit var barraEnemigo: ProgressBar
    private lateinit var textoNarracion: TextView
    private lateinit var inputContrasena: EditText
    private lateinit var botonAtacar: Button

    private var vidaJugador = 100
    private var vidaEnemigo = 80
    private var turnoJugador = true
    private val contrasenasUsadas = mutableSetOf<String>()
    private val danoSecuaz = 10

    private lateinit var tvPregunta: TextView
    private lateinit var rgOpciones: RadioGroup
    private lateinit var btnSiguiente: Button
    private val preguntasOriginal = listOf(
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("123456", "GatoRojo\$42", "abcd", "password"),
            opcionCorrecta = 1
        ),
        QuizQuestion(
            "¿Cuál de estas es más segura?",
            listOf("maria2004", "M@ri4_2004", "Maria04", "maría"),
            opcionCorrecta = 1
        ),
        QuizQuestion(
            "¿Cuál de estas es más segura?",
            listOf("pepepepe", "P3pe\$Rock!", "PePePePe", "pepe1234"),
            opcionCorrecta = 1
        ),
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("contraseña", "C0ntr@s3ñ4!", "1234", "contra"),
            opcionCorrecta = 1
        )
    )

    // Al empezar, barajamos el orden de las preguntas
    private val preguntas = preguntasOriginal.shuffled().toMutableList()
    private var idxPregunta = 0
    private var puntuacionQuiz = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nivel1, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pantallaAtaque = view.findViewById(R.id.pantalla_ataque)
        pantallaByte = view.findViewById(R.id.pantalla_byte)
        pantallaContexto = view.findViewById(R.id.pantalla_contexto)
        pantallaQuiz = view.findViewById(R.id.pantalla_quiz)
        pantallaMejora = view.findViewById(R.id.pantalla_mejora)
        pantallaCombate = view.findViewById(R.id.pantalla_combate)
        barraJugador = view.findViewById(R.id.barraVidaJugador)
        barraEnemigo = view.findViewById(R.id.barraVidaEnemigo)
        textoNarracion = view.findViewById(R.id.textoNarracion)
        inputContrasena = view.findViewById(R.id.inputContrasena)
        botonAtacar = view.findViewById(R.id.btnAtacar)
        tvPregunta = view.findViewById(R.id.tvPregunta)
        rgOpciones    = view.findViewById(R.id.rgOpciones)
        btnSiguiente  = view.findViewById(R.id.btnSiguiente)

        rgOpciones.setOnCheckedChangeListener { _, _ ->
            btnSiguiente.isEnabled = true
        }


        view.findViewById<Button>(R.id.btnSiguienteAtaque).setOnClickListener {
            mostrarPantalla(pantallaByte)
        }

        view.findViewById<Button>(R.id.btnUnirse).setOnClickListener {
            mostrarPantalla(pantallaContexto)
        }
        view.findViewById<Button>(R.id.btnContextoListo).setOnClickListener {
            puntuacionQuiz = 0
            idxPregunta = 0
            mostrarPantalla(pantallaQuiz)
            cargarPregunta()
        }

        view.findViewById<Button>(R.id.btnRechazar).setOnClickListener {
            Toast.makeText(requireContext(), "¡Vamos, el mundo te necesita!", Toast.LENGTH_SHORT).show()
        }
        btnSiguiente.setOnClickListener {
            // 1) comprobar si la opción elegida es correcta
            val seleccionadoId = rgOpciones.checkedRadioButtonId
            val radio = rgOpciones.findViewById<RadioButton>(seleccionadoId)
            if (radio?.tag == true) {
                puntuacionQuiz += preguntas[idxPregunta].puntos
            }

            // 2) pasar a la siguiente pregunta o terminar
            idxPregunta++
            if (idxPregunta < preguntas.size) {
                cargarPregunta()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Quiz completado: $puntuacionQuiz/${preguntas.size*5} puntos",
                    Toast.LENGTH_SHORT
                ).show()
                // avanza al siguiente bloque (pantalla_mejora)
                mostrarPantalla(pantallaMejora)
            }
            btnSiguiente.isEnabled = false
        }

        val spinner1 = view.findViewById<Spinner>(R.id.spinner1)
        val spinner2 = view.findViewById<Spinner>(R.id.spinner2)
        val spinner3 = view.findViewById<Spinner>(R.id.spinner3)

        val opciones1 = listOf("juanito2009", "Juanito_2009!", "miJuan*2023")
        val opciones2 = listOf("contraseña", "C0ntr@seÑa2024", "123456")
        val opciones3 = listOf("pepe123", "Pep3_321!", "pepeloco")

        val adapter1 = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opciones1)
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner1.adapter = adapter1
        spinner2.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opciones2)
        spinner3.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opciones3)

        view.findViewById<Button>(R.id.btnValidarMejora).setOnClickListener {
            val correctas = listOf(
                spinner1.selectedItem == "Juanito_2009!",
                spinner2.selectedItem == "C0ntr@seÑa2024",
                spinner3.selectedItem == "Pep3_321!"
            ).count { it }

            if (correctas == 3) {
                Toast.makeText(requireContext(), "¡Contraseñas mejoradas! 💪", Toast.LENGTH_SHORT).show()
                // Aquí pasarás a la pantalla 6 (combate)
                mostrarPantalla(pantallaCombate)
                iniciarCombate()

            } else {
                Toast.makeText(requireContext(), "Algunas respuestas no son seguras 😬", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun mostrarPantalla(pantalla: View) {
        listOf(
            pantallaAtaque,
            pantallaByte,
            pantallaContexto,
            pantallaQuiz,
            pantallaMejora,
            pantallaCombate
        ).forEach { it.visibility = View.GONE }

        pantalla.visibility = View.VISIBLE
    }

    private fun iniciarCombate() {
        turnoJugador = (0..1).random() == 0
        textoNarracion.text = if (turnoJugador) {
            "¡Empiezas tú el combate!"
        } else {
            "CobraX ataca primero..."
        }

        if (!turnoJugador) ataqueSecuaz()

        botonAtacar.setOnClickListener {
            if (!turnoJugador) return@setOnClickListener

            val input = inputContrasena.text.toString()
            if (input.isBlank()) {
                textoNarracion.text = "Escribe una contraseña primero."
                return@setOnClickListener
            }
            if (contrasenasUsadas.contains(input)) {
                textoNarracion.text = "¡No puedes repetir contraseña!"
                return@setOnClickListener
            }

            contrasenasUsadas.add(input)
            val dano = calcularDano(input)
            vidaEnemigo -= dano
            barraEnemigo.progress = vidaEnemigo
            textoNarracion.text = "¡Le hiciste $dano de daño a CobraX! 🐍"

            if (vidaEnemigo <= 0) {
                textoNarracion.text = "🎉 ¡Has vencido a CobraX!"
                botonAtacar.isEnabled = false
                //esperar 3 segundos y navegar a la siguiente pantalla
                Handler().postDelayed({
                    findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
                }, 3000)
                return@setOnClickListener
            }

            turnoJugador = false
            Handler().postDelayed({ ataqueSecuaz() }, 1500)
        }
    }
    private fun ataqueSecuaz() {
        vidaJugador -= danoSecuaz
        barraJugador.progress = vidaJugador
        textoNarracion.text = "🐍 CobraX te atacó y te hizo $danoSecuaz de daño."

        if (vidaJugador <= 0) {
            textoNarracion.text = "😢 Has sido derrotado... Intenta de nuevo."
            botonAtacar.isEnabled = false
            return
        }

        turnoJugador = true
    }
    private fun calcularDano(pw: String): Int {
        var score = 0
        if (pw.length >= 12) score += 10
        if (pw.any { it.isUpperCase() }) score += 5
        if (pw.any { it.isDigit() }) score += 5
        if (pw.any { "!@#\$%^&*".contains(it) }) score += 10
        return score.coerceAtMost(30) // máximo daño por turno
    }

    private fun cargarPregunta() {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto

        // mezclamos opciones con flag de correctness
        val opcionesBarajadas = q.opciones
            .mapIndexed { i, txt -> txt to (i == q.opcionCorrecta) }
            .shuffled()

        rgOpciones.removeAllViews()
        opcionesBarajadas.forEach { (textoOpcion, esCorrecta) ->
            val radio = RadioButton(requireContext()).apply {
                text = textoOpcion
                tag = esCorrecta       // guardamos en tag
                textSize = 16f
                setPadding(8,16,8,16)
                id = View.generateViewId()
            }
            rgOpciones.addView(radio)
        }
        rgOpciones.clearCheck()
        btnSiguiente.isEnabled = false
    }

}

data class QuizQuestion(
    val texto: String,
    val opciones: List<String>,
    val opcionCorrecta: Int,   // índice en opciones[]
    val puntos: Int = 5        // 5 puntos cada una
)
