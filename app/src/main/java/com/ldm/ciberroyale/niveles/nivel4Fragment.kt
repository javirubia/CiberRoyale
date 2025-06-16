package com.ldm.ciberroyale.niveles

import android.content.ClipData
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.ProgresoManager
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel4Binding

class Nivel4Fragment : Fragment(R.layout.fragment_nivel4) {

    private var _binding: FragmentNivel4Binding? = null
    private val binding get() = _binding!!

    //region Estado del Nivel
    private enum class Etapa { INTRO, PIN_PUZZLE, WIFI, RECOMPENSA }
    private var etapaActual = Etapa.INTRO

    // PIN Puzzle
    private lateinit var secret: List<Int>
    private data class Clue(val digits: List<Int>, val desc: String)
    private lateinit var clues: List<Clue>
    private var attemptsPin = 0
    private var puntuacionPin = 0

    // Wi-Fi Detective
    private var idxWifi = 0
    private var correctasWifi = 0
    private var puntuacionWifi = 0
    //endregion

    //region Ciclo de Vida
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
    //endregion

    //region Setup y Navegación
    private fun setupListeners() = with(binding) {
        btnIntroSiguiente.setOnClickListener {
            mostrarEtapa(Etapa.PIN_PUZZLE)
            iniciarPinPuzzle()
        }
        btnInfoPin.setOnClickListener {
            showInfoDialog(R.string.dialog_info_nivel4_pin_titulo, R.string.dialog_info_nivel4_pin_mensaje)
        }
        btnCheckPin.setOnClickListener {
            val guess = listOf(npDigit0.value, npDigit1.value, npDigit2.value)
            handlePinGuess(guess)
        }
        btnInfoWifi.setOnClickListener {
            showInfoDialog(R.string.dialog_info_nivel4_wifi_titulo, R.string.dialog_info_nivel4_wifi_mensaje)
        }
        configurarWifiDragAndDrop()

        btnRecompContinuar.setOnClickListener {
            val total = puntuacionPin + puntuacionWifi
            if (total < WIN_SCORE_THRESHOLD) {
                reiniciarNivel()
            } else {
                findNavController().navigate(R.id.action_nivel4Fragment_to_juegoFragment)
            }
        }
    }

    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        etapaActual = etapa
        listOf(pantallaIntro, pantallaPinPuzzle, pantallaWifi, scrollPantallaRecomp).forEach { it.isVisible = false }
        btnInfoPin.isVisible = false
        btnInfoWifi.isVisible = false

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
    //endregion

    //region Lógica PIN Puzzle
    private fun iniciarPinPuzzle() = with(binding) {
        secret = (0..9).shuffled().take(3)
        clues = listOf(
            Clue(findGuess { cp, wp -> cp == 0 && wp == 2 }, getString(R.string.nivel4_pin_pista1_desc)),
            Clue(findGuess { cp, wp -> cp == 1 && cp + wp == 2 }, getString(R.string.nivel4_pin_pista2_desc)),
            Clue(findGuess { cp, wp -> cp + wp == 0 }, getString(R.string.nivel4_pin_pista3_desc))
        )
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
        binding.tvPista4Desc.text = getString(R.string.nivel4_pin_pista4_suma, secret.sum())

        attemptsPin = 0
        puntuacionPin = 0
        tvFeedbackPin.text = ""
        listOf(npDigit0, npDigit1, npDigit2).forEach { np ->
            np.minValue = 0
            np.maxValue = 9
            np.value = 0
        }
    }

    private fun findGuess(condition: (correctPos: Int, wrongPos: Int) -> Boolean): List<Int> {
        val pool = (0..9).toList()
        while (true) {
            val guess = pool.shuffled().take(3)
            if (guess == secret) continue
            val (cp, wp) = evaluate(guess)
            if (condition(cp, wp)) return guess
        }
    }

    private fun evaluate(guess: List<Int>): Pair<Int, Int> {
        val correctPos = guess.withIndex().count { it.value == secret[it.index] }
        val common = guess.intersect(secret.toSet()).size
        val wrongPos = common - correctPos
        return correctPos to wrongPos
    }

    private fun handlePinGuess(guess: List<Int>) = with(binding) {
        if (guess.toSet().size < guess.size) {
            Toast.makeText(requireContext(), R.string.nivel4_pin_error_repetidos, Toast.LENGTH_SHORT).show()
            return
        }
        val (cp, wp) = evaluate(guess)
        attemptsPin++
        tvFeedbackPin.text = getString(R.string.nivel4_pin_feedback, cp, wp)

        if (cp == 3) { // Victoria
            if (attemptsPin <= 3) {
                ProgresoManager.unlockAchievement("NIVEL4_MENTE_MAESTRA")
            }
            puntuacionPin = ((MAX_ATTEMPTS_PIN - attemptsPin + 1) * MAX_SCORE_PIN) / MAX_ATTEMPTS_PIN
            pasarAWifi()
        } else if (attemptsPin >= MAX_ATTEMPTS_PIN) { // Derrota
            Toast.makeText(requireContext(), getString(R.string.nivel4_pin_derrota_toast, secret.joinToString("")), Toast.LENGTH_LONG).show()
            puntuacionPin = 0
            pasarAWifi()
        }
    }

