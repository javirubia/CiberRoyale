package com.ldm.ciberroyale.niveles

import android.content.Context
import android.content.res.ColorStateList
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

    private companion object {
        const val VIDA_INICIAL = 100
        const val DANIO_SECUAZ = 10
        const val PUNTOS_PREGUNTA = 5
        const val PUNTOS_MEJORA = 5
        const val MAX_SCORE_COMBATE = 60
    }

    private var vidaJugador = VIDA_INICIAL
    private var vidaEnemigo = VIDA_INICIAL
    private var turnoJugador = true

    private var puntuacionQuiz = 0
    private var puntuacionMejora = 0
    private var puntuacionCombate = 0

    private val preguntas = listOf(
        QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("123456", "GatoRojo$42", "abcd", "password"), 1),
        QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("maria2004", "M@ri4_2004", "Maria04", "maría"), 1),
        QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("pepepepe", "P3pe\$Rock!", "PePePePe", "pepe1234"), 1),
        QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("contraseña", "C0ntr@s3ñ4!", "1234", "contra"), 1)
    ).shuffled().toMutableList()
    private var idxPregunta = 0

    private val mejoraOpciones = listOf(
        listOf("jnito2009", "Ju@nito_2009!", "miJuan*2023"),
        listOf("c0ntraseña", "C0ntr@seÑa2024", "123456"),
        listOf("123pepe", "Pep3_321!", "peperisa"),
        listOf("verano", "Pr1m4v3r4!", "Primavera2024")
    )
    private val mejoraCorrectas = listOf(
        "Ju@nito_2009!", "C0ntr@seÑa2024", "Pep3_321!", "Pr1m4v3r4!"
    )
    private val contrasenasUsadas = mutableSetOf<String>()

    enum class Paso { ATAQUE, BYTE, CONTEXTO, QUIZ, MEJ1, MEJ2, COMBATE, RECOMP }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNivel1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVideo()
        setupNavigation()
        setupQuiz()
        setupMejora()
        setupCombate()

        binding.imgByteContexto.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
        listOf(binding.imgEnemigo, binding.imgJugador).forEach {
            it.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
        }

        binding.btnRecompContinuar.setOnClickListener { onFinalizarNivel() }
        setInfoButtons()
        mostrarPantalla(Paso.ATAQUE)
    }

    override fun onResume() { super.onResume(); binding.videoViper.start() }
    override fun onPause() { super.onPause(); binding.videoViper.pause() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private fun setupVideo() = binding.run {
        val uri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.viper_attack}")
        videoViper.apply {
            setVideoURI(uri)
            setOnPreparedListener { it.isLooping = true; it.setVolume(0f, 0f); it.start() }
            setOnCompletionListener { it.start() }
        }
    }

    private fun setupNavigation() = binding.run {
        btnSiguienteAtaque.setOnClickListener { mostrarPantalla(Paso.BYTE) }
        btnUnirse.setOnClickListener { mostrarPantalla(Paso.CONTEXTO) }
        btnRechazar.setOnClickListener { Toast.makeText(context, getString(R.string.toast_necesario), Toast.LENGTH_SHORT).show() }
        btnContextoListo.setOnClickListener {
            puntuacionQuiz = 0
            idxPregunta = 0
            preguntas.shuffle()
            cargarPregunta()
            mostrarPantalla(Paso.QUIZ)
        }
    }

    private fun setupQuiz() = binding.run {
        rgOpciones.setOnCheckedChangeListener { _, _ -> btnSiguiente.isEnabled = true }
        btnSiguiente.setOnClickListener {
            (rgOpciones.findViewById<MaterialRadioButton>(rgOpciones.checkedRadioButtonId))?.let {
                if (it.tag as Boolean) puntuacionQuiz += PUNTOS_PREGUNTA
            }
            idxPregunta++
            if (idxPregunta < preguntas.size) cargarPregunta() else showQuizResult()
            btnSiguiente.isEnabled = false
        }
    }

    private fun cargarPregunta() = binding.run {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto
        rgOpciones.removeAllViews()
        q.opciones.shuffled().forEach { opt ->
            MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = opt
                tag = (opt == q.opciones[q.opcionCorrecta])
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_700)))
                typeface = ResourcesCompat.getFont(context, R.font.orbitron_bold)
                layoutParams = RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 8, 0, 8)
                }
            }.also { rgOpciones.addView(it) }
        }
        rgOpciones.clearCheck()
        btnSiguiente.isEnabled = false
        tvQuizTitle.text = getString(R.string.quiz_titulo, idxPregunta + 1, preguntas.size)
        progressQuiz.setProgressCompat((idxPregunta + 1) * 100 / preguntas.size, true)
    }

    private fun showQuizResult() = binding.run {
        Toast.makeText(requireContext(), getString(R.string.resultado_bien, puntuacionQuiz, preguntas.size * PUNTOS_PREGUNTA), Toast.LENGTH_SHORT).show()
        puntuacionMejora = 0
        mostrarPantalla(Paso.MEJ1)
    }

    private fun setupMejora() = binding.run {
        listOf(btnMejora1Next, btnMejora2Next).forEach { it.isEnabled = false }
        fun Spinner.init(opts: List<String>) {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_orbitron, R.id.tvSpinnerItem, listOf("-- Elige opción --") + opts).apply {
                setDropDownViewResource(R.layout.spinner_item_orbitron)
            }
            setSelection(0, false)
        }
        spinnerM1P1.init(mejoraOpciones[0]); spinnerM1P2.init(mejoraOpciones[1])
        val listener1 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                btnMejora1Next.isEnabled = spinnerM1P1.selectedItemPosition > 0 && spinnerM1P2.selectedItemPosition > 0
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerM1P1.onItemSelectedListener = listener1
        spinnerM1P2.onItemSelectedListener = listener1
        btnMejora1Next.setOnClickListener { checkMejora(0, 1); mostrarPantalla(Paso.MEJ2) }
        spinnerM2P1.init(mejoraOpciones[2]); spinnerM2P2.init(mejoraOpciones[3])
        val listener2 = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                btnMejora2Next.isEnabled = spinnerM2P1.selectedItemPosition > 0 && spinnerM2P2.selectedItemPosition > 0
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerM2P1.onItemSelectedListener = listener2
        spinnerM2P2.onItemSelectedListener = listener2
        btnMejora2Next.setOnClickListener { checkMejora(2, 3); iniciarCombate(); mostrarPantalla(Paso.COMBATE) }
    }

    private fun checkMejora(i1: Int, i2: Int) {
        if (binding.spinnerM1P1.selectedItem == mejoraCorrectas[i1]) puntuacionMejora += PUNTOS_MEJORA
        if (binding.spinnerM1P2.selectedItem == mejoraCorrectas[i2]) puntuacionMejora += PUNTOS_MEJORA
    }

    private fun setupCombate() = binding.btnAtacar.setOnClickListener { playerTurn() }

    private fun iniciarCombate() = binding.run {
        vidaJugador = VIDA_INICIAL
        vidaEnemigo = VIDA_INICIAL
        contrasenasUsadas.clear()
        barraVidaJugador.setProgressCompat(vidaJugador, true)
        barraVidaEnemigo.setProgressCompat(vidaEnemigo, true)
        turnoJugador = listOf(true, false).shuffled().first()
        btnAtacar.isEnabled = turnoJugador
        textoNarracion.text = if (turnoJugador) getString(R.string.empiezas) else getString(R.string.enemigo_primero)
        if (!turnoJugador) lifecycleScope.launch { delay(1500); enemyTurn() }
    }

    private fun playerTurn() {
        if (!turnoJugador) return
        binding.run {
            val pw = inputContrasena.text.toString().trim()
            if (pw.isBlank()) {
                textoNarracion.text = getString(R.string.hint_contrasena)
                return@run
            }
            if (!contrasenasUsadas.add(pw)) {
                textoNarracion.text = getString(R.string.mensaje_pw_repetida)
                return@run
            }
            val dmg = calcularDano(pw)
            if (dmg > 0) {
                vidaEnemigo -= dmg
                barraVidaEnemigo.setProgressCompat(vidaEnemigo.coerceAtLeast(0), true)
                textoNarracion.text = getString(R.string.mensaje_damage, dmg)
            } else {
                textoNarracion.text = getString(R.string.mensaje_demasiado_debil)
            }
            inputContrasena.text?.clear()
            (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(inputContrasena.windowToken, 0)
            if (vidaEnemigo <= 0) {
                finalizeCombate()
                return@run
            }
            turnoJugador = false
            btnAtacar.isEnabled = false
            lifecycleScope.launch {
                delay(1500)
                enemyTurn()
            }
        }
    }

    private fun calcularDano(pw: String): Int {
        var score = 0
        if (pw.length >= 12) score += 10
        if (pw.any { it.isUpperCase() }) score += 5
        if (pw.any { it.isDigit() }) score += 5
        if (pw.any { "!@#$%^&*".contains(it) }) score += 10
        return score.coerceAtMost(30)
    }

    private fun enemyTurn() = binding.run {
        vidaJugador -= DANIO_SECUAZ
        barraVidaJugador.setProgressCompat(vidaJugador.coerceAtLeast(0), true)
        textoNarracion.text = getString(R.string.mensaje_enemigo_ataca, DANIO_SECUAZ)
        if (vidaJugador <= 0) {
            finalizeCombate()
            return@run
        }
        turnoJugador = true
        btnAtacar.isEnabled = true
    }

    private fun finalizeCombate() {
        puntuacionCombate = when {
            vidaJugador > VIDA_INICIAL / 2 -> MAX_SCORE_COMBATE
            vidaJugador > 0 -> MAX_SCORE_COMBATE / 2
            else -> 0
        }
        handleLevelCompletion()
    }

    private fun handleLevelCompletion() = binding.run {
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        mostrarPantalla(Paso.RECOMP)
        tvPuntuacionFinal.text = getString(R.string.puntuacion_total, finalScore)
        val icon = when {
            finalScore < 50 -> R.drawable.byte_triste
            finalScore < 71 -> R.drawable.byte_frame2
            finalScore < 100 -> R.drawable.byte_frame3
            else -> R.drawable.byte_frame1
        }
        imgConfeti.setImageResource(icon)
        imgConfeti.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
        binding.btnRecompContinuar.text = if (finalScore < 50) getString(R.string.btn_reintentar) else getString(R.string.btn_continuar_final)
    }

    private fun onFinalizarNivel() {
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        if (finalScore < 50) resetLevel() else findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
    }

    private fun resetLevel() {
        puntuacionQuiz = 0
        puntuacionMejora = 0
        puntuacionCombate = 0
        idxPregunta = 0
        preguntas.shuffle()
        contrasenasUsadas.clear()
        mostrarPantalla(Paso.ATAQUE)
    }

    private fun mostrarPantalla(paso: Paso) = binding.run {
        // Ocultar todas las pantallas
        listOf(scrollPantallaAtaque, scrollPantallaByte, scrollPantallaContexto,
            scrollPantallaQuiz, scrollPantallaMejora1, scrollPantallaMejora2,
            scrollPantallaCombate, scrollPantallaRecomp).forEach { it.isVisible = false }

        // Mostrar solo la pantalla correspondiente
        when (paso) {
            Paso.ATAQUE -> { scrollPantallaAtaque.isVisible = true; binding.videoViper.start() }
            Paso.BYTE -> scrollPantallaByte.isVisible = true
            Paso.CONTEXTO -> scrollPantallaContexto.isVisible = true
            Paso.QUIZ -> scrollPantallaQuiz.isVisible = true
            Paso.MEJ1 -> scrollPantallaMejora1.isVisible = true
            Paso.MEJ2 -> scrollPantallaMejora2.isVisible = true
            Paso.COMBATE -> scrollPantallaCombate.isVisible = true
            Paso.RECOMP -> scrollPantallaRecomp.isVisible = true
        }
        if (paso != Paso.ATAQUE) binding.videoViper.pause()

        // Mostrar solo el botón de info correspondiente
        btnInfoQuiz.isVisible = paso == Paso.QUIZ
        btnInfoMejora1.isVisible = paso == Paso.MEJ1
        btnInfoMejora2.isVisible = paso == Paso.MEJ2
        btnInfoCombate.isVisible = paso == Paso.COMBATE
    }

    private fun setInfoButtons() = binding.run {
        btnInfoQuiz.setOnClickListener { showInfoDialog("quiz") }
        btnInfoMejora1.setOnClickListener { showInfoDialog("mejora1") }
        btnInfoMejora2.setOnClickListener { showInfoDialog("mejora2") }
        btnInfoCombate.setOnClickListener { showInfoDialog("combate") }
    }

    private fun showInfoDialog(type: String) {
        val (title, msg) = when (type) {
            "quiz" -> "¿Cómo funciona el Quiz?" to getString(R.string.info_quiz)
            "mejora1" -> "Cómo funciona Mejora (Bloque 1)" to getString(R.string.info_mejora1)
            "mejora2" -> "Cómo funciona Mejora (Bloque 2)" to getString(R.string.info_mejora2)
            else -> "Cómo funciona Combate" to getString(R.string.info_combate)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
            .show()
    }

    data class QuizQuestion(
        val texto: String,
        val opciones: List<String>,
        val opcionCorrecta: Int
    )
}
