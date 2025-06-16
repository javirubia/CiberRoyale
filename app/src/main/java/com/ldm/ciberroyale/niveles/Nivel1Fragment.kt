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
import com.ldm.ciberroyale.ProgresoManager
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel1Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Nivel1Fragment : Fragment() {

    private var _binding: FragmentNivel1Binding? = null
    private val binding get() = _binding!!

    //region Estado del Nivel
    private var vidaJugador = VIDA_INICIAL
    private var vidaEnemigo = VIDA_INICIAL
    private var turnoJugador = true

    private var puntuacionQuiz = 0
    private var puntuacionMejora = 0
    private var puntuacionCombate = 0

    private val preguntas = PREGUNTAS_QUIZ.shuffled().toMutableList()
    private var idxPregunta = 0
    private val contrasenasUsadas = mutableSetOf<String>()
    //endregion

    enum class Paso { ATAQUE, BYTE, CONTEXTO, QUIZ, MEJ1, MEJ2, COMBATE, RECOMP }

    //region Ciclo de Vida
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
        setupAnimations()
        setInfoButtons()

        mostrarPantalla(Paso.ATAQUE)
    }

    override fun onResume() {
        super.onResume()
        binding.videoViper.start()
    }

    override fun onPause() {
        super.onPause()
        binding.videoViper.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //endregion

    //region Configuraciones Iniciales (Setup)
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
        btnRechazar.setOnClickListener {
            Toast.makeText(context, getString(R.string.nivel1_toast_necesario), Toast.LENGTH_SHORT).show()
        }
        btnContextoListo.setOnClickListener {
            resetLevelState() // Reinicia las puntuaciones antes de empezar el quiz
            cargarPregunta()
            mostrarPantalla(Paso.QUIZ)
        }
        // El botón de la pantalla de recompensa llama a onFinalizarNivel
        btnRecompContinuar.setOnClickListener { onFinalizarNivel() }
    }

    private fun setupAnimations() {
        val floatAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        binding.imgByteContexto.startAnimation(floatAnimation)
        binding.imgEnemigo.startAnimation(floatAnimation)
        binding.imgJugador.startAnimation(floatAnimation)
    }
    //endregion

    //region Lógica del Quiz
    private fun setupQuiz() = binding.run {
        rgOpciones.setOnCheckedChangeListener { _, _ -> btnSiguiente.isEnabled = true }
        btnSiguiente.setOnClickListener {
            val radioButton = rgOpciones.findViewById<MaterialRadioButton>(rgOpciones.checkedRadioButtonId)
            if (radioButton != null && radioButton.tag as Boolean) {
                puntuacionQuiz += PUNTOS_PREGUNTA
            }

            idxPregunta++
            if (idxPregunta < preguntas.size) {
                cargarPregunta()
            } else {
                showQuizResult()
            }
        }
    }

    private fun cargarPregunta() = binding.run {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto
        rgOpciones.removeAllViews()
        q.opciones.shuffled().forEach { opcion ->
            val radioButton = MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = opcion
                tag = (opcion == q.opciones[q.opcionCorrecta])
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_700))
                typeface = ResourcesCompat.getFont(context, R.font.orbitron_bold)
                layoutParams = RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
            }
            rgOpciones.addView(radioButton)
        }
        rgOpciones.clearCheck()
        btnSiguiente.isEnabled = false
        tvQuizTitle.text = getString(R.string.nivel1_quiz_titulo, idxPregunta + 1, preguntas.size)
        progressQuiz.setProgressCompat((idxPregunta + 1) * 100 / preguntas.size, true)
    }

    private fun showQuizResult() {
        val totalPuntos = preguntas.size * PUNTOS_PREGUNTA
        Toast.makeText(requireContext(), getString(R.string.nivel1_quiz_resultado_toast, puntuacionQuiz, totalPuntos), Toast.LENGTH_SHORT).show()
        mostrarPantalla(Paso.MEJ1)
    }
    //endregion

    //region Lógica de Mejora de Contraseñas
    private fun setupMejora() = binding.run {
        fun Spinner.init(options: List<String>, onItemSelected: () -> Unit) {
            val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_orbitron, R.id.tvSpinnerItem, listOf(getString(R.string.nivel1_mejora_spinner_prompt)) + options).apply {
                setDropDownViewResource(R.layout.spinner_item_orbitron)
            }
            adapter = spinnerAdapter
            setSelection(0, false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { onItemSelected() }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }

        spinnerM1P1.init(MEJORA_OPCIONES[0]) { btnMejora1Next.isEnabled = spinnerM1P1.selectedItemPosition > 0 && spinnerM1P2.selectedItemPosition > 0 }
        spinnerM1P2.init(MEJORA_OPCIONES[1]) { btnMejora1Next.isEnabled = spinnerM1P1.selectedItemPosition > 0 && spinnerM1P2.selectedItemPosition > 0 }
        spinnerM2P1.init(MEJORA_OPCIONES[2]) { btnMejora2Next.isEnabled = spinnerM2P1.selectedItemPosition > 0 && spinnerM2P2.selectedItemPosition > 0 }
        spinnerM2P2.init(MEJORA_OPCIONES[3]) { btnMejora2Next.isEnabled = spinnerM2P1.selectedItemPosition > 0 && spinnerM2P2.selectedItemPosition > 0 }

        btnMejora1Next.setOnClickListener { checkMejora(spinnerM1P1, spinnerM1P2, 0, 1); mostrarPantalla(Paso.MEJ2) }
        btnMejora2Next.setOnClickListener { checkMejora(spinnerM2P1, spinnerM2P2, 2, 3); iniciarCombate(); mostrarPantalla(Paso.COMBATE) }
    }

    private fun checkMejora(spinner1: Spinner, spinner2: Spinner, correctIndex1: Int, correctIndex2: Int) {
        if (spinner1.selectedItem as String == MEJORA_CORRECTAS[correctIndex1]) puntuacionMejora += PUNTOS_MEJORA
        if (spinner2.selectedItem as String == MEJORA_CORRECTAS[correctIndex2]) puntuacionMejora += PUNTOS_MEJORA
    }
    //endregion

    //region Lógica del Combate Final
    private fun setupCombate() = binding.btnAtacar.setOnClickListener { playerTurn() }

    private fun iniciarCombate() = binding.run {
        vidaJugador = VIDA_INICIAL
        vidaEnemigo = VIDA_INICIAL
        contrasenasUsadas.clear()
        barraVidaJugador.setProgressCompat(vidaJugador, true)
        barraVidaEnemigo.setProgressCompat(vidaEnemigo, true)

        turnoJugador = listOf(true, false).random()
        btnAtacar.isEnabled = turnoJugador

        if (turnoJugador) {
            textoNarracion.text = getString(R.string.nivel1_combate_narracion_turno_jugador)
        } else {
            textoNarracion.text = getString(R.string.nivel1_combate_narracion_turno_enemigo)
            lifecycleScope.launch { delay(1500); enemyTurn() }
        }
    }

    private fun playerTurn() {
        if (!turnoJugador) return
        binding.run {
            val password = inputContrasena.text.toString().trim()
            if (password.isBlank()) {
                textoNarracion.text = getString(R.string.nivel1_combate_hint)
                return
            }
            if (!contrasenasUsadas.add(password)) {
                textoNarracion.text = getString(R.string.nivel1_combate_narracion_pw_repetida)
                return
            }

            val damage = calcularDano(password)
            if (damage > 0) {
                vidaEnemigo -= damage
                barraVidaEnemigo.setProgressCompat(vidaEnemigo.coerceAtLeast(0), true)
                textoNarracion.text = getString(R.string.nivel1_combate_narracion_damage_hecho, damage)
            } else {
                textoNarracion.text = getString(R.string.nivel1_combate_narracion_damage_fallido)
            }
            inputContrasena.text?.clear()
            hideKeyboard()

            if (vidaEnemigo <= 0) {
                finalizeCombate()
                return
            }
            turnoJugador = false
            btnAtacar.isEnabled = false
            lifecycleScope.launch { delay(1500); enemyTurn() }
        }
    }

    private fun enemyTurn() = binding.run {
        vidaJugador -= DANIO_ENEMIGO
        barraVidaJugador.setProgressCompat(vidaJugador.coerceAtLeast(0), true)
        textoNarracion.text = getString(R.string.nivel1_combate_narracion_damage_recibido, DANIO_ENEMIGO)
        if (vidaJugador <= 0) {
            finalizeCombate()
            return
        }
        turnoJugador = true
        btnAtacar.isEnabled = true
    }

    private fun calcularDano(pw: String): Int {
        var score = 0
        if (pw.length >= 12) score += PUNTOS_DANO_LONGITUD
        if (pw.any { it.isUpperCase() }) score += PUNTOS_DANO_MAYUS
        if (pw.any { it.isDigit() }) score += PUNTOS_DANO_NUM
        if (pw.any { it in SIMBOLOS_DANO }) score += PUNTOS_DANO_SIMBOLO
        return score.coerceAtMost(MAX_DANO_JUGADOR)
    }
    //endregion

    //region Finalización y Flujo del Nivel
    private fun finalizeCombate() {
        puntuacionCombate = when {
            vidaJugador > VIDA_INICIAL / 2 -> PUNTOS_COMBATE_MAX
            vidaJugador > 0 -> PUNTOS_COMBATE_MAX / 2
            else -> 0
        }
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        if (finalScore >= SCORE_MIN_APROBADO) {
            ProgresoManager.desbloquearSiguienteNivel(1)
            checkLevel1Achievements(finalScore, vidaJugador)
        }
        handleLevelCompletion(finalScore)
    }

    private fun handleLevelCompletion(finalScore: Int) = binding.run {
        mostrarPantalla(Paso.RECOMP)
        tvPuntuacionFinal.text = getString(R.string.nivel1_recompensa_puntuacion, finalScore)

        val (icon, title, subtitle) = when {
            finalScore < SCORE_MIN_APROBADO -> Triple(R.drawable.byte_triste, "Derrota", "ViperWare ha ganado...")
            finalScore < SCORE_NOTABLE -> Triple(R.drawable.byte_frame2, "¡Victoria!", "¡Buen trabajo!")
            finalScore < SCORE_EXCELENTE -> Triple(R.drawable.byte_frame3, "¡VICTORIA ÉPICA!", "¡Gran puntuación!")
            else -> Triple(R.drawable.byte_frame1, "¡VICTORIA PERFECTA!", "¡Has dominado el nivel!")
        }

        imgConfeti.setImageResource(icon)
        imgConfeti.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
        tvRecompensaTitulo.text = title
        tvRecompensaSubtitulo.text = subtitle
        btnRecompContinuar.text = if (finalScore < SCORE_MIN_APROBADO) {
            getString(R.string.common_button_reintentar)
        } else {
            getString(R.string.common_button_finalizar_nivel)
        }
    }

    private fun onFinalizarNivel() {
        val finalScore = puntuacionQuiz + puntuacionMejora + puntuacionCombate
        if (finalScore >= SCORE_MIN_APROBADO) {
            findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
        } else {
            resetLevel()
        }
    }

    private fun checkLevel1Achievements(score: Int, vida: Int) {
        ProgresoManager.unlockAchievement("NIVEL1_COMPLETADO")
        if (score > 90) {
            ProgresoManager.unlockAchievement("NIVEL1_EXCELENTE")
        }
        if (vida == VIDA_INICIAL) {
            ProgresoManager.unlockAchievement("NIVEL1_INTACTO")
        }
    }

    private fun resetLevel() {
        resetLevelState()
        mostrarPantalla(Paso.ATAQUE)
    }

    private fun resetLevelState(){
        vidaJugador = VIDA_INICIAL
        vidaEnemigo = VIDA_INICIAL
        puntuacionQuiz = 0
        puntuacionMejora = 0
        puntuacionCombate = 0
        idxPregunta = 0
        preguntas.shuffle()
        contrasenasUsadas.clear()
    }

    private fun mostrarPantalla(paso: Paso) = binding.run {
        val allScreens = listOf(
            scrollPantallaAtaque, scrollPantallaByte, scrollPantallaContexto,
            scrollPantallaQuiz, scrollPantallaMejora1, scrollPantallaMejora2,
            scrollPantallaCombate, scrollPantallaRecomp
        )
        allScreens.forEach { it.isVisible = false }

        when (paso) {
            Paso.ATAQUE -> { scrollPantallaAtaque.isVisible = true; videoViper.start() }
            Paso.BYTE -> scrollPantallaByte.isVisible = true
            Paso.CONTEXTO -> scrollPantallaContexto.isVisible = true
            Paso.QUIZ -> scrollPantallaQuiz.isVisible = true
            Paso.MEJ1 -> scrollPantallaMejora1.isVisible = true
            Paso.MEJ2 -> scrollPantallaMejora2.isVisible = true
            Paso.COMBATE -> scrollPantallaCombate.isVisible = true
            Paso.RECOMP -> scrollPantallaRecomp.isVisible = true
        }
        if (paso != Paso.ATAQUE) videoViper.pause()

        btnInfoQuiz.isVisible = paso == Paso.QUIZ
        btnInfoMejora1.isVisible = paso == Paso.MEJ1
        btnInfoMejora2.isVisible = paso == Paso.MEJ2
        btnInfoCombate.isVisible = paso == Paso.COMBATE
    }

    private fun setInfoButtons() = binding.run {
        btnInfoQuiz.setOnClickListener { showInfoDialog(getString(R.string.desc_info_nivel1_quiz), getString(R.string.dialog_info_nivel1_quiz)) }
        btnInfoMejora1.setOnClickListener { showInfoDialog(getString(R.string.desc_info_nivel1_mejora1), getString(R.string.dialog_info_nivel1_mejora1)) }
        btnInfoMejora2.setOnClickListener { showInfoDialog(getString(R.string.desc_info_nivel1_mejora2), getString(R.string.dialog_info_nivel1_mejora2)) }
        btnInfoCombate.setOnClickListener { showInfoDialog(getString(R.string.desc_info_nivel1_combate), getString(R.string.dialog_info_nivel1_combate)) }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.common_dialog_entendido) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    //endregion

    //region Datos y Constantes del Nivel
    companion object {
        private const val VIDA_INICIAL = 100
        private const val DANIO_ENEMIGO = 10
        private const val PUNTOS_PREGUNTA = 5
        private const val PUNTOS_MEJORA = 5
        private const val PUNTOS_COMBATE_MAX = 60
        private const val MAX_DANO_JUGADOR = 30
        private const val PUNTOS_DANO_LONGITUD = 10
        private const val PUNTOS_DANO_MAYUS = 5
        private const val PUNTOS_DANO_NUM = 5
        private const val PUNTOS_DANO_SIMBOLO = 10
        private const val SIMBOLOS_DANO = "!@#$%^&*"
        private const val SCORE_MIN_APROBADO = 50
        private const val SCORE_NOTABLE = 71
        private const val SCORE_EXCELENTE = 100

        private data class QuizQuestion(val texto: String, val opciones: List<String>, val opcionCorrecta: Int)

        private val PREGUNTAS_QUIZ = listOf(
            QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("123456", "GatoRojo$42", "abcd", "password"), 1),
            QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("maria2004", "M@ri4_2004", "Maria04", "maría"), 1),
            QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("pepepepe", "P3pe\$Rock!", "PePePePe", "pepe1234"), 1),
            QuizQuestion("¿Cuál de estas contraseñas es más segura?", listOf("contraseña", "C0ntr@s3ñ4!", "1234", "contra"), 1)
        )

        private val MEJORA_OPCIONES = listOf(
            listOf("jnito2009", "Ju@nito_2009!", "miJuan*2023"),
            listOf("c0ntraseña", "C0ntr@seÑa2024", "123456"),
            listOf("123pepe", "Pep3_321!", "peperisa"),
            listOf("verano", "Pr1m4v3r4!", "Primavera2024")
        )
        private val MEJORA_CORRECTAS = listOf(
            "Ju@nito_2009!", "C0ntr@seÑa2024", "Pep3_321!", "Pr1m4v3r4!"
        )
    }
    //endregion
}