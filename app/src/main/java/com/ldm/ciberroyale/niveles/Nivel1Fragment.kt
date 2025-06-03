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
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNivel1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Configurar video en Pantalla 1
        setupVideo()

        // 2) Configurar navegación estática de pantallas
        setupNavigation()

        // 3) Configurar Quiz (pantalla 4)
        setupQuiz()

        // 4) Configurar Mejora (pantallas 5.1 y 5.2)
        setupMejora()

        // 5) Configurar Combate (pantalla 6)
        setupCombate()

        // 6) Iniciar mostrando SOLO Pantalla 1: ATAQUE
        mostrarPantalla(Paso.ATAQUE)

        // 7) Animación flotante de Byte en Pantalla 3 y robots
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        binding.imgByteContexto.startAnimation(floatAnim)
        binding.imgEnemigo.startAnimation(floatAnim)
        binding.imgJugador.startAnimation(floatAnim)
        // imgConfeti se animará dinámicamente en handleLevelCompletion()

        // 8) En Pantalla 7, el mismo botón se usará para Reintentar o Continuar
        binding.btnRecompContinuar.setOnClickListener {
            val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
            if (finalScore < 50) {
                resetLevel()
            } else {
                findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
            }
        }

        // 9) Configurar los botones de Info para cada sección:
        binding.btnInfoQuiz.setOnClickListener { mostrarDialogoInfoQuiz() }
        binding.btnInfoMejora1.setOnClickListener { mostrarDialogoInfoMejora1() }
        binding.btnInfoMejora2.setOnClickListener { mostrarDialogoInfoMejora2() }
        binding.btnInfoCombate.setOnClickListener { mostrarDialogoInfoCombate() }
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
        rgOpciones.setOnCheckedChangeListener { _, _ ->
            btnSiguiente.isEnabled = true
        }
        btnSiguiente.setOnClickListener {
            // Obtener RadioButton seleccionado
            val sel = rgOpciones.findViewById<android.widget.RadioButton>(
                rgOpciones.checkedRadioButtonId
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
                    getString(R.string.resultado_bien) + ": $puntuacionQuiz/${preguntas.size * 5} pts",
                    Toast.LENGTH_SHORT
                ).show()
                // Reiniciar Mejora
                puntuacionMejora = 0
                mostrarPantalla(Paso.MEJ1)
            }
            btnSiguiente.isEnabled = false
        }
    }

    /** Carga en el layout la pregunta idxPregunta de la lista 'preguntas', barajando opciones */
    private fun cargarPregunta() = with(binding) {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto

        // 1) Borrar las opciones viejas
        rgOpciones.removeAllViews()

        // 2) Por cada opción, crear un MaterialRadioButton con Orbitron Bold
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
                // Asignar Typeface de Orbitron Bold
                typeface = ResourcesCompat.getFont(requireContext(), R.font.orbitron_bold)

                // Márgenes opcionales para separación
                val lp = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 8, 0, 8)
                layoutParams = lp
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

    /** Configura los Spinners de Mejora con Orbitron Bold y un único "– Elige una opción –" */
    private fun setupMejora() = with(binding) {
        // Deshabilitados al inicio
        btnMejora1Next.isEnabled = false
        btnMejora2Next.isEnabled = false

        // Función de extensión para inicializar Spinner usando nuestro layout (@layout/spinner_item_orbitron)
        fun Spinner.initOrbitron(opts: List<String>) {
            // Preparamos la lista con un placeholder al inicio
            val listaCompleta = listOf("-- Elige una opción --") + opts
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_item_orbitron,  // layout custom con TextView que usa orbitron_bold
                R.id.tvSpinnerItem,
                listaCompleta
            )
            adapter.setDropDownViewResource(R.layout.spinner_item_orbitron)
            this.adapter = adapter
            // Evitar disparar onItemSelectedListener al inflar
            setSelection(0, false)
        }

        // Inicializar Spinners de Mejora1
        spinnerM1P1.initOrbitron(mejoraOpciones[0])
        spinnerM1P2.initOrbitron(mejoraOpciones[1])

        // Listener común para habilitar botón "Siguiente" en Mejora1
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
        spinnerM2P1.initOrbitron(mejoraOpciones[2])
        spinnerM2P2.initOrbitron(mejoraOpciones[3])

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

    /** Inicia Combate reiniciando vidas, borrando contraseñas usadas y determinando quién empieza */
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

    /** Lógica del turno del jugador: valida la contraseña, calcula daño, actualiza barras, controla fin de combate */
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
                    // Si queda más de la mitad de la vida → 60 puntos
                    COMBATE_MAX_SCORE
                }
                vidaJugador in 1..mitadVida -> {
                    // Si queda de 1 hasta la mitad → 30 puntos
                    COMBATE_MAX_SCORE / 2
                }
                else -> {
                    // Si vidaJugador <= 0 → 0 puntos
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
        barraVidaJugador.setProgressCompat(vidaJugador.coerceAtLeast(0), true)
        textoNarracion.text = getString(R.string.mensaje_enemigo_ataca, DANIO_SECUAZ)

        if (vidaJugador <= 0) {
            // Si pierdes, puntuación de combate = 0
            puntuacionCombate = 0
            handleLevelCompletion()
            return
        }
        turnoJugador = true
        btnAtacar.isEnabled = true
    }

    /** Calcula el daño de 'pw' según longitud, mayúsculas, dígitos y símbolos. Máximo 30 pts. */
    private fun calcularDano(pw: String): Int {
        var s = 0
        if (pw.length >= 12) s += 10
        if (pw.any { it.isUpperCase() }) s += 5
        if (pw.any { it.isDigit() }) s += 5
        if (pw.any { "!@#$%^&*".contains(it) }) s += 10
        return s.coerceAtMost(30)
    }

    /** Cuando el combate termina, calculamos la puntuación final y mostramos pantalla de recompensa */
    private fun handleLevelCompletion() {
        // Sumar puntuaciones parciales
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        // Mostrar Pantalla 7: Recompensa/Derrota
        mostrarPantalla(Paso.RECOMP)
        binding.tvPuntuacionFinal.text = getString(R.string.puntuacion_total, finalScore)

        // Animación flotante para imgConfeti
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)

        when {
            finalScore < 50 -> {
                // Ha perdido → Menor de 50
                binding.tvRecompensaTitulo.text = getString(R.string.derrota_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_mal)
                binding.imgConfeti.setImageResource(R.drawable.byte_triste)
                binding.btnRecompContinuar.text = getString(R.string.btn_reintentar)
            }
            finalScore in 50..70 -> {
                // Entre 50 y 70
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_bien)
                binding.imgConfeti.setImageResource(R.drawable.byte_frame2)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore in 71..99 -> {
                // Entre 71 y 99
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_muy_bien)
                binding.imgConfeti.setImageResource(R.drawable.byte_frame3)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore >= 100 -> {
                // 100 puntos → ¡perfecto!
                binding.tvRecompensaTitulo.text = getString(R.string.victoria_general)
                binding.tvRecompensaSubtitulo.text = getString(R.string.resultado_perfecto)
                binding.imgConfeti.setImageResource(R.drawable.byte_frame1)
                binding.btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
        }

        // Iniciar animación flotante en imgConfeti
        binding.imgConfeti.startAnimation(floatAnim)
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

        // Reiniciar Mejora (spinners volverán a posición 0 cuando se muestre la pantalla de Mejora)
        // Reiniciar Combate
        contrasenasUsadas.clear()

        // Volver a Pantalla 1
        mostrarPantalla(Paso.ATAQUE)
    }

    /**
     * Muestra solamente la pantalla correspondiente a 'paso' ocultando el resto.
     * Además, arranca o pausa el video en función de si ATAQUE está visible.
     * También gestiona la visibilidad de los botones de info.
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

        // 2) Ocultar TODOS los botones de info
        btnInfoQuiz.isVisible = false
        btnInfoMejora1.isVisible = false
        btnInfoMejora2.isVisible = false
        btnInfoCombate.isVisible = false

        // 3) Si mostramos ATAQUE, arrancar video; si no, pausarlo.
        if (paso == Paso.ATAQUE) {
            videoViper.start()
            scrollPantallaAtaque.isVisible = true
        } else {
            videoViper.pause()
            when (paso) {
                Paso.BYTE -> scrollPantallaByte.isVisible = true
                Paso.CONTEXTO -> scrollPantallaContexto.isVisible = true
                Paso.QUIZ -> {
                    scrollPantallaQuiz.isVisible = true
                    btnInfoQuiz.isVisible = true
                }
                Paso.MEJ1 -> {
                    scrollPantallaMejora1.isVisible = true
                    progressMejora1.max = 100
                    progressMejora1.setProgressCompat(50, true)
                    btnInfoMejora1.isVisible = true
                }
                Paso.MEJ2 -> {
                    scrollPantallaMejora2.isVisible = true
                    progressMejora2.max = 100
                    progressMejora2.setProgressCompat(100, true)
                    btnInfoMejora2.isVisible = true
                }
                Paso.COMBATE -> {
                    scrollPantallaCombate.isVisible = true
                    btnInfoCombate.isVisible = true
                }
                Paso.RECOMP -> scrollPantallaRecomp.isVisible = true
                else -> {}
            }
        }
    }

    // -----------------------------------------------------------------------
    // Funciones que muestran un AlertDialog con la información correspondiente
    // -----------------------------------------------------------------------

    private fun mostrarDialogoInfoQuiz() {
        val mensaje = """
            Aquí debes seleccionar la contraseña más segura de las opciones. 
            Cada pregunta vale 5 puntos y el Quiz consta de 4 preguntas. 
            Elige con cuidado: pulsa tu opción para habilitar “Siguiente”.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("¿Cómo funciona el Quiz?")
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoInfoMejora1() {
        val mensaje = """
            En esta sección (Mejora - Bloque 1) verás dos contraseñas escritas arriba de cada Spinner. 
            Elige la versión más segura de cada contraseña. 
            Cada respuesta correcta suma 5 puntos. 
            Cuando selecciones ambas opciones, el botón “Siguiente” se habilitará.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Cómo funciona Mejora (Bloque 1)")
            .setMessage(mensaje)
            .setPositiveButton("¡Vale!") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoInfoMejora2() {
        val mensaje = """
            Ahora estás en la parte Mejora (Bloque 2). 
            Nuevamente, elige la opción más segura de cada contraseña mostrada arriba. 
            Cada respuesta correcta suma 5 puntos adicionales. 
            Al completar ambas, pulsa “Siguiente” para pasar al combate.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Cómo funciona Mejora (Bloque 2)")
            .setMessage(mensaje)
            .setPositiveButton("Entendido") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoInfoCombate() {
        val mensaje = """
            Bienvenido al Combate Final. 
            En cada turno, escribe una contraseña en el campo de texto y pulsa “Atacar”. 
            Si la contraseña es muy segura, harás más daño al enemigo. 
            El enemigo te devolverá el golpe con 10 puntos de daño fijo por turno. 
            Tu puntuación de combate depende de cuánta vida te quede al final:
            • Si matas al enemigo con más de la mitad de tu vida → 60 puntos. 
            • Si matas al enemigo con entre 1 y la mitad de tu vida → 30 puntos.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Cómo funciona Combate")
            .setMessage(mensaje)
            .setPositiveButton("¡Vamos!") { dialog, _ -> dialog.dismiss() }
            .show()
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
