package com.ldm.ciberroyale

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Nivel1Fragment organiza el flujo completo del Nivel 1:
 * 1) Animación de ViperWare
 * 2) Presentación de Byte
 * 3) Contexto del nivel
 * 4) Quiz de contraseñas seguras
 * 5) Mejora de contraseñas (dos bloques)
 * 6) Combate final al estilo Pokémon
 */
class Nivel1Fragment : Fragment() {

    // MARK: - UI Views (pantallas)
    private lateinit var pantallaAtaque: View
    private lateinit var pantallaByte: View
    private lateinit var pantallaContexto: View
    private lateinit var pantallaQuiz: View
    private lateinit var pantallaMejora1: View
    private lateinit var pantallaMejora2: View
    private lateinit var pantallaCombate: ConstraintLayout

    // Quiz
    private lateinit var tvPregunta: TextView
    private lateinit var rgOpciones: RadioGroup
    private lateinit var btnSiguiente: Button

    // Mejora contraseñas bloque 1
    private lateinit var spinnerM1P1: Spinner
    private lateinit var spinnerM1P2: Spinner
    private lateinit var btnMejora1Next: Button

    // Mejora contraseñas bloque 2
    private lateinit var spinnerM2P1: Spinner
    private lateinit var spinnerM2P2: Spinner
    private lateinit var btnMejora2Next: Button

    // Combate
    private lateinit var imgEnemigo: ImageView
    private lateinit var barraEnemigo: ProgressBar
    private lateinit var imgJugador: ImageView
    private lateinit var barraJugador: ProgressBar
    private lateinit var inputContrasena: EditText
    private lateinit var botonAtacar: Button
    private lateinit var textoNarracion: TextView

    // Estado
    private var vidaJugador = 100
    private var vidaEnemigo = 80
    private var turnoJugador = true
    private val contrasenasUsadas = mutableSetOf<String>()
    private val danoSecuaz = 10

    private var idxPregunta = 0
    private var puntuacionQuiz = 0
    private var puntuacionMejora = 0

    // Datos del quiz
    private val preguntasOriginal = listOf(
        QuizQuestion("¿Cuál de estas contraseñas es más segura?",
            listOf("123456","GatoRojo$42","abcd","password"),1),
        QuizQuestion("¿Cuál de estas es más segura?",
            listOf("maria2004","M@ri4_2004","Maria04","maría"),1),
        QuizQuestion("¿Cuál de estas es más segura?",
            listOf("pepepepe","P3pe\$Rock!","PePePePe","pepe1234"),1),
        QuizQuestion("¿Cuál de estas contraseñas es más segura?",
            listOf("contraseña","C0ntr@s3ñ4!","1234","contra"),1)
    )
    private val preguntas = preguntasOriginal.shuffled().toMutableList()

    // Opciones y respuestas para mejora
    private val mejoraOpciones = listOf(
        listOf("juanito2009","Juanito_2009!","miJuan*2023"),
        listOf("contraseña","C0ntr@seÑa2024","123456"),
        listOf("pepe123","Pep3_321!","pepeloco"),
        listOf("primavera","Pr1m4v3r4!","Primavera2024")
    )
    private val mejoraCorrectas = listOf("Juanito_2009!","C0ntr@seÑa2024","Pep3_321!","Pr1m4v3r4!")

    // MARK: - Lifecycle
    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View? =
        inflater.inflate(R.layout.fragment_nivel1, c, false)

    override fun onViewCreated(view: View, saved: Bundle?) {
        super.onViewCreated(view, saved)
        bindViews(view)
        setupNavigation()
        setupQuiz()
        setupMejora()
        setupCombate()
    }