    private fun pasarAWifi() {
        etapaActual = Etapa.WIFI
        mostrarEtapa(etapaActual)
        iniciarWifi()
    }
    //endregion

    //region Lógica Wi-Fi Detective
    private fun iniciarWifi() = with(binding) {
        idxWifi = 0
        correctasWifi = 0
        puntuacionWifi = 0
        showNetwork(EJEMPLOS_WIFI[idxWifi])
    }

    private fun showNetwork(net: WifiNetwork) = with(binding) {
        tvWifiSsid.text = net.ssid
        tvEncryption.text = net.encryption
        tvSignalLevel.text = "${net.signalLevel}/5"
        imgSignal.setImageResource(
            when (net.signalLevel) {
                in 1..2 -> R.drawable.ic_signal_low
                3 -> R.drawable.ic_signal_med
                else -> R.drawable.ic_signal_high
            }
        )
        tvClients.text = "${net.clients}"
    }

    private fun configurarWifiDragAndDrop() = with(binding) {
        cardSsid.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val listener = View.OnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_ENTERED -> { v.setBackgroundColor(Color.LTGRAY); true }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> { v.setBackgroundResource(R.drawable.bg_drop_target); true }
                DragEvent.ACTION_DROP -> { handleWifiDrop(v.id); true }
                else -> true
            }
        }
        targetSeguro.setOnDragListener(listener)
        targetPeligro.setOnDragListener(listener)
    }

    private fun handleWifiDrop(targetId: Int) {
        val net = EJEMPLOS_WIFI[idxWifi]
        val esAcierto = (targetId == binding.targetSeguro.id) == net.isSecure

        if (esAcierto) {
            correctasWifi++
            Toast.makeText(requireContext(), R.string.common_respuesta_correcta, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), R.string.common_respuesta_incorrecta, Toast.LENGTH_SHORT).show()
        }

        idxWifi++
        if (idxWifi < EJEMPLOS_WIFI.size) {
            showNetwork(EJEMPLOS_WIFI[idxWifi])
        } else {
            puntuacionWifi = (correctasWifi * MAX_SCORE_WIFI) / EJEMPLOS_WIFI.size
            etapaActual = Etapa.RECOMPENSA
            mostrarEtapa(etapaActual)
            mostrarRecompensa()
        }
    }
    //endregion

    //region Recompensa y UI Helpers
    private fun mostrarRecompensa() = with(binding) {
        val totalScore = puntuacionPin + puntuacionWifi
        tvPuntuacionFinal.text = getString(R.string.nivel1_recompensa_puntuacion, totalScore)

        if (totalScore < WIN_SCORE_THRESHOLD) {
            tvRecompensaTitulo.text = getString(R.string.nivel2_recompensa_titulo_derrota)
            tvRecompensaSubtitulo.text = getString(R.string.nivel4_recompensa_subtitulo_derrota)
            imgConfeti.setImageResource(R.drawable.byte_triste)
            btnRecompContinuar.text = getString(R.string.common_button_reintentar)
        } else {
            // ¡AQUÍ ESTÁ LA LÓGICA COMPLETA DE VICTORIA!
            // 1. Desbloqueamos el siguiente paso (el final del juego)
            ProgresoManager.desbloquearSiguienteNivel(4)

            // 2. Desbloqueamos los logros correspondientes
            ProgresoManager.unlockAchievement("NIVEL4_COMPLETADO")
            ProgresoManager.unlockAchievement("JUEGO_COMPLETADO")

            // 3. Mostramos la UI de victoria
            tvRecompensaTitulo.text = getString(R.string.nivel2_recompensa_titulo_victoria)
            tvRecompensaSubtitulo.text = getString(R.string.nivel4_recompensa_subtitulo_victoria)
            imgConfeti.setImageResource(R.drawable.byte_frame2)
            btnRecompContinuar.text = getString(R.string.common_button_finalizar_nivel)
        }
    }

    private fun reiniciarNivel() {
        puntuacionPin = 0
        puntuacionWifi = 0
        mostrarEtapa(Etapa.INTRO)
    }

    private fun showInfoDialog(titleRes: Int, msgRes: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setMessage(msgRes)
            .setPositiveButton(R.string.common_dialog_entendido, null)
            .show()
    }
    //endregion

    //region Datos y Constantes del Nivel
    companion object {
        private const val MAX_ATTEMPTS_PIN = 7
        private const val MAX_SCORE_PIN = 50
        private const val MAX_SCORE_WIFI = 50
        private const val WIN_SCORE_THRESHOLD = 50

        private data class WifiNetwork(
            val ssid: String,
            val isSecure: Boolean,
            val encryption: String,
            val signalLevel: Int,
            val clients: Int
        )

        private val EJEMPLOS_WIFI = listOf(
            WifiNetwork("Biblioteca_Free", false, "Abierta", 2, 5),
            WifiNetwork("Biblioteca_Oficial", true, "WPA2", 4, 12),
            WifiNetwork("FreeWiFi_Attack", false, "Abierta", 1, 3),
            WifiNetwork("Cafeteria_WiFi", true, "WPA3", 5, 8)
        )
    }
    //endregion
}