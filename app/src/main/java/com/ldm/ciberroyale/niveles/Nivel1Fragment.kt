package com.ldm.ciberroyale.niveles

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.radiobutton.MaterialRadioButton
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel1Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Nivel1Fragment : Fragment() {

    private var _binding: FragmentNivel1Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val VIDA_INICIAL_JUGADOR = 100
        private const val VIDA_INICIAL_ENEMIGO  = 100
        private const val QUIZ_MAX_SCORE       = 20
        private const val MEJORA_MAX_SCORE     = 20
        private const val COMBATE_MAX_SCORE    = 60
        private const val DANIO_SECUAZ         = 10
    }

    // Vida y turno
    private var vidaJugador = VIDA_INICIAL_JUGADOR
    private var vidaEnemigo = VIDA_INICIAL_ENEMIGO
    private var turnoJugador = true

    // Puntuaciones parciales
    private var puntuacionQuiz = 0
    private var puntuacionMejora = 0
    private var puntuacionCombate = 0

    // Quiz
    private val preguntasOriginal = listOf(
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("123456", "GatoRojo\$42", "abcd", "password"),
            1
        ),
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("maria2004", "M@ri4_2004", "Maria04", "maría"),
            1
        ),
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("pepepepe", "P3pe\$Rock!", "PePePePe", "pepe1234"),
            1
        ),
        QuizQuestion(
            "¿Cuál de estas contraseñas es más segura?",
            listOf("contraseña", "C0ntr@s3ñ4!", "1234", "contra"),
            1
        )
    )
    private var preguntas = preguntasOriginal.shuffled().toMutableList()
    private var idxPregunta = 0

    // Mejora
    private val mejoraOpciones = listOf(
        listOf("jnito2009", "Ju@nito_2009!", "miJuan*2023"),
        listOf("c0ntraseña", "C0ntr@seÑa2024", "123456"),
        listOf("123pepe", "Pep3_321!", "peperisa"),
        listOf("verano", "Pr1m4v3r4!", "Primavera2024")
    )
    private val mejoraCorrectas = listOf(
        "Ju@nito_2009!",
        "C0ntr@seÑa2024",
        "Pep3_321!",
        "Pr1m4v3r4!"
    )

    // Para controlar contraseñas usadas en combate
    private val contrasenasUsadas = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNivel1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar video en Pantalla 1
        setupVideo()

        // Configurar navegación estática de pantallas
        setupNavigation()

        // Configurar Quiz (pantalla 4)
        setupQuiz()

        // Configurar Mejora (pantallas 5.1 y 5.2)
        setupMejora()

        // Configurar Combate (pantalla 6)
        setupCombate()

        // Iniciar mostrando SOLO Pantalla 1: ATAQUE
        mostrarPantalla(Paso.ATAQUE)

        // Animación flotante de Byte en Pantalla 3
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        binding.imgByteContexto.startAnimation(floatAnim)

        // En Pantalla 7, el mismo botón se usará para Reintentar o Continuar
        binding.btnRecompContinuar.setOnClickListener {
            // Si ha perdido (menos de 50), reiniciamos el nivel
            val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
            if (finalScore < 50) {
                resetLevel()
            } else {
                // Si ha aprobado, navegamos al fragment general
                findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Asegurar que el video arrancará cuando volvamos a Pantalla 1
        binding.videoViper.start()
    }

    override fun onPause() {
        super.onPause()
        // Pausar el video si dejamos el Fragment o cambiamos de pantalla
        binding.videoViper.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Configura el VideoView para que reproduzca el recurso R.raw.viper_attack en bucle sin sonido */
    private fun setupVideo() = with(binding) {
        val uri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.viper_attack}")
        videoViper.setVideoURI(uri)
        videoViper.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.start()
        }
        videoViper.setOnCompletionListener { it.start() }
    }

    /** Configura los botones de navegación "CONTINUAR", "¡Sí, quiero ayudar!", "No cuentes conmigo", "Estoy listo" */
    private fun setupNavigation() = with(binding) {
        // Desde Pantalla 1 -> 2
        btnSiguienteAtaque.setOnClickListener {
            mostrarPantalla(Paso.BYTE)
        }
        // Desde Pantalla 2 -> 3
        btnUnirse.setOnClickListener {
            mostrarPantalla(Paso.CONTEXTO)
        }
        btnRechazar.setOnClickListener {
            Toast.makeText(requireContext(), "¡El mundo te necesita!", Toast.LENGTH_SHORT).show()
        }
        // Desde Pantalla 3 -> 4 (reiniciar Quiz)
        btnContextoListo.setOnClickListener {
            puntuacionQuiz = 0
            idxPregunta = 0
            preguntas = preguntasOriginal.shuffled().toMutableList()
            cargarPregunta()
            mostrarPantalla(Paso.QUIZ)
        }
    }

    /** Configura la lógica del Quiz: habilitar botón al seleccionar, avanzar preguntas, sumar 5 pts por respuesta correcta */
    private fun setupQuiz() = with(binding) {
        binding.rgOpciones.setOnCheckedChangeListener { _, _ ->
            binding.btnSiguiente.isEnabled = true
        }
        binding.btnSiguiente.setOnClickListener {
            // Obtener RadioButton seleccionado
            val sel = binding.rgOpciones.findViewById<android.widget.RadioButton>(
                binding.rgOpciones.checkedRadioButtonId
            )
            // Si su tag == true, sumar 5 pts
            if (sel?.tag == true) {
                puntuacionQuiz += 5
            }
            idxPregunta++
            if (idxPregunta < preguntas.size) {
                cargarPregunta()
            } else {
                // Terminado Quiz
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.resultado_bien
                    ) + ": $puntuacionQuiz/${preguntas.size * 5} pts",
                    Toast.LENGTH_SHORT
                ).show()
                // Reiniciar Mejora
                puntuacionMejora = 0
                mostrarPantalla(Paso.MEJ1)
            }
            binding.btnSiguiente.isEnabled = false
        }
    }

    /** Carga en el layout la pregunta idxPregunta de la lista 'preguntas', barajando opciones */
    private fun cargarPregunta() = with(binding) {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto
        rgOpciones.removeAllViews()

        q.opciones.mapIndexed { i, texto ->
            MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                this.text = texto
                // Tag = true si i == opción correcta
                this.tag = (i == q.opcionCorrecta)
                setTextColor(Color.BLACK)
                setButtonTintList(
                    ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.purple_700)
                    )
                )
            }
        }
            .shuffled()
            .forEach { rgOpciones.addView(it) }

        rgOpciones.clearCheck()
        btnSiguiente.isEnabled = false

        // Actualiza título y barra de progreso
        tvQuizTitle.text = getString(
            R.string.quiz_titulo,
            idxPregunta + 1,
            preguntas.size
        )
        val percent = (idxPregunta + 1) * 100 / preguntas.size
        progressQuiz.setProgressCompat(percent, true)
    }

    /** Configura los Spinners de Mejora con un único "– Elige una opción –" + opciones, sumando 5 pts por cada elección correcta */
    private fun setupMejora() = with(binding) {
        // Deshabilitados al inicio
        btnMejora1Next.isEnabled = false
        btnMejora2Next.isEnabled = false

        // Función de extensión para inicializar Spinner
        fun Spinner.initSimple(opts: List<String>) {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listOf("-- Elige una opción --") + opts
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            // Evitar disparar onItemSelectedListener al inflar
            setSelection(0, false)
        }

        // Inicializar Spinners de Mejora1
        spinnerM1P1.initSimple(mejoraOpciones[0])
        spinnerM1P2.initSimple(mejoraOpciones[1])

        // Listener común para habilitar botón "Siguiente"
        val escuchaMejora1 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                val sel1 = spinnerM1P1.selectedItemPosition > 0
                val sel2 = spinnerM1P2.selectedItemPosition > 0
                btnMejora1Next.isEnabled = sel1 && sel2
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerM1P1.onItemSelectedListener = escuchaMejora1
        spinnerM1P2.onItemSelectedListener = escuchaMejora1

        btnMejora1Next.setOnClickListener {
            // Cada correcta suma 5 pts (hay 2 preguntas en este bloque)
            if (spinnerM1P1.selectedItem == mejoraCorrectas[0]) puntuacionMejora += 5
            if (spinnerM1P2.selectedItem == mejoraCorrectas[1]) puntuacionMejora += 5
            // Avanzar a Mejora2
            mostrarPantalla(Paso.MEJ2)
        }

        // Inicializar Spinners de Mejora2
        spinnerM2P1.initSimple(mejoraOpciones[2])
        spinnerM2P2.initSimple(mejoraOpciones[3])

        val escuchaMejora2 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                val sel1 = spinnerM2P1.selectedItemPosition > 0
                val sel2 = spinnerM2P2.selectedItemPosition > 0
                btnMejora2Next.isEnabled = sel1 && sel2
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerM2P1.onItemSelectedListener = escuchaMejora2
        spinnerM2P2.onItemSelectedListener = escuchaMejora2

        btnMejora2Next.setOnClickListener {
            // Cada correcta suma 5 pts (hay 2 preguntas en este bloque)
            if (spinnerM2P1.selectedItem == mejoraCorrectas[2]) puntuacionMejora += 5
            if (spinnerM2P2.selectedItem == mejoraCorrectas[3]) puntuacionMejora += 5

            Toast.makeText(
                requireContext(),
                "${getString(R.string.mejora_title1)}: $puntuacionMejora/$MEJORA_MAX_SCORE pts",
                Toast.LENGTH_SHORT
            ).show()

            // Iniciar Combate
            iniciarCombate()
            mostrarPantalla(Paso.COMBATE)
        }
    }

    /** Configura el botón Atacar en Combate */
    private fun setupCombate() = with(binding) {
        btnAtacar.setOnClickListener {
            playerTurn()
        }
    }

    /** Inicia Combate reiniciando vidas, borrando contraseñas usadas y determinando quien empieza */
    private fun iniciarCombate() = with(binding) {
        vidaJugador = VIDA_INICIAL_JUGADOR
        vidaEnemigo = VIDA_INICIAL_ENEMIGO
        contrasenasUsadas.clear()
        barraVidaJugador.setProgressCompat(vidaJugador, true)
        barraVidaEnemigo.setProgressCompat(vidaEnemigo, true)

        turnoJugador = (0..1).random() == 0
        btnAtacar.isEnabled = turnoJugador
        textoNarracion.text = if (turnoJugador) {
            "¡Empiezas tú!"
        } else {
            "CobraX ataca primero..."
        }

        if (!turnoJugador) {
            lifecycleScope.launch {
                delay(1500)
                enemyTurn()
            }
        }
    }

    /** Lógica del turno del jugador: valida contraseña, calcula daño, actualiza barras, controla fin de combate */
    private fun playerTurn() {
        if (!turnoJugador) return

        val pw = binding.inputContrasena.text.toString().trim()
        if (pw.isBlank()) {
            binding.textoNarracion.text = getString(R.string.mensaje_escribe_pw)
            return
        }
        if (!contrasenasUsadas.add(pw)) {
            binding.textoNarracion.text = getString(R.string.mensaje_pw_repetida)
            return
        }

        val damage = calcularDano(pw)
        if (damage == 0) {
            binding.textoNarracion.text = getString(R.string.mensaje_demasiado_debil)
        } else {
            vidaEnemigo -= damage
            binding.barraVidaEnemigo.setProgressCompat(vidaEnemigo.coerceAtLeast(0), true)
            binding.textoNarracion.text = getString(R.string.mensaje_damage, damage)
        }

        // Limpiar el campo y ocultar teclado
        binding.inputContrasena.setText("")
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.inputContrasena.windowToken, 0)

        if (vidaEnemigo <= 0) {
            // *** Lógica modificada: asignar 60 o 30 puntos según la vida restante ***

            val mitadVida = VIDA_INICIAL_JUGADOR / 2
            puntuacionCombate = when {
                vidaJugador > mitadVida -> {
                    // Si queda más de la mitad de la vida (strictly greater), 60 puntos
                    COMBATE_MAX_SCORE
                }
                vidaJugador in 1..mitadVida -> {
                    // Si queda de 1 hasta la mitad (inclusive), 30 puntos
                    COMBATE_MAX_SCORE / 2
                }
                else -> {
                    // Si vidaJugador <= 0 (por seguridad), 0 puntos
                    0
                }
            }
            handleLevelCompletion()
            return
        }

        turnoJugador = false
        binding.btnAtacar.isEnabled = false
        lifecycleScope.launch {
            delay(1500)
            enemyTurn()
        }
    }

    /** Lógica del turno del enemigo: resta 10 pts al jugador, actualiza barra, controla si el jugador pierde */
    private fun enemyTurn() = with(binding) {
        vidaJugador -= DANIO_SECUAZ
        binding.barraVidaJugador.setProgressCompat(vidaJugador.coerceAtLeast(0), true)
        binding.textoNarracion.text = getString(R.string.mensaje_enemigo_ataca, DANIO_SECUAZ)

        if (vidaJugador <= 0) {
            // Si pierdes, puntuación de combate = 0
            puntuacionCombate = 0
            handleLevelCompletion()
            return
        }
        turnoJugador = true
        binding.btnAtacar.isEnabled = true
    }

    /**
     * Calcula el daño de 'pw' según longitud, mayúsculas, dígitos y símbolos.
     * Máximo 30 pts, pero ya no se traduce directamente en la puntuación final del combate.
     */
    private fun calcularDano(pw: String): Int {
        var s = 0
        if (pw.length >= 12) s += 10
        if (pw.any { it.isUpperCase() }) s += 5
        if (pw.any { it.isDigit() }) s += 5
        if (pw.any { "!@#$%^&*".contains(it) }) s += 10
        return s.coerceAtMost(30)
    }

    /** Cuando el combate termina (tanto victoria como derrota), calculamos la puntuación final y mostramos pantalla de recompensa */
    private fun handleLevelCompletion() {
        // Sumar puntuaciones parciales: Quiz (máx. 20) + Mejora (máx. 20) + Combate (0, 30 o 60)
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        // Mostrar Pantalla 7: Recompensa/Derrota
        mostrarPantalla(Paso.RECOMP)
        // Configurar textos e imagen según rango
        binding.tvPuntuacionFinal.text = getString(R.string.puntuacion_total, finalScore)

        when {
            finalScore < 50 -> {
                // Ha perdido → Menor de 50
                binding.tvRecompensaTitulo.text = getString(R.string.derrota_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_mal)
                binding.imgConfeti.setImageResource(R.drawable.ic_alerta)
                binding.btnRecompContinuar.text = getString(R.string.btn_reintentar)
            }
            finalScore in 50..70 -> {
                // Entre 50 y 70
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_bien)
                binding.imgConfeti.setImageResource(R.drawable.ic_actor)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore in 71..99 -> {
                // Entre 71 y 99
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_muy_bien)
                binding.imgConfeti.setImageResource(R.drawable.ic_actor)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore >= 100 -> {
                // 100 puntos
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_perfecto)
                binding.imgConfeti.setImageResource(R.drawable.ic_actor)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
        }
    }

    /** Reinicia TODO el nivel para que el usuario lo vuelva a intentar desde Pantalla 1 */
    private fun resetLevel() {
        // Reiniciar puntuaciones parciales
        puntuacionQuiz = 0
        puntuacionMejora = 0
        puntuacionCombate = 0

        // Reiniciar Quiz
        idxPregunta = 0
        preguntas = preguntasOriginal.shuffled().toMutableList()

        // Reiniciar Mejora (spinners volverán a su posición 0 cuando se muestre la pantalla de Mejora)
        // Reiniciar Combate
        contrasenasUsadas.clear()

        // Volver a Pantalla 1
        mostrarPantalla(Paso.ATAQUE)
    }

    /**
     * Muestra solamente la pantalla correspondiente a 'paso' ocultando el resto.
     * Además, arranca o pausa el video en función de si ATAQUE está visible.
     */
    private fun mostrarPantalla(paso: Paso) = with(binding) {
        // 1) Ocultar todas las pantallas
        listOf(
            scrollPantallaAtaque,
            scrollPantallaByte,
            scrollPantallaContexto,
            scrollPantallaQuiz,
            scrollPantallaMejora1,
            scrollPantallaMejora2,
            scrollPantallaCombate,
            scrollPantallaRecomp
        ).forEach { it.isVisible = false }

        // 2) Si mostramos ATAQUE, arrancar video; si no, pausarlo.
        if (paso == Paso.ATAQUE) {
            videoViper.start()
            scrollPantallaAtaque.isVisible = true
        } else {
            videoViper.pause()
            when (paso) {
                Paso.BYTE -> scrollPantallaByte.isVisible = true
                Paso.CONTEXTO -> scrollPantallaContexto.isVisible = true
                Paso.QUIZ -> scrollPantallaQuiz.isVisible = true
                Paso.MEJ1 -> {
                    scrollPantallaMejora1.isVisible = true
                    progressMejora1.max = 100
                    progressMejora1.setProgressCompat(50, true)
                }
                Paso.MEJ2 -> {
                    scrollPantallaMejora2.isVisible = true
                    progressMejora2.max = 100
                    progressMejora2.setProgressCompat(100, true)
                }
                Paso.COMBATE -> scrollPantallaCombate.isVisible = true
                Paso.RECOMP -> scrollPantallaRecomp.isVisible = true
                else -> {}
            }
        }
    }

    /** Enum para identificar cada pantalla dentro del mismo Fragment */
    enum class Paso {
        ATAQUE, BYTE, CONTEXTO, QUIZ, MEJ1, MEJ2, COMBATE, RECOMP
    }

    /** Clase de datos para una pregunta de Quiz */
    private data class QuizQuestion(
        val texto: String,
        val opciones: List<String>,
        val opcionCorrecta: Int
    )
}
