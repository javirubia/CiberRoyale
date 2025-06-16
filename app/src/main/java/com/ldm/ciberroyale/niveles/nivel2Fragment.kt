package com.ldm.ciberroyale.niveles

import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

class Nivel2Fragment : Fragment() {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    private companion object {
        const val DURACION_BATALLA = 30         // segundos
        const val DURACION_MOLE = 800L         // ms
        const val PAUSA_MOLE = 200L            // ms
    }

    private enum class Etapa { INTRO, DETECT, URL, BAT_INTRO, BAT_JUGANDO, RECOMP }
    private var etapaActual = Etapa.INTRO

    // — Detect & URL —
    private var idxDetect = 0
    private var idxUrl = 0
    private var aciertosDetect = 0
    private var aciertosUrl = 0
    private val ejemplosDetect = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to false,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña" to false,
        "Notificación de tu UPS: tu paquete está en camino con enlace oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private val listaUrls = listOf(
        "https://mi-banco.com/login" to true,
        "http://secure-mi-banco.net/confirmar" to false,
        "https://www.faceboook.com" to false,
        "https://support.google.com" to true
    )

    // — Batalla pesca —
    private var score = 0
    private var timeLeft = DURACION_BATALLA
    private var currentMole = -1
    private var lastHole = -1
    private lateinit var holes: List<ImageButton>
    private var moleJob: Job? = null
    private var timerJob: Job? = null

    // — Puntuaciones parciales —
    private var ptsDetect = 0
    private var ptsUrl = 0
    private var ptsBatalla = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNivel2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        mostrarEtapa(etapaActual)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moleJob?.cancel()
        timerJob?.cancel()
        _binding = null
    }

    private fun setupListeners() = binding.run {
        btnIntroSiguiente.setOnClickListener {
            etapaActual = Etapa.DETECT
            mostrarEtapa(etapaActual)
            comenzarDetect()
        }
        btnInfoDetect.setOnClickListener { showInfo(R.string.info_titulo_detect, R.string.info_mensaje_detect) }
        btnInfoUrl.setOnClickListener { showInfo(R.string.info_titulo_url, R.string.info_mensaje_url) }
        btnInfoBatalla.setOnClickListener { showInfo(R.string.info_titulo_batalla, R.string.info_mensaje_batalla) }
        btnStartBattle.setOnClickListener {
            etapaActual = Etapa.BAT_JUGANDO
            mostrarEtapa(etapaActual)
            iniciarBatalla()
        }
        btnRecompContinuar.setOnClickListener {
            if (tvRecompensaTitulo.text.contains(getString(R.string.derrota_general), true)) {
                etapaActual = Etapa.INTRO
                mostrarEtapa(etapaActual)
            } else findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        }
    }

    // --- DETECT ---
    private fun comenzarDetect() = binding.run {
        idxDetect = 0; aciertosDetect = 0
        tvMensaje.text = ejemplosDetect[0].first
        habilitarDetect()
    }

    private fun habilitarDetect() = binding.run {
        cardMensaje.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        targetSeguro.setOnDragListener(createDropListener(::procesarDetect))
        targetPhishing.setOnDragListener(createDropListener(::procesarDetect))
    }

    private fun procesarDetect(targetId: Int) = binding.run {
        val correcto = ejemplosDetect[idxDetect].second
        val userPhishing = targetId == targetPhishing.id
        if (userPhishing != correcto) aciertosDetect++
        Toast.makeText(requireContext(), if (userPhishing != correcto) getString(R.string.correcto) else getString(R.string.incorrecto), Toast.LENGTH_SHORT).show()
        idxDetect++
        if (idxDetect < ejemplosDetect.size) tvMensaje.text = ejemplosDetect[idxDetect].first
        else {
            deshabilitarDetect()
            ptsDetect = aciertosDetect * 4
            etapaActual = Etapa.URL
            mostrarEtapa(etapaActual)
            comenzarUrl()
        }
    }

    private fun deshabilitarDetect() = binding.run {
        cardMensaje.setOnLongClickListener(null)
        targetSeguro.setOnDragListener(null)
        targetPhishing.setOnDragListener(null)
    }

    // --- URL ---
    private fun comenzarUrl() = binding.run {
        idxUrl = 0; aciertosUrl = 0
        tvUrl.text = listaUrls[0].first
        habilitarUrl()
    }

    private fun habilitarUrl() = binding.run {
        cardUrl.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        targetValida.setOnDragListener(createDropListener(::procesarUrl))
        targetInvalida.setOnDragListener(createDropListener(::procesarUrl))
    }

    private fun procesarUrl(targetId: Int) = binding.run {
        val valido = listaUrls[idxUrl].second
        val userValid = targetId == targetValida.id
        if (userValid == valido) aciertosUrl++
        Toast.makeText(requireContext(), if (userValid == valido) getString(R.string.correcto) else getString(R.string.incorrecto), Toast.LENGTH_SHORT).show()
        idxUrl++
        if (idxUrl < listaUrls.size) tvUrl.text = listaUrls[idxUrl].first
        else {
            deshabilitarUrl()
            ptsUrl = aciertosUrl * 5
            etapaActual = Etapa.BAT_INTRO
            mostrarEtapa(etapaActual)
        }
    }

    private fun deshabilitarUrl() = binding.run {
        cardUrl.setOnLongClickListener(null)
        targetValida.setOnDragListener(null)
        targetInvalida.setOnDragListener(null)
    }

    private fun createDropListener(onDrop: (Int) -> Unit) = View.OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> { v.setBackgroundColor(Color.LTGRAY); true }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> { v.setBackgroundResource(R.drawable.bg_drop_target); true }
            DragEvent.ACTION_DROP -> { onDrop(v.id); true }
            else -> true
        }
    }

    // --- BATALLA PESCA ---
    private fun iniciarBatalla() = binding.run {
        score = 0; timeLeft = DURACION_BATALLA; currentMole = -1; lastHole = -1
        holes = listOf(hoyo0,hoyo1,hoyo2,hoyo3,hoyo4,hoyo5,hoyo6,hoyo7,hoyo8,hoyo9,hoyo10,hoyo11)
        holes.forEachIndexed { idx, btn ->
            btn.setImageResource(R.drawable.ic_hole)
            btn.scaleY = 1f; btn.translationY = 0f; btn.animate().setListener(null).cancel()
            btn.setOnClickListener { if (idx == currentMole && timeLeft>0) atraparPez(idx) }
        }
        battleIntroLayout.isVisible = false
        imgCana.isVisible = true
        layoutInfoBatalla.isVisible = true
        gridMoles.isVisible = true
        tvScore.text = getString(R.string.score_label, score)
        tvTimer.text = getString(R.string.tiempo_label, timeLeft)

        moleJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            while (timeLeft>0 && isActive) {
                var next: Int
                do { next = Random.nextInt(holes.size) } while (next==lastHole)
                lastHole = next; currentMole = next
                val hole = holes[next]
                hole.setImageResource(R.drawable.viper)
                hole.scaleY = 0f
                hole.animate().scaleY(1f).setDuration(200).setInterpolator(DecelerateInterpolator()).setListener(null).start()
                delay(DURACION_MOLE)
                if (currentMole==next) hundirPez(hole)
                delay(PAUSA_MOLE)
            }
        }

        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (timeLeft>0 && isActive) {
                delay(1000)
                timeLeft--
                tvTimer.text = getString(R.string.tiempo_label, timeLeft)
            }
            if (isActive) terminarBatalla()
        }
    }

    private fun atraparPez(idx: Int) {
        score++
        binding.tvScore.text = getString(R.string.score_label, score)
        animarSubida(holes[idx])
        currentMole = -1
    }

    private fun hundirPez(hole: ImageButton) {
        currentMole = -1
        hole.animate().scaleY(0f).setDuration(200).setInterpolator(AccelerateInterpolator())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hole.setImageResource(R.drawable.ic_hole); hole.scaleY = 1f
                }
            }).start()
    }

    private fun animarSubida(hole: ImageButton) {
        hole.animate().translationYBy(-500f).setDuration(400).setInterpolator(DecelerateInterpolator())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hole.setImageResource(R.drawable.ic_hole)
                    hole.scaleY = 1f; hole.translationY = 0f
                }
            }).start()
    }

    @SuppressLint("StringFormatInvalid")
    private fun terminarBatalla() = binding.run {
        moleJob?.cancel(); timerJob?.cancel()
        ptsBatalla = if (score>=10) 60 else score*6
        val finalScore = ptsDetect + ptsUrl + ptsBatalla
        etapaActual = Etapa.RECOMP
        mostrarEtapa(etapaActual)
        tvPuntuacionFinal.text = getString(R.string.puntuacion_total, finalScore)
        when {
            finalScore<50    -> showResult(R.string.derrota_general, R.string.resultado_mal, R.drawable.byte_triste, R.string.btn_reintentar)
            finalScore in 50..70 -> showResult(R.string.victoria_general, R.string.resultado_bien, R.drawable.byte_frame2, R.string.btn_continuar_final)
            finalScore in 71..99 -> showResult(R.string.victoria_general, R.string.resultado_muy_bien, R.drawable.byte_frame3, R.string.btn_continuar_final)
            else             -> showResult(R.string.victoria_general, R.string.resultado_perfecto, R.drawable.byte_frame1, R.string.btn_continuar_final)
        }
    }

    private fun showResult(titleId:Int, subId:Int, iconId:Int, btnTextId:Int) = binding.run {
        tvRecompensaTitulo.text = getString(titleId)
        tvRecompensaSubtitulo.text = getString(subId)
        imgConfeti.setImageResource(iconId)
        btnRecompContinuar.text = getString(btnTextId)
        imgConfeti.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
    }

    private fun mostrarEtapa(et: Etapa) = binding.run {
        listOf(pantallaIntro, pantallaDetec, pantallaUrl, pantallaCombate, scrollPantallaRecomp).forEach { it.isVisible=false }
        listOf(btnInfoDetect, btnInfoUrl, btnInfoBatalla).forEach { it.isVisible=false }
        when(et) {
            Etapa.INTRO      -> pantallaIntro.isVisible = true
            Etapa.DETECT     -> {
                pantallaDetec.isVisible = true;
                btnInfoDetect.isVisible = true}
            Etapa.URL        -> {pantallaUrl.isVisible = true; btnInfoUrl.isVisible = true}
            Etapa.BAT_INTRO, Etapa.BAT_JUGANDO -> {pantallaCombate.isVisible = true; btnInfoBatalla.isVisible = true}
            Etapa.RECOMP     -> scrollPantallaRecomp.isVisible = true
        }
    }

    private fun showInfo(titleRes:Int, msgRes:Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(titleRes))
            .setMessage(getString(msgRes))
            .setPositiveButton(getString(R.string.entendido)) { d,_ -> d.dismiss() }
            .show()
    }
}