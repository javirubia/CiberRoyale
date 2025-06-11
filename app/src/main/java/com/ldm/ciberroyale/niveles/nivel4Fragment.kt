package com.ldm.ciberroyale.niveles

import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel4Binding

class Nivel4Fragment : Fragment(R.layout.fragment_nivel4) {

    private var _binding: FragmentNivel4Binding? = null
    private val binding get() = _binding!!

    private enum class Etapa { INTRO, PIN_PUZZLE, WIFI, RECOMPENSA }
    private var etapaActual = Etapa.INTRO

    // — PIN PUZZLE dinámico (igual que antes) —
    private lateinit var secret: List<Int>
    private data class Clue(val digits: List<Int>, val desc: String)
    private lateinit var clues: List<Clue>
    private var attemptsPin = 0
    private val maxAttemptsPin = 7
    private var puntuacionPin = 0

    // — Wi-Fi Detective enriquecido —
    private data class WifiNetwork(
        val ssid: String,
        val isSecure: Boolean,
        val encryption: String, // "WPA2", "WPA3" o "Abierta"
        val signalLevel: Int,   // 1..5
        val clients: Int
    )
    private val ejemplosWifi = listOf(
        WifiNetwork("Biblioteca_Free",    false, "Abierta", 2,  5),
        WifiNetwork("Biblioteca_Oficial", true,  "WPA2",   4, 12),
        WifiNetwork("FreeWiFi_Attack",    false, "Abierta", 1,  3),
        WifiNetwork("Cafeteria_WiFi",     true,  "WPA3",   5,  8)
    )
    private var idxWifi = 0
    private var correctasWifi = 0
    private var puntuacionWifi = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel4Binding.bind(view)
        setupListeners()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // Intro → PIN_PUZZLE
        btnIntroSiguiente.setOnClickListener {
            etapaActual = Etapa.PIN_PUZZLE
            mostrarEtapa(etapaActual)
            iniciarPinPuzzle()
        }
        // Info PIN
        btnInfoPin.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.info_titulo_pin)
                .setMessage(R.string.info_mensaje_pin)
                .setPositiveButton(R.string.entendido, null)
                .show()
        }
        // Comprobar PIN
        btnCheckPin.setOnClickListener {
            val guess = listOf(npDigit0.value, npDigit1.value, npDigit2.value)
            handlePinGuess(guess)
        }
        // Info Wi-Fi
        btnInfoWifi.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.info_titulo_wifi)
                .setMessage(R.string.info_mensaje_wifi)
                .setPositiveButton(R.string.entendido, null)
                .show()
        }
        // Drag&Drop Wi-Fi
        configurarWifiDragAndDrop()
        // Info detallada de la red
        btnInfoWifi.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.info_titulo_wifi))
                .setMessage(getString(R.string.info_mensaje_wifi))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        // Recompensa / Continuar o Reintentar
        btnRecompContinuar.setOnClickListener {
            val total = puntuacionPin + puntuacionWifi
            if (total < 50) {
                etapaActual = Etapa.PIN_PUZZLE
                mostrarEtapa(etapaActual)
                iniciarPinPuzzle()
            } else {
                findNavController().navigate(R.id.action_nivel4Fragment_to_juegoFragment)
            }
        }
    }
    // — PIN Puzzle dinámico —
    private fun iniciarPinPuzzle() = with(binding) {
        // 1) Generar secreto (3 dígitos únicos)
        secret = (0..9).shuffled().take(3)

        // 2) Generar pistas dinámicas basadas en el secreto
        clues = listOf(
            Clue(findGuess { cp, wp -> cp == 0 && wp == 2 }, getString(R.string.pista1_desc)),
            Clue(findGuess { cp, wp -> cp == 1 && cp + wp == 2 }, getString(R.string.pista2_desc)), // equivale a cp=1, wp=1
            Clue(findGuess { cp, wp -> cp + wp == 0 }, getString(R.string.pista3_desc))
        )

        // 3) Mostrar pistas en la UI
        clues[0].let {
            tvPista1Digit0.text = it.digits[0].toString()
            tvPista1Digit1.text = it.digits[1].toString()
            tvPista1Digit2.text = it.digits[2].toString()
            tvPista1Desc.text = it.desc
        }
        clues[1].let {
            tvPista2Digit0.text = it.digits[0].toString()
            tvPista2Digit1.text = it.digits[1].toString()
            tvPista2Digit2.text = it.digits[2].toString()
            tvPista2Desc.text = it.desc
        }
        clues[2].let {
            tvPista3Digit0.text = it.digits[0].toString()
            tvPista3Digit1.text = it.digits[1].toString()
            tvPista3Digit2.text = it.digits[2].toString()
            tvPista3Desc.text = it.desc
        }

        // Pista extra: la suma de los dígitos del secreto
        val suma = secret.sum()
        binding.tvPista4Desc.text = getString(R.string.pista4_suma, suma)

        // 4) Reiniciar estado del puzzle
        attemptsPin = 0
        puntuacionPin = 0
        tvFeedbackPin.text = ""

        // 5) Configurar NumberPickers
        listOf(npDigit0, npDigit1, npDigit2).forEach { np ->
            np.minValue = 0
            np.maxValue = 9
            np.value = 0 // Reiniciar valor
        }
    }

    /** Busca aleatoriamente un guess de 3 dígitos únicos que cumpla la condición. */
    private fun findGuess(condition: (correctPos: Int, wrongPos: Int) -> Boolean): List<Int> {
        val pool = (0..9).toList()
        while (true) {
            val guess = pool.shuffled().take(3)
            // Asegurarse de no generar el propio secreto como pista
            if (guess == secret) continue
            val (cp, wp) = evaluate(guess)
            if (condition(cp, wp)) return guess
        }
    }

    /** Evalúa un intento (guess) contra el secreto. Devuelve (dígitos en posición correcta, dígitos en posición incorrecta). */
    private fun evaluate(guess: List<Int>): Pair<Int, Int> {
        val correctPos = guess.withIndex().count { it.value == secret[it.index] }
        val common = guess.intersect(secret.toSet()).size
        val wrongPos = common - correctPos
        return correctPos to wrongPos
    }

    private fun handlePinGuess(guess: List<Int>) = with(binding) {
        // --- REFINAMIENTO APLICADO ---
        // Comprobar si el usuario ha introducido dígitos repetidos.
        // El PIN secreto no tiene repetidos, así que el intento tampoco debería.
        if (guess.toSet().size < guess.size) {
            Toast.makeText(requireContext(), "El PIN no contiene dígitos repetidos", Toast.LENGTH_SHORT).show()
            return // No se cuenta como intento y se notifica al usuario.
        }

        val (cp, wp) = evaluate(guess)
        attemptsPin++
        tvFeedbackPin.text = "En su sitio: $cp | En otro sitio: $wp"

        if (cp == 3) { // Victoria
            puntuacionPin = ((maxAttemptsPin - attemptsPin + 1) * 50) / maxAttemptsPin
            etapaActual = Etapa.WIFI
            mostrarEtapa(etapaActual)
            iniciarWifi()
        } else if (attemptsPin >= maxAttemptsPin) { // Derrota
            Toast.makeText(
                requireContext(),
                "¡Se acabaron los intentos! El PIN era: ${secret.joinToString("")}",
                Toast.LENGTH_LONG
            ).show()
            puntuacionPin = 0
            etapaActual = Etapa.WIFI
            mostrarEtapa(etapaActual)
            iniciarWifi()
        }
    }

        // — Wi-Fi Detective enriquecido —
    private fun iniciarWifi() = with(binding) {
        idxWifi = 0; correctasWifi = 0; puntuacionWifi = 0
        showNetwork(ejemplosWifi[idxWifi])
    }

    private fun showNetwork(net: WifiNetwork) = with(binding) {
        tvWifiSsid.text    = net.ssid
        tvEncryption.text  = net.encryption
        tvSignalLevel.text = "${net.signalLevel}/5"
        imgSignal.setImageResource(
            when (net.signalLevel) {
                in 1..2 -> R.drawable.ic_signal_low
                3       -> R.drawable.ic_signal_med
                else    -> R.drawable.ic_signal_high
            }
        )
        tvClients.text     = "${net.clients}"
    }

    private fun configurarWifiDragAndDrop() = with(binding) {
        cardSsid.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val listener = View.OnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_ENTERED -> { v.setBackgroundColor(Color.LTGRAY); true }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.setBackgroundResource(R.drawable.bg_drop_target); true
                }
                DragEvent.ACTION_DROP -> {
                    handleWifiDrop(v.id); true
                }
                else -> true
            }
        }
        targetSeguro.setOnDragListener(listener)
        targetPeligro.setOnDragListener(listener)
    }

    private fun handleWifiDrop(targetId: Int) = with(binding) {
        val net = ejemplosWifi[idxWifi]
        val correct = net.isSecure
        val user = (targetId == targetSeguro.id)

        if (user == correct) {
            correctasWifi++
            Toast.makeText(requireContext(), R.string.correcto, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.incorrecto, Toast.LENGTH_SHORT).show()
        }

        idxWifi++
        if (idxWifi < ejemplosWifi.size) {
            showNetwork(ejemplosWifi[idxWifi])
        } else {
            puntuacionWifi = correctasWifi * 50 / ejemplosWifi.size
            etapaActual = Etapa.RECOMPENSA
            mostrarEtapa(etapaActual)
            mostrarRecompensa()
        }
    }


    // — Recompensa —
    @SuppressLint("StringFormatMatches")
    private fun mostrarRecompensa() = with(binding) {
        val totalScore = puntuacionPin + puntuacionWifi
        tvPuntuacionFinal.text = getString(R.string.puntuacion_total, totalScore)

        if (totalScore < 50) {
            tvRecompensaTitulo.text = getString(R.string.derrota_general)
            imgConfeti.setImageResource(R.drawable.byte_triste)
            btnRecompContinuar.text = getString(R.string.btn_reintentar)
        } else {
            tvRecompensaTitulo.text = getString(R.string.victoria_general)
            imgConfeti.setImageResource(R.drawable.byte_frame2)
            btnRecompContinuar.text = getString(R.string.btn_continuar_final)
        }
    }

    // — Visibilidad de Etapas —
    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        // Ocultar todas las pantallas y botones de info
        listOf(pantallaIntro, pantallaPinPuzzle, pantallaWifi, scrollPantallaRecomp)
            .forEach { it.isVisible = false }
        btnInfoPin.isVisible = false
        btnInfoWifi.isVisible = false

        // Mostrar solo la pantalla y el botón de info correspondientes a la etapa actual
        when (etapa) {
            Etapa.INTRO -> pantallaIntro.isVisible = true
            Etapa.PIN_PUZZLE -> {
                pantallaPinPuzzle.isVisible = true
                btnInfoPin.isVisible = true
            }
            Etapa.WIFI -> {
                pantallaWifi.isVisible = true
                btnInfoWifi.isVisible = true
            }
            Etapa.RECOMPENSA -> scrollPantallaRecomp.isVisible = true
        }
    }
}
