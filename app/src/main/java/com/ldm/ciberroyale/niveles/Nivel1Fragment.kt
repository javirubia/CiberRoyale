package com.ldm.ciberroyale.niveles

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
        private const val VIDA_INICIAL_ENEMIGO = 80
        private const val DANIO_SECUAZ = 10
    }

    private var vidaJugador = VIDA_INICIAL_JUGADOR
    private var vidaEnemigo = VIDA_INICIAL_ENEMIGO
    private var turnoJugador = true
    private val contrasenasUsadas = mutableSetOf<String>()

    // Quiz
    private val preguntasOriginal = listOf(
        QuizQuestion("¿Cuál de estas contraseñas es más segura?",
            listOf("123456", "GatoRojo$42", "abcd", "password"), 1),
        QuizQuestion("¿Cuál de estas es más segura?",
            listOf("maria2004", "M@ri4_2004", "Maria04", "maría"), 1),
        QuizQuestion("¿Cuál de estas es más segura?",
            listOf("pepepepe", "P3pe\$Rock!", "PePePePe", "pepe1234"), 1),
        QuizQuestion("¿Cuál de estas contraseñas es más segura?",
            listOf("contraseña", "C0ntr@s3ñ4!", "1234", "contra"), 1)
    )
    private val preguntas = preguntasOriginal.shuffled().toMutableList()
    private var idxPregunta = 0
    private var puntuacionQuiz = 0

    // Mejora
    private val mejoraOpciones = listOf(
        listOf("jnito2009", "Ju@nito_2009!", "miJuan*2023"),
        listOf("c0ntraseña", "C0ntr@seÑa2024", "123456"),
        listOf("123pepe", "Pep3_321!", "peperisa"),
        listOf("verano", "Pr1m4v3r4!", "Primavera2024")
    )
    private val mejoraCorrectas = listOf("Ju@nito_2009!", "C0ntr@seÑa2024", "Pep3_321!", "Pr1m4v3r4!")
    private var puntuacionMejora = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        binding.imgByteContexto.startAnimation(floatAnim)
    }

    override fun onResume() {
        super.onResume()
        binding.videoViper.start()    // reanuda si estaba pausado
    }

    override fun onPause() {
        super.onPause()
        binding.videoViper.pause()    // pausa para liberar recursos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupVideo() = with(binding) {
        // URI al recurso raw/viper_attack.mp4
        val videoUri = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.viper_attack}")
        videoViper.setVideoURI(videoUri)

        videoViper.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.start()
        }
        // En caso de que OnCompletion se llame
        videoViper.setOnCompletionListener { it.start() }
    }

    private fun setupNavigation() = with(binding) {
        btnSiguienteAtaque.setOnClickListener { mostrarPantalla(Paso.BYTE) }
        btnUnirse.setOnClickListener { mostrarPantalla(Paso.CONTEXTO) }
        btnContextoListo.setOnClickListener {
            puntuacionQuiz = 0; idxPregunta = 0
            cargarPregunta()
            mostrarPantalla(Paso.QUIZ)
        }
        btnRechazar.setOnClickListener {
            Toast.makeText(requireContext(), "¡El mundo te necesita!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupQuiz() = with(binding) {
        rgOpciones.setOnCheckedChangeListener { _, _ ->
            btnSiguiente.isEnabled = true
        }
        btnSiguiente.setOnClickListener {
            val seleccionado = rgOpciones.findViewById<android.widget.RadioButton>(
                rgOpciones.checkedRadioButtonId
            )
            if (seleccionado?.tag == true) puntuacionQuiz += preguntas[idxPregunta].puntos
            idxPregunta++
            if (idxPregunta < preguntas.size) cargarPregunta()
            else {
                Toast.makeText(requireContext(),
                    "Quiz: $puntuacionQuiz/${preguntas.size * 5} pts",
                    Toast.LENGTH_SHORT).show()
                mostrarPantalla(Paso.MEJ1)
            }
            btnSiguiente.isEnabled = false
        }
    }


    private fun cargarPregunta() = with(binding) {
        val q = preguntas[idxPregunta]
        tvPregunta.text = q.texto
        rgOpciones.removeAllViews()

        // ← Aquí sustituimos la creación de RadioButton...
        q.opciones.mapIndexed { i, texto ->
            MaterialRadioButton(requireContext()).apply {
                id = View.generateViewId()
                this.text = texto
                this.tag = (i == q.opcionCorrecta)
                // ----> Estilos Material:
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
        tvQuizTitle.text = "Pregunta ${idxPregunta+1} de ${preguntas.size}"
        val percent = (idxPregunta+1)*100 / preguntas.size
        progressQuiz.setProgressCompat(percent, true)
    }

    private fun setupMejora() = with(binding) {
        fun Spinner.init(opts: List<String>, onChange: () -> Unit) {
            adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_item, opts).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>,
                    view: View?, pos: Int, id: Long
                ) = onChange()
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
        }

        // Bloque 1
        spinnerM1P1.init(mejoraOpciones[0]) {
            btnMejora1Next.isEnabled =
                spinnerM1P1.selectedItemPosition > 0 &&
                        spinnerM1P2.selectedItemPosition > 0
        }
        spinnerM1P2.init(mejoraOpciones[1]) {
            btnMejora1Next.isEnabled =
                spinnerM1P1.selectedItemPosition > 0 &&
                        spinnerM1P2.selectedItemPosition > 0
        }
        btnMejora1Next.setOnClickListener {
            if (spinnerM1P1.selectedItem == mejoraCorrectas[0]) puntuacionMejora += 3
            if (spinnerM1P2.selectedItem == mejoraCorrectas[1]) puntuacionMejora += 3
            mostrarPantalla(Paso.MEJ2)
        }

        // Bloque 2
        spinnerM2P1.init(mejoraOpciones[2]) {
            btnMejora2Next.isEnabled =
                spinnerM2P1.selectedItemPosition > 0 &&
                        spinnerM2P2.selectedItemPosition > 0
        }
        spinnerM2P2.init(mejoraOpciones[3]) {
            btnMejora2Next.isEnabled =
                spinnerM2P1.selectedItemPosition > 0 &&
                        spinnerM2P2.selectedItemPosition > 0
        }
        btnMejora2Next.setOnClickListener {
            if (spinnerM2P1.selectedItem == mejoraCorrectas[2]) puntuacionMejora += 3
            if (spinnerM2P2.selectedItem == mejoraCorrectas[3]) puntuacionMejora += 3
            Toast.makeText(requireContext(),
                "Mejora: $puntuacionMejora/12 pts",
                Toast.LENGTH_SHORT).show()
            iniciarCombate()
            mostrarPantalla(Paso.COMBATE)
        }
    }



    private fun setupCombate() = with(binding) {
        btnAtacar.setOnClickListener { playerTurn() }
    }

    private fun iniciarCombate() = with(binding) {
        turnoJugador = (0..1).random() == 0
        textoNarracion.text = if (turnoJugador) "¡Empiezas tú!"
        else "CobraX ataca primero..."
        if (!turnoJugador) lifecycleScope.launch {
            delay(1500)
            enemyTurn()
        }
    }

    private fun playerTurn() {
        if (!turnoJugador) return
        val pw = binding.inputContrasena.text.toString()
        if (pw.isBlank()) {
            binding.textoNarracion.text = "Escribe una contraseña antes"
            return
        }
        if (!contrasenasUsadas.add(pw)) {
            binding.textoNarracion.text = "No repitas contraseñas"
            return
        }
        val damage = calcularDano(pw)
        vidaEnemigo -= damage
        binding.barraVidaEnemigo.progress = vidaEnemigo
        binding.textoNarracion.text = "Le haces $damage pts"
        if (vidaEnemigo <= 0){
            victory()
            return
        }
        turnoJugador = false
        lifecycleScope.launch {
            delay(1500)
            enemyTurn()
        }
    }

    private fun enemyTurn() = with(binding) {
        vidaJugador -= DANIO_SECUAZ
        barraVidaJugador.progress = vidaJugador
        textoNarracion.text = "CobraX inflige $DANIO_SECUAZ pts"
        if (vidaJugador <= 0) return defeat()
        turnoJugador = true
    }

    private fun victory() = with(binding) {
        textoNarracion.text = "🎉 ¡Has ganado!"
        btnAtacar.isEnabled = false
        lifecycleScope.launch {
            delay(2000)
            findNavController().navigate(R.id.action_nivel1Fragment_to_juegoFragment)
        }
    }

    private fun defeat() = with(binding) {
        textoNarracion.text = "😢 Derrotado. Intenta otra vez."
        btnAtacar.isEnabled = false
    }

    private fun calcularDano(pw: String): Int {
        var s = 0
        if (pw.length >= 12) s += 10
        if (pw.any { it.isUpperCase() }) s += 5
        if (pw.any { it.isDigit() }) s += 5
        if (pw.any { "!@#$%^&*".contains(it) }) s += 10
        return s.coerceAtMost(30)
    }

    private fun mostrarPantalla(paso: Paso) {
        listOf(
            binding.pantallaAtaque,
            binding.pantallaByte,
            binding.pantallaContexto,
            binding.pantallaQuiz,
            binding.pantallaMejora1,
            binding.pantallaMejora2,
            binding.pantallaCombate
        ).forEach { it.isVisible = false }
        when (paso) {
            Paso.ATAQUE -> binding.pantallaAtaque.isVisible = true
            Paso.BYTE    -> binding.pantallaByte.isVisible = true
            Paso.CONTEXTO-> {
                binding.pantallaContexto.isVisible = true
                val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
                binding.imgByteContexto.startAnimation(floatAnim)
            }

            Paso.QUIZ    -> binding.pantallaQuiz.isVisible = true
            Paso.MEJ1    -> {
                binding.pantallaMejora1.isVisible = true
                binding.progressMejora1.max = 100
                binding.progressMejora1.setProgressCompat(50, true)
            }
            Paso.MEJ2    -> {
                binding.pantallaMejora2.isVisible = true
                binding.progressMejora2.max = 100
                binding.progressMejora2.setProgressCompat(100, true)
            }
            Paso.COMBATE -> binding.pantallaCombate.isVisible = true
        }
    }
}

enum class Paso { ATAQUE, BYTE, CONTEXTO, QUIZ, MEJ1, MEJ2, COMBATE }

private data class QuizQuestion(
    val texto: String,
    val opciones: List<String>,
    val opcionCorrecta: Int,
    val puntos: Int = 5
)