    // MARK: - Binding
    private fun bindViews(v: View) {
        pantallaAtaque   = v.findViewById(R.id.pantalla_ataque)
        pantallaByte     = v.findViewById(R.id.pantalla_byte)
        pantallaContexto = v.findViewById(R.id.pantalla_contexto)
        pantallaQuiz     = v.findViewById(R.id.pantalla_quiz)
        pantallaMejora1  = v.findViewById(R.id.pantalla_mejora1)
        pantallaMejora2  = v.findViewById(R.id.pantalla_mejora2)
        pantallaCombate  = v.findViewById(R.id.pantalla_combate)

        tvPregunta   = v.findViewById(R.id.tvPregunta)
        rgOpciones   = v.findViewById(R.id.rgOpciones)
        btnSiguiente = v.findViewById(R.id.btnSiguiente)

        spinnerM1P1    = v.findViewById(R.id.spinner_m1_p1)
        spinnerM1P2    = v.findViewById(R.id.spinner_m1_p2)
        btnMejora1Next = v.findViewById(R.id.btnMejora1Next)

        spinnerM2P1    = v.findViewById(R.id.spinner_m2_p1)
        spinnerM2P2    = v.findViewById(R.id.spinner_m2_p2)
        btnMejora2Next = v.findViewById(R.id.btnMejora2Next)

        imgEnemigo      = v.findViewById(R.id.imgEnemigo)
        barraEnemigo    = v.findViewById(R.id.barraVidaEnemigo)
        imgJugador      = v.findViewById(R.id.imgJugador)
        barraJugador    = v.findViewById(R.id.barraVidaJugador)
        inputContrasena = v.findViewById(R.id.inputContrasena)
        botonAtacar     = v.findViewById(R.id.btnAtacar)
        textoNarracion  = v.findViewById(R.id.textoNarracion)
    }

    // MARK: - Navigation
    private fun setupNavigation() {
        view?.findViewById<Button>(R.id.btnSiguienteAtaque)
            ?.setOnClickListener { mostrarPaso(Paso.BYTE) }
        view?.findViewById<Button>(R.id.btnUnirse)
            ?.setOnClickListener { mostrarPaso(Paso.CONTEXTO) }
        view?.findViewById<Button>(R.id.btnContextoListo)
            ?.setOnClickListener {
                puntuacionQuiz=0; idxPregunta=0
                mostrarPaso(Paso.QUIZ)
                cargarPregunta()
            }
        view?.findViewById<Button>(R.id.btnRechazar)
            ?.setOnClickListener {
                Toast.makeText(requireContext(), "¡El mundo te necesita!", Toast.LENGTH_SHORT).show()
            }
    }

    // MARK: - Quiz logic
    private fun setupQuiz() {
        rgOpciones.setOnCheckedChangeListener { _, _ -> btnSiguiente.isEnabled = true }
        btnSiguiente.setOnClickListener {
            val sel = rgOpciones.findViewById<RadioButton>(rgOpciones.checkedRadioButtonId)
            if (sel?.tag == true) puntuacionQuiz += preguntas[idxPregunta].puntos
            idxPregunta++
            if (idxPregunta < preguntas.size) cargarPregunta()
            else {
                Toast.makeText(requireContext(), "Quiz: $puntuacionQuiz/${preguntas.size*5} pts", Toast.LENGTH_SHORT).show()
                mostrarPaso(Paso.MEJ1)
            }
            btnSiguiente.isEnabled = false
        }
    }

    private fun cargarPregunta() {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto
        rgOpciones.removeAllViews()
        q.opciones.mapIndexed { i, txt -> txt to (i == q.opcionCorrecta) }
            .shuffled().forEach {
                RadioButton(requireContext()).apply {
                    text = it.first; tag = it.second; id = View.generateViewId()
                }.also { rgOpciones.addView(it) }
            }
        rgOpciones.clearCheck()
        btnSiguiente.isEnabled = false
    }

    // MARK: - Mejora blocks
    private fun setupMejora() {
        fun config(sp: Spinner, opts: List<String>) = sp.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opts).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        config(spinnerM1P1, mejoraOpciones[0]); config(spinnerM1P2, mejoraOpciones[1])
        config(spinnerM2P1, mejoraOpciones[2]); config(spinnerM2P2, mejoraOpciones[3])

        val l1 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                btnMejora1Next.isEnabled = spinnerM1P1.selectedItemPosition > 0 && spinnerM1P2.selectedItemPosition > 0
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        spinnerM1P1.onItemSelectedListener = l1; spinnerM1P2.onItemSelectedListener = l1
        btnMejora1Next.setOnClickListener {
            if (spinnerM1P1.selectedItem == mejoraCorrectas[0]) puntuacionMejora += 3
            if (spinnerM1P2.selectedItem == mejoraCorrectas[1]) puntuacionMejora += 3
            mostrarPaso(Paso.MEJ2)
        }

