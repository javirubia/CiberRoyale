package com.ldm.ciberroyale.niveles

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel2Binding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val DURACION_BATALLA_SEG = 30
        private const val DURACION_MOLE_MS = 800L
        private const val PAUSA_ENTRE_MOLES_MS = 200L
    }

    private enum class Etapa { INTRO, DETECT, URL, BATALLA_INICIO, BATALLA_JUGANDO, RECOMPENSA }
    private var etapaActual: Etapa = Etapa.INTRO

    // ——————————————
    // Variables DETECT / URL
    // ——————————————
    private var correctasDetect = 0
    private var correctasUrl = 0

    private val ejemplosDetect = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to false,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña" to false,
        "Notificación de tu UPS: tu paquete está en camino con enlace oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private var idxMensaje = 0

    private val listaUrls = listOf(
        "https://mi-banco.com/login" to true,
        "http://secure-mi-banco.net/confirmar" to false,
        "https://www.faceboook.com" to false,
        "https://support.google.com" to true
    )
    private var idxUrl = 0

    // ——————————————
    // Variables de BATALLA
    // ——————————————
    private var scoreBatalla = 0
    private var tiempoRestante = DURACION_BATALLA_SEG
    private var moleIndex = -1
    private var ultimoHoyo = -1

    private lateinit var holes: List<ImageButton>
    private var batallaJob: Job? = null
    private var moleJob: Job? = null

    // ——————————————
    // Variables de Puntuación final
    // ——————————————
    private var puntuacionDetect = 0    // 0..20
    private var puntuacionUrl = 0       // 0..20
    private var puntuacionCombate = 0   // 0..60

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel2Binding.bind(view)
        setupListeners()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        batallaJob?.cancel()
        moleJob?.cancel()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // —————————————
        // Intro → pasar a DETECT
        // —————————————
        btnIntroSiguiente.setOnClickListener {
            etapaActual = Etapa.DETECT
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // —————————————
        // INFO DETECT
        // —————————————
        btnInfoDetect.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.info_titulo_detect))
                .setMessage(getString(R.string.info_mensaje_detect))
                .setPositiveButton(getString(R.string.entendido)) { d, _ -> d.dismiss() }
                .show()
        }

        // —————————————
        // INFO URL
        // —————————————
        btnInfoUrl.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.info_titulo_url))
                .setMessage(getString(R.string.info_mensaje_url))
                .setPositiveButton(getString(R.string.entendido)) { d, _ -> d.dismiss() }
                .show()
        }

        // —————————————
        // INFO BATALLA
        // —————————————
        btnInfoBatalla.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.info_titulo_batalla))
                .setMessage(getString(R.string.info_mensaje_batalla))
                .setPositiveButton(getString(R.string.entendido)) { d, _ -> d.dismiss() }
                .show()
        }

        // —————————————
        // Botón Recompensa (Continuar o Reintentar)
        // —————————————
        btnRecompContinuar.setOnClickListener {
            val titulo = tvRecompensaTitulo.text.toString()
            if (titulo.contains(getString(R.string.derrota_general), ignoreCase = true)) {
                // Si era “Derrota”, reiniciamos la batalla
                etapaActual = Etapa.INTRO
                mostrarEtapa(Etapa.INTRO)
            } else {
                // Si era “Victoria”, volvemos al menú principal
                findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
            }
        }

        // —————————————
        // Botón “Comenzar batalla” (subpantalla de introducción)
        // —————————————
        btnStartBattle.setOnClickListener {
            binding.battleIntroLayout.isVisible = false
            binding.imgCana.isVisible           = true
            binding.layoutInfoBatalla.isVisible = true
            binding.gridMoles.isVisible         = true
            etapaActual = Etapa.BATALLA_JUGANDO
            iniciarBatalla()
        }

    }

    // —————————————
    //  DETECT
    // —————————————
    private fun iniciarDetect() = with(binding) {
        idxMensaje = 0
        correctasDetect = 0
        tvMensaje.text = ejemplosDetect[idxMensaje].first
        configurarDetectDragAndDrop()
    }

    private fun configurarDetectDragAndDrop() = with(binding) {
        cardMensaje.setOnLongClickListener { v ->
            v.startDragAndDrop(
                ClipData.newPlainText("", ""),
                View.DragShadowBuilder(v),
                v,
                0
            )
            true
        }
        val listener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.setBackgroundColor(Color.LTGRAY); true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.setBackgroundResource(R.drawable.bg_drop_target); true
                }
                DragEvent.ACTION_DROP -> {
                    handleDetectDrop(v.id); true
                }
                else -> true
            }
        }
        targetSeguro.setOnDragListener(listener)
        targetPhishing.setOnDragListener(listener)
    }

    private fun removerDetectDragAndDrop() = with(binding) {
        cardMensaje.setOnLongClickListener(null)
        targetSeguro.setOnDragListener(null)
        targetPhishing.setOnDragListener(null)
        targetSeguro.setBackgroundResource(R.drawable.bg_drop_target)
        targetPhishing.setBackgroundResource(R.drawable.bg_drop_target)
    }

    private fun handleDetectDrop(targetId: Int) = with(binding) {
        val esPhishingCorrecto = !ejemplosDetect[idxMensaje].second
        val usuarioDijoPhishing = (targetId == binding.targetPhishing.id)
        val acerto = (usuarioDijoPhishing && esPhishingCorrecto) ||
                (!usuarioDijoPhishing && !esPhishingCorrecto)

        if (acerto) correctasDetect++
        Toast.makeText(
            requireContext(),
            if (acerto) getString(R.string.correcto) else getString(R.string.incorrecto),
            Toast.LENGTH_SHORT
        ).show()

        idxMensaje++
        if (idxMensaje < ejemplosDetect.size) {
            tvMensaje.text = ejemplosDetect[idxMensaje].first
        } else {
            // Ya terminó Detect → calculamos su parte (20 pts máximo)
            removerDetectDragAndDrop()
            puntuacionDetect = correctasDetect * 4   // 5 preguntas → 4 pts c/u = 20
            etapaActual = Etapa.URL
            mostrarEtapa(Etapa.URL)
            iniciarUrl()
        }
    }

    // —————————————
    //    URL
    // —————————————
    private fun iniciarUrl() = with(binding) {
        idxUrl = 0
        correctasUrl = 0
        tvUrl.text = listaUrls[idxUrl].first
        configurarUrlDragAndDrop()
    }

    private fun configurarUrlDragAndDrop() = with(binding) {
        cardUrl.setOnLongClickListener { v ->
            v.startDragAndDrop(
                ClipData.newPlainText("", ""),
                View.DragShadowBuilder(v),
                v,
                0
            )
            true
        }
        val listener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.alpha = 0.5f; true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1f; true
                }
                DragEvent.ACTION_DROP -> {
                    handleUrlDrop(v.id); true
                }
                else -> true
            }
        }
        targetValida.setOnDragListener(listener)
        targetInvalida.setOnDragListener(listener)
    }

    private fun removerUrlDragAndDrop() = with(binding) {
        cardUrl.setOnLongClickListener(null)
        targetValida.setOnDragListener(null)
        targetInvalida.setOnDragListener(null)
        targetValida.alpha = 1f
        targetInvalida.alpha = 1f
    }

    private fun handleUrlDrop(targetId: Int) = with(binding) {
        val esValidaCorrecto = listaUrls[idxUrl].second
        val respuestaUsuarioEsValida = (targetId == binding.targetValida.id)
        val acerto = (respuestaUsuarioEsValida == esValidaCorrecto)

        if (acerto) correctasUrl++
        Toast.makeText(
            requireContext(),
            if (acerto) getString(R.string.correcto) else getString(R.string.incorrecto),
            Toast.LENGTH_SHORT
        ).show()

        idxUrl++
        if (idxUrl < listaUrls.size) {
            tvUrl.text = listaUrls[idxUrl].first
        } else {
            // Terminó URL → calculamos su parte (20 pts máximo)
            removerUrlDragAndDrop()
            puntuacionUrl = correctasUrl * 5   // 4 preguntas → 5 pts c/u = 20
            etapaActual = Etapa.BATALLA_INICIO
            mostrarEtapa(Etapa.BATALLA_INICIO)
        }
    }

    // —————————————
    //   BATALLA
    // —————————————
    private fun iniciarBatalla() = with(binding) {
        // 1) Reiniciar estado
        scoreBatalla = 0
        tiempoRestante = DURACION_BATALLA_SEG
        moleIndex = -1
        ultimoHoyo = -1

        // 2) Mostrar Score y Timer
        layoutInfoBatalla.isVisible = true
        tvScore.isVisible = true
        tvTimer.isVisible = true
        tvScore.text   = getString(R.string.score_label, scoreBatalla)
        tvTimer.text   = getString(R.string.tiempo_label, tiempoRestante)

        // 3) Preparar lista de “charcos” (hoyos)
        holes = listOf(
            hoyo0, hoyo1, hoyo2,
            hoyo3, hoyo4, hoyo5,
            hoyo6, hoyo7, hoyo8,
            hoyo9, hoyo10, hoyo11
        )
        holes.forEach { holeButton ->
            holeButton.setImageResource(R.drawable.ic_hole)
            holeButton.scaleY = 1f
            holeButton.translationY = 0f
            holeButton.animate().setListener(null).cancel()
        }

        // 4) Listener para “pescar” el pez/Viper
        holes.forEachIndexed { index, holeButton ->
            holeButton.setOnClickListener {
                if (index == moleIndex && tiempoRestante > 0) {
                    scoreBatalla++
                    tvScore.text = getString(R.string.score_label, scoreBatalla)
                    val caughtHole = holes[moleIndex]
                    moleIndex = -1
                    animateFishUp(caughtHole)
                }
            }
        }

        // 5) Cancelar loops previos
        moleJob?.cancel()
        batallaJob?.cancel()

        // 6) Comenzamos coroutine que controla aparición / desaparición de “pez”
        moleJob = viewLifecycleOwner.lifecycleScope.launch {
            // Pequeña espera para que el layout se estabilice
            delay(300)
            while (tiempoRestante > 0 && isActive) {
                var nuevoIndex: Int
                do {
                    nuevoIndex = Random.nextInt(holes.size)
                } while (nuevoIndex == ultimoHoyo)
                ultimoHoyo = nuevoIndex
                moleIndex = nuevoIndex

                // Mostrar “pez” (Viper) emergiendo del charco
                val currentHole = holes[nuevoIndex]
                currentHole.setImageResource(R.drawable.viper)
                currentHole.scaleY = 0f
                currentHole.animate()
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(null)
                    .start()

                // Esperar DURACION_MOLE_MS
                delay(DURACION_MOLE_MS)

                // Si aun no fue pescado, hundimos el pez
                if (moleIndex == nuevoIndex) {
                    val timedOutHole = holes[moleIndex]
                    moleIndex = -1
                    animateFishSink(timedOutHole)
                }

                // Pequeña pausa
                delay(PAUSA_ENTRE_MOLES_MS)
            }
        }

        // 7) Comenzamos coroutine que cuenta el tiempo restante
        batallaJob = viewLifecycleOwner.lifecycleScope.launch {
            while (tiempoRestante > 0 && isActive) {
                delay(1000)
                if (!isActive) break
                tiempoRestante--
                tvTimer.text = getString(R.string.tiempo_label, tiempoRestante)
            }
            // Cuando el tiempo se acaba
            if (isActive) {
                // Si hay un pez “activo”, lo hundimos antes de terminar
                if (moleIndex != -1 && moleIndex in holes.indices) {
                    val lastHole = holes[moleIndex]
                    moleIndex = -1
                    animateFishSink(lastHole)
                }
                terminarBatalla()
            }
        }
    }

    /**
     * El pez “capturado” sube en 400 ms y luego se reemplaza por el charco.
     */
    private fun animateFishUp(holeButton: ImageButton) {
        holeButton.animate()
            .translationYBy(-500f)    // Ajusta esta distancia si lo necesitas
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    holeButton.animate().setListener(null)
                    holeButton.setImageResource(R.drawable.ic_hole)
                    holeButton.scaleY = 1f
                    holeButton.translationY = 0f
                }
                override fun onAnimationCancel(animation: Animator) {
                    onAnimationEnd(animation)
                }
            })
            .start()
    }

    /**
     * El pez que no fue pescado “hunde” en 200 ms y vuelve a ser charco.
     */
    private fun animateFishSink(holeButton: ImageButton) {
        holeButton.animate()
            .scaleY(0f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    holeButton.animate().setListener(null)
                    holeButton.setImageResource(R.drawable.ic_hole)
                    holeButton.scaleY = 1f
                }
                override fun onAnimationCancel(animation: Animator) {
                    onAnimationEnd(animation)
                }
            })
            .start()
    }

    /**
     * Al terminar la Batalla (tiempo = 0), calculamos la parte de la puntuación (60 puntos máximo),
     * mostramos pantalla de Recompensa, y asignamos Textos/Imágenes igual que en Nivel 1.
     */
    @SuppressLint("StringFormatInvalid")
    private fun terminarBatalla() = with(binding) {
        moleJob?.cancel()
        batallaJob?.cancel()

        // Calculamos la parte de la batalla (scoreBatalla)
        // 10 o más capturas → 60 pts; cada captura vale 6 pts
        puntuacionCombate = if (scoreBatalla >= 10) 60 else (scoreBatalla * 6)

        // Sumar todas las partes: Detect (20) + URL (20) + Combate (60) = 0..100
        val finalScore = puntuacionDetect + puntuacionUrl + puntuacionCombate

        // Mostramos pantalla de Recompensa
        etapaActual = Etapa.RECOMPENSA
        mostrarEtapa(Etapa.RECOMPENSA)

        // Le ponemos el texto de la puntuación total
        tvPuntuacionFinal.text = getString(R.string.puntuacion_total, finalScore)

        // Decoramos según rangos EXACTOS del Nivel 1:
        // (<50 → Derrota | 50..70 → Bien | 71..99 → Muy bien | >=100 → Perfecto)
        when {
            finalScore < 50 -> {
                tvRecompensaTitulo.text = getString(R.string.derrota_general)
                tvRecompensaSubtitulo.text = getString(R.string.resultado_mal)
                imgConfeti.setImageResource(R.drawable.byte_triste)
                btnRecompContinuar.text = getString(R.string.btn_reintentar)
            }
            finalScore in 50..70 -> {
                tvRecompensaTitulo.text = getString(R.string.victoria_general)
                tvRecompensaSubtitulo.text = getString(R.string.resultado_bien)
                imgConfeti.setImageResource(R.drawable.byte_frame2)
                btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore in 71..99 -> {
                tvRecompensaTitulo.text = getString(R.string.victoria_general)
                tvRecompensaSubtitulo.text = getString(R.string.resultado_muy_bien)
                imgConfeti.setImageResource(R.drawable.byte_frame3)
                btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
            finalScore >= 100 -> {
                tvRecompensaTitulo.text = getString(R.string.victoria_general)
                tvRecompensaSubtitulo.text = getString(R.string.resultado_perfecto)
                imgConfeti.setImageResource(R.drawable.byte_frame1)
                btnRecompContinuar.text = getString(R.string.btn_continuar_final)
            }
        }


    }

    // —————————————
    //  Mostrar / Ocultar pantallas
    // —————————————
    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        listOf(
            pantallaIntro,
            pantallaDetec,
            pantallaUrl,
            pantallaCombate,
            scrollPantallaRecomp
        ).forEach { it.isVisible = false }

        btnInfoDetect.isVisible = false
        btnInfoUrl.isVisible = false
        btnInfoBatalla.isVisible = false

        when (etapa) {
            Etapa.INTRO -> {
                pantallaIntro.isVisible = true
            }
            Etapa.DETECT -> {
                pantallaDetec.isVisible = true
                btnInfoDetect.isVisible = true
            }
            Etapa.URL -> {
                pantallaUrl.isVisible = true
                btnInfoUrl.isVisible = true
            }
            Etapa.BATALLA_INICIO -> {
                binding.pantallaCombate.isVisible   = true
                binding.btnInfoBatalla.isVisible    = true
                binding.battleIntroLayout.isVisible = true
            }
            Etapa.BATALLA_JUGANDO -> {
                // Ya se encargará iniciarBatalla() de mostrar los elementos de la batalla
                pantallaCombate.isVisible = true
                btnInfoBatalla.isVisible = true
            }
            Etapa.RECOMPENSA -> {
                scrollPantallaRecomp.isVisible = true
            }
        }
    }
}
