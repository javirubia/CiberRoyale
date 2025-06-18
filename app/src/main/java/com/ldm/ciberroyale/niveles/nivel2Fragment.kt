package com.ldm.ciberroyale.niveles

import android.animation.AnimatorListenerAdapter
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
import com.ldm.ciberroyale.ProgresoManager
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.SoundManager
import com.ldm.ciberroyale.databinding.FragmentNivel2Binding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class Nivel2Fragment : Fragment() {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    //region Estado del Nivel
    private var etapaActual = Etapa.INTRO
    private var idxDetect = 0
    private var idxUrl = 0
    private var aciertosDetect = 0
    private var aciertosUrl = 0
    private var scoreBatalla = 0
    private var timeLeft = DURACION_BATALLA_S
    private var currentMole = -1
    private var lastHole = -1
    private lateinit var holes: List<ImageButton>
    private var moleJob: Job? = null
    private var timerJob: Job? = null
    private var puntuacionTotal = 0
    //endregion

    private enum class Etapa { INTRO, DETECT, URL, BAT_INTRO, BAT_JUGANDO, RECOMP }

    //region Ciclo de Vida del Fragment
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
    //endregion

    override fun onResume() {
        super.onResume()
        // Cuando entramos al nivel, ponemos la música de juego
        SoundManager.playMusic(requireContext(), R.raw.music_ingame) // <-- AÑADIDO
    }

    override fun onPause() {
        super.onPause()
        // Paramos toda la música al salir
        SoundManager.stopMusic() // <-- AÑADIDO
    }
    //region Configuración Inicial y Navegación
    private fun setupListeners() = binding.run {
        btnIntroSiguiente.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            mostrarEtapa(Etapa.DETECT)
            comenzarDetect()
        }
        btnInfoDetect.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfo(R.string.dialog_info_nivel2_detect_titulo, R.string.dialog_info_nivel2_detect_mensaje) }
        btnInfoUrl.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfo(R.string.dialog_info_nivel2_url_titulo, R.string.dialog_info_nivel2_url_mensaje) }
        btnInfoBatalla.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfo(R.string.dialog_info_nivel2_batalla_titulo, R.string.dialog_info_nivel2_batalla_mensaje) }

        btnStartBattle.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_level_start) // <-- AÑADIDO
            mostrarEtapa(Etapa.BAT_JUGANDO)
            iniciarBatalla()
        }
        btnRecompContinuar.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); onFinalizarNivel() }
    }

    private fun mostrarEtapa(et: Etapa) {
        etapaActual = et
        binding.run {
            listOf(pantallaIntro, pantallaDetec, pantallaUrl, pantallaCombate, scrollPantallaRecomp).forEach { it.isVisible = false }
            listOf(btnInfoDetect, btnInfoUrl, btnInfoBatalla).forEach { it.isVisible = false }
            when (et) {
                Etapa.INTRO -> pantallaIntro.isVisible = true
                Etapa.DETECT -> {
                    pantallaDetec.isVisible = true
                    btnInfoDetect.isVisible = true
                }
                Etapa.URL -> {
                    pantallaUrl.isVisible = true
                    btnInfoUrl.isVisible = true
                }
                Etapa.BAT_INTRO, Etapa.BAT_JUGANDO -> {
                    pantallaCombate.isVisible = true
                    btnInfoBatalla.isVisible = true
                }
                Etapa.RECOMP -> scrollPantallaRecomp.isVisible = true
            }
        }
    }
    //endregion

    //region Lógica de Detección de Phishing y URL
    private fun comenzarDetect() = binding.run {
        idxDetect = 0
        aciertosDetect = 0
        tvMensaje.text = EJEMPLOS_DETECT[idxDetect].first
        habilitarDragAndDrop(cardMensaje, targetSeguro, targetPhishing, ::procesarDetect)
    }

    private fun procesarDetect(targetId: Int) {
        val esPhishingCorrecto = EJEMPLOS_DETECT[idxDetect].second
        val usuarioAcierta = (targetId == binding.targetPhishing.id) == esPhishingCorrecto
        if (usuarioAcierta) {
            SoundManager.playSfx(R.raw.sfx_correct_answer) // <-- AÑADIDO
            aciertosDetect++
        } else {
            SoundManager.playSfx(R.raw.sfx_wrong_answer) // <-- AÑADIDO
        }

        val feedbackToast = if (usuarioAcierta) R.string.common_respuesta_correcta else R.string.common_respuesta_incorrecta
        Toast.makeText(requireContext(), feedbackToast, Toast.LENGTH_SHORT).show()

        idxDetect++
        if (idxDetect < EJEMPLOS_DETECT.size) {
            binding.tvMensaje.text = EJEMPLOS_DETECT[idxDetect].first
        } else {
            deshabilitarDragAndDrop(binding.cardMensaje, binding.targetSeguro, binding.targetPhishing)
            mostrarEtapa(Etapa.URL)
            comenzarUrl()
        }
    }

    private fun comenzarUrl() = binding.run {
        idxUrl = 0
        aciertosUrl = 0
        tvUrl.text = LISTA_URLS[idxUrl].first
        habilitarDragAndDrop(cardUrl, targetValida, targetInvalida, ::procesarUrl)
    }

    private fun procesarUrl(targetId: Int) {
        val esValidaCorrecta = LISTA_URLS[idxUrl].second
        val usuarioAcierta = (targetId == binding.targetValida.id) == esValidaCorrecta
        if (usuarioAcierta) {
            SoundManager.playSfx(R.raw.sfx_correct_answer) // <-- AÑADIDO
            aciertosUrl++
        } else {
            SoundManager.playSfx(R.raw.sfx_wrong_answer) // <-- AÑADIDO
        }
        val feedbackToast = if (usuarioAcierta) R.string.common_respuesta_correcta else R.string.common_respuesta_incorrecta
        Toast.makeText(requireContext(), feedbackToast, Toast.LENGTH_SHORT).show()

        idxUrl++
        if (idxUrl < LISTA_URLS.size) {
            binding.tvUrl.text = LISTA_URLS[idxUrl].first
        } else {
            deshabilitarDragAndDrop(binding.cardUrl, binding.targetValida, binding.targetInvalida)
            mostrarEtapa(Etapa.BAT_INTRO)
        }
    }

    private fun habilitarDragAndDrop(dragView: View, target1: View, target2: View, onDrop: (Int) -> Unit) {
        dragView.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val listener = createDropListener(onDrop)
        target1.setOnDragListener(listener)
        target2.setOnDragListener(listener)
    }

    private fun deshabilitarDragAndDrop(dragView: View, target1: View, target2: View) {
        dragView.setOnLongClickListener(null)
        target1.setOnDragListener(null)
        target2.setOnDragListener(null)
    }

    private fun createDropListener(onDrop: (Int) -> Unit) = View.OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> { v.setBackgroundColor(Color.LTGRAY); true }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> { v.setBackgroundResource(R.drawable.bg_drop_target); true }
            DragEvent.ACTION_DROP -> { onDrop(v.id); true }
            else -> true
        }
    }
    //endregion

    //region Batalla de Pesca
    private fun iniciarBatalla() = binding.run {
        scoreBatalla = 0
        timeLeft = DURACION_BATALLA_S
        currentMole = -1
        lastHole = -1
        holes = listOf(hoyo0, hoyo1, hoyo2, hoyo3, hoyo4, hoyo5, hoyo6, hoyo7, hoyo8, hoyo9, hoyo10, hoyo11)
        holes.forEachIndexed { idx, btn ->
            btn.setImageResource(R.drawable.ic_hole)
            btn.scaleY = 1f; btn.translationY = 0f; btn.animate().setListener(null).cancel()
            btn.setOnClickListener { if (idx == currentMole && timeLeft > 0) atraparPez(idx) }
        }
        battleIntroLayout.isVisible = false
        imgCana.isVisible = true
        layoutInfoBatalla.isVisible = true
        gridMoles.isVisible = true
        tvScore.text = getString(R.string.nivel2_batalla_score, scoreBatalla)
        tvTimer.text = getString(R.string.nivel2_batalla_tiempo, timeLeft)

        moleJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            while (timeLeft > 0 && isActive) {
                var nextHole: Int
                do { nextHole = Random.nextInt(holes.size) } while (nextHole == lastHole)
                lastHole = nextHole
                currentMole = nextHole
                val hole = holes[nextHole]
                hole.setImageResource(R.drawable.viper)
                hole.scaleY = 0f
                hole.animate().scaleY(1f).setDuration(DURACION_APARECER_MS).setInterpolator(DecelerateInterpolator()).setListener(null).start()
                delay(DURACION_VISIBLE_MS)
                if (currentMole == nextHole) hundirPez(hole)
                delay(PAUSA_ENTRE_MOLES_MS)
            }
        }

        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (timeLeft > 0 && isActive) {
                delay(1000)
                timeLeft--
                tvTimer.text = getString(R.string.nivel2_batalla_tiempo, timeLeft)
            }
            if (isActive) terminarBatalla()
        }
    }

    private fun atraparPez(idx: Int) {
        SoundManager.playSfx(R.raw.punch)
        scoreBatalla++
        binding.tvScore.text = getString(R.string.nivel2_batalla_score, scoreBatalla)
        animarSubida(holes[idx])
        currentMole = -1
    }

    private fun hundirPez(hole: ImageButton) {
        currentMole = -1
        hole.animate().scaleY(0f).setDuration(DURACION_DESAPARECER_MS).setInterpolator(AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hole.setImageResource(R.drawable.ic_hole); hole.scaleY = 1f
                }
            }).start()
    }

    private fun animarSubida(hole: ImageButton) {
        hole.animate().translationYBy(-500f).setDuration(400).setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    hole.setImageResource(R.drawable.ic_hole)
                    hole.scaleY = 1f; hole.translationY = 0f
                }
            }).start()
    }
    //endregion

    //region Fin del Nivel y UI Helpers
    private fun terminarBatalla() {
        SoundManager.stopMusic()
        moleJob?.cancel()
        timerJob?.cancel()

        val ptsDetect = aciertosDetect * PUNTOS_POR_ACIERTO_DETECT
        val ptsUrl = aciertosUrl * PUNTOS_POR_ACIERTO_URL
        val ptsBatalla = if (scoreBatalla >= SCORE_BATALLA_MAX_PUNTOS) PUNTOS_BATALLA_TOTAL else scoreBatalla * PUNTOS_BATALLA_MULTIPLICADOR
        puntuacionTotal = ptsDetect + ptsUrl + ptsBatalla

        if (puntuacionTotal >= SCORE_MIN_APROBADO) { // Si ha ganado
            // ¡¡AQUÍ ESTÁ LA LÍNEA CLAVE!!
            ProgresoManager.desbloquearSiguienteNivel(2)
            checkLevel2Achievements()
        }

        mostrarEtapa(Etapa.RECOMP)
        mostrarResultados()
    }

    private fun checkLevel2Achievements() {
        // Llamamos a la nueva función que incluye el context
        ProgresoManager.unlockAchievement(requireContext(), "NIVEL2_COMPLETADO")
        if (aciertosDetect == EJEMPLOS_DETECT.size && aciertosUrl == LISTA_URLS.size) {
            ProgresoManager.unlockAchievement(requireContext(), "NIVEL2_PERFECCIONISTA")
        }
        if (scoreBatalla > 15) {
            ProgresoManager.unlockAchievement(requireContext(), "NIVEL2_PESCADOR_PRO")
        }
    }

    private fun mostrarResultados() = binding.run {
        tvPuntuacionFinal.text = getString(R.string.nivel2_recompensa_puntuacion_final, puntuacionTotal, 100)

        val (titleId, subId, iconId, jingle) = when {
            puntuacionTotal < SCORE_MIN_APROBADO -> Result(R.string.nivel2_recompensa_titulo_derrota, R.string.nivel2_recompensa_subtitulo_mal, R.drawable.byte_triste, R.raw.music_defeat)
            puntuacionTotal < SCORE_NOTABLE -> Result(R.string.nivel2_recompensa_titulo_victoria, R.string.nivel2_recompensa_subtitulo_bien, R.drawable.byte_frame2, R.raw.music_victory)
            puntuacionTotal < SCORE_EXCELENTE -> Result(R.string.nivel2_recompensa_titulo_victoria, R.string.nivel2_recompensa_subtitulo_muy_bien, R.drawable.byte_frame3, R.raw.music_victory)
            else -> Result(R.string.nivel2_recompensa_titulo_victoria, R.string.nivel2_recompensa_subtitulo_perfecto, R.drawable.byte_frame1, R.raw.music_victory)
        }

        SoundManager.playJingle(requireContext(), jingle)

        val buttonText = if (puntuacionTotal < SCORE_MIN_APROBADO) R.string.common_button_reintentar else R.string.common_button_finalizar_nivel

        tvRecompensaTitulo.text = getString(titleId)
        tvRecompensaSubtitulo.text = getString(subId)
        imgConfeti.setImageResource(iconId)
        btnRecompContinuar.text = getString(buttonText)
        imgConfeti.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down))
    }

    private fun onFinalizarNivel() {
        if (puntuacionTotal >= SCORE_MIN_APROBADO) {
            findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        } else {
            reiniciarNivel()
        }
    }

    private fun reiniciarNivel(){
        puntuacionTotal = 0
        // También reiniciamos los contadores parciales
        aciertosDetect = 0
        aciertosUrl = 0
        scoreBatalla = 0
        mostrarEtapa(Etapa.INTRO)
    }

    private fun showInfo(titleRes: Int, msgRes: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(titleRes))
            .setMessage(getString(msgRes))
            .setPositiveButton(getString(R.string.common_dialog_entendido)) { d, _ -> d.dismiss() }
            .show()
    }
    //endregion

    //region Datos y Constantes del Nivel
    companion object {
        // Puntuaciones
        private const val PUNTOS_POR_ACIERTO_DETECT = 4
        private const val PUNTOS_POR_ACIERTO_URL = 5
        private const val PUNTOS_BATALLA_MULTIPLICADOR = 6
        private const val PUNTOS_BATALLA_TOTAL = 60
        private const val SCORE_BATALLA_MAX_PUNTOS = 10
        private const val SCORE_MIN_APROBADO = 50
        private const val SCORE_NOTABLE = 71
        private const val SCORE_EXCELENTE = 100

        // Configuración Batalla
        private const val DURACION_BATALLA_S = 30
        private const val DURACION_VISIBLE_MS = 800L
        private const val PAUSA_ENTRE_MOLES_MS = 200L
        private const val DURACION_APARECER_MS = 200L
        private const val DURACION_DESAPARECER_MS = 200L

        private val EJEMPLOS_DETECT = listOf(
            "Tu banco te envía un correo solicitando que confirmes tu PIN" to true,
            "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to true,
            "Email desde soporte técnico pidiendo tu contraseña" to true,
            "Notificación de tu UPS: tu paquete está en camino con enlace oficial" to false,
            "SMS de tu operadora con enlace raro y faltas de ortografía" to true
        )

        private val LISTA_URLS = listOf(
            "https://mi-banco.com/login" to true,
            "http://secure-mi-banco.net/confirmar" to false,
            "https://www.faceboook.com" to false,
            "https://support.google.com" to true
        )
    }
    //endregion
}
data class Result(
    val titleId: Int,
    val subId: Int,
    val iconId: Int,
    val jingle: Int
)