        val l2 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                btnMejora2Next.isEnabled = spinnerM2P1.selectedItemPosition > 0 && spinnerM2P2.selectedItemPosition > 0
            }
            override fun onNothingSelected(p: AdapterView<*>) {}
        }
        spinnerM2P1.onItemSelectedListener = l2; spinnerM2P2.onItemSelectedListener = l2
        btnMejora2Next.setOnClickListener {
            if (spinnerM2P1.selectedItem == mejoraCorrectas[2]) puntuacionMejora += 3
            if (spinnerM2P2.selectedItem == mejoraCorrectas[3]) puntuacionMejora += 3
            Toast.makeText(requireContext(), "Mejora: $puntuacionMejora/12 pts", Toast.LENGTH_SHORT).show()
            mostrarPaso(Paso.COMBATE)
            iniciarCombate()
        }
    }

    // MARK: - Combate logic
    private fun setupCombate() = botonAtacar.setOnClickListener { playerTurn() }

    /**
     * Inicializa el combate y lanza el primer turno.
     */
    private fun iniciarCombate() {
        turnoJugador = (0..1).random() == 0
        textoNarracion.text = if (turnoJugador) "¡Empiezas tú!" else "CobraX ataca primero..."
        if (!turnoJugador) Handler().postDelayed({ enemyTurn() }, 1500)
    }

    private fun playerTurn() {
        if (!turnoJugador) return
        val pw = inputContrasena.text.toString().takeIf { it.isNotBlank() }
            ?: return textoNarracion.setText("Escribe una contraseña antes").also { return }
        if (!contrasenasUsadas.add(pw)) return textoNarracion.setText("No repitas contraseñas").also { return }
        val daño = calcularDano(pw)
        vidaEnemigo -= daño; barraEnemigo.progress = vidaEnemigo
        textoNarracion.text = "Le haces $daño pts"
        if (vidaEnemigo <= 0) return victory()
        turnoJugador = false; Handler().postDelayed({ enemyTurn() }, 1500)
    }

    private fun enemyTurn() {
        vidaJugador -= danoSecuaz; barraJugador.progress = vidaJugador
        textoNarracion.text = "CobraX inflige $danoSecuaz pts"
        if (vidaJugador <= 0) return defeat()
        turnoJugador = true
    }

    private fun victory() {
        textoNarracion.text = "🎉 ¡Has ganado!"
        botonAtacar.isEnabled = false
        Handler().postDelayed({ findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment) }, 2000)
    }

    private fun defeat() {
        textoNarracion.text = "😢 Derrotado. Intenta otra vez."
        botonAtacar.isEnabled = false
    }

    private fun calcularDano(pw: String): Int {
        var s = 0
        if (pw.length >= 12) s += 10
        if (pw.any { it.isUpperCase() }) s += 5
        if (pw.any { it.isDigit() }) s += 5
        if (pw.any { "!@#$%^&*".contains(it) }) s += 10
        return s.coerceAtMost(30)
    }

    private fun mostrarPaso(paso: Paso) {
        listOf(pantallaAtaque,pantallaByte,pantallaContexto,
            pantallaQuiz,pantallaMejora1,pantallaMejora2,pantallaCombate)
            .forEach { it.visibility = View.GONE }
        when (paso) {
            Paso.ATAQUE   -> pantallaAtaque.visibility = View.VISIBLE
            Paso.BYTE     -> pantallaByte.visibility   = View.VISIBLE
            Paso.CONTEXTO -> pantallaContexto.visibility = View.VISIBLE
            Paso.QUIZ     -> pantallaQuiz.visibility   = View.VISIBLE
            Paso.MEJ1     -> pantallaMejora1.visibility = View.VISIBLE
            Paso.MEJ2     -> pantallaMejora2.visibility = View.VISIBLE
            Paso.COMBATE  -> pantallaCombate.visibility = View.VISIBLE
        }
    }
}

// Navegación interna
enum class Paso { ATAQUE,BYTE,CONTEXTO,QUIZ,MEJ1,MEJ2,COMBATE }

// Modelo de pregunta
private data class QuizQuestion(
    val texto: String,
    val opciones: List<String>,
    val opcionCorrecta: Int,
    val puntos: Int = 5
)