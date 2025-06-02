package com.ldm.ciberroyale.niveles

import android.content.ClipData
import android.graphics.Color
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel2Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Nivel2Fragment revisado para corregir:
 * - Eliminación de métodos duplicados (handleUrlDrop, iniciarCombate).
 * - Fusión de la pantalla “Derrota” en el mismo binding (FragmentNivel2Binding).
 * - Uso de un único Binding para todas las pantallas.
 * - Inclusión de indicadores de aciertos (correctasDetect, correctasUrl, correctasCombate).
 * - Animaciones y feedback al arrastrar.
 * - Habilitación/deshabilitación de botones de combate.
 */
class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val VIDA_INICIAL_JUGADOR = 100
        private const val VIDA_INICIAL_ENEMIGO = 100
        private const val DANIO_FALLIDO = 15
        // Con 3 preguntas y 34 de daño cada acierto, el enemigo llega a 0
        private const val DANIO_ACIERTO = 34
    }

    // Fases de la pantalla
    private enum class Etapa { INTRO, DETECT, URL, COMBATE, RECOMPENSA, DERROTA }
    private var etapaActual: Etapa = Etapa.INTRO

    // Contadores de aciertos para estadísticas finales
    private var correctasDetect = 0
    private var correctasUrl = 0
    private var correctasCombate = 0

    // Modelo de pregunta de combate
    private data class CombateQuestion(val texto: String, val opciones: List<String>, val correcta: Int)

    // Datos para DETECT
    private val ejemplos = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to true,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña" to false,
        "Notificación de tu UPS: tu paquete está en camino con enlace oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private var idxMensaje = 0

    // Datos para URL
    private val listaUrls = listOf(
        "https://mi-banco.com/login" to true,
        "http://secure-mi-banco.net/confirmar" to false,
        "https://www.faceboook.com" to false,
        "https://support.google.com" to true
    )
    private var idxUrl = 0

    // Datos para COMBATE
    private val preguntasCombate = listOf(
        CombateQuestion(
            "Recibes un correo que pide tu contraseña. ¿Qué haces?",
            listOf("Hago clic y escribo", "Ignoro y reporto al soporte", "Reenvío a un amigo"),
            1
        ),
        CombateQuestion(
            "Un SMS de tu banco pide enviar tu PIN. ¿Acción correcta?",
            listOf("Responder con PIN", "Ignorar y llamar al oficial", "Guardar SMS"),
            1
        ),
        CombateQuestion(
            "Mensaje de Facebook pide contraseña. ¿Qué haces?",
            listOf("Confirmo contraseña", "Abrir app y revisar notifs", "Compartir con amigos"),
            1
        )
    )
    private var vidaJugador = VIDA_INICIAL_JUGADOR
    private var vidaEnemigo = VIDA_INICIAL_ENEMIGO
    private var idxCombate = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel2Binding.bind(view)
        setupListeners()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // INTRO → pasa a DETECT
        btnIntroSiguiente.setOnClickListener {
            etapaActual = Etapa.DETECT
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // BOTONES DE COMBATE
        listOf(btnOpcion0, btnOpcion1, btnOpcion2).forEachIndexed { index, btn ->
            btn.setOnClickListener { evaluarCombate(index) }
        }

        // RECOMPENSA → volver al menú o fragment general
        btnRecompContinuar.setOnClickListener {
            findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        }

        // DERROTA → reintentar o salir
        btnDerrotaReintentar.setOnClickListener {
            etapaActual = Etapa.DETECT
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }
        btnDerrotaSalir.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /** INICIA la fase DETECT */
    private fun iniciarDetect() = with(binding) {
        idxMensaje = 0
        correctasDetect = 0
        tvMensaje.text = ejemplos[idxMensaje].first
        configurarDetectDragAndDrop()
    }

    /** Configura Drag & Drop para DETECT */
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

        val detectListener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.setBackgroundColor(Color.LTGRAY)   // Feedback visual
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.setBackgroundResource(R.drawable.bg_drop_target)
                    return@OnDragListener true
                }
                DragEvent.ACTION_DROP -> {
                    handleDetectDrop(v.id)
                    return@OnDragListener true
                }
                else -> return@OnDragListener true
            }
        }
        targetSeguro.setOnDragListener(detectListener)
        targetPhishing.setOnDragListener(detectListener)
    }

    /** Elimina listeners de DETECT */
    private fun removerDetectDragAndDrop() = with(binding) {
        cardMensaje.setOnLongClickListener(null)
        targetSeguro.setOnDragListener(null)
        targetPhishing.setOnDragListener(null)
    }

    /** Se llama al soltar el mensaje en DETECT */
    private fun handleDetectDrop(targetId: Int) = with(binding) {
        val esCorrecto = ejemplos[idxMensaje].second
        val respuestaUsuario = (targetId == binding.targetSeguro.id)
        val explanation = when {
            !respuestaUsuario && esCorrecto -> "El mensaje es seguro."
            respuestaUsuario && !esCorrecto -> "El mensaje es phishing."
            else -> ""
        }
        if (respuestaUsuario == esCorrecto) {
            correctasDetect++
        }
        Toast.makeText(
            requireContext(),
            if (respuestaUsuario == esCorrecto) "¡Correcto!" else "Incorrecto… $explanation",
            Toast.LENGTH_SHORT
        ).show()

        idxMensaje++
        if (idxMensaje < ejemplos.size) {
            binding.tvMensaje.text = ejemplos[idxMensaje].first
        } else {
            Toast.makeText(requireContext(), "Fase DETECT completada", Toast.LENGTH_SHORT).show()
            removerDetectDragAndDrop()
            etapaActual = Etapa.URL
            mostrarEtapa(Etapa.URL)
            iniciarUrl()
        }
    }

    /** INICIA la fase URL */
    private fun iniciarUrl() = with(binding) {
        idxUrl = 0
        correctasUrl = 0
        tvUrl.text = listaUrls[idxUrl].first
        configurarUrlDragAndDrop()
    }

    /** Configura Drag & Drop para URL */
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

        val urlListener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.alpha = 0.5f   // Feedback visual
                    return@OnDragListener true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1f
                    return@OnDragListener true
                }
                DragEvent.ACTION_DROP -> {
                    handleUrlDrop(v.id)
                    return@OnDragListener true
                }
                else -> return@OnDragListener true
            }
        }
        targetValida.setOnDragListener(urlListener)
        targetInvalida.setOnDragListener(urlListener)
    }

    /** Elimina listeners de URL */
    private fun removerUrlDragAndDrop() = with(binding) {
        cardUrl.setOnLongClickListener(null)
        targetValida.setOnDragListener(null)
        targetInvalida.setOnDragListener(null)
    }

    /** Se llama al soltar la URL en URL */
    private fun handleUrlDrop(targetId: Int) = with(binding) {
        val esCorrecta = listaUrls[idxUrl].second
        val respuestaUsuario = (targetId == binding.targetValida.id)
        val url = listaUrls[idxUrl].first
        val urlInfo = when {
            url.startsWith("http://") -> "La URL no es segura (HTTP)."
            url.contains("faceboook.com") -> "La URL tiene un dominio sospechoso."
            else -> ""
        }
        if (respuestaUsuario == esCorrecta) {
            correctasUrl++
        }
        Toast.makeText(
            requireContext(),
            if (respuestaUsuario == esCorrecta) "¡Correcto!" else "Incorrecto… $urlInfo",
            Toast.LENGTH_SHORT
        ).show()

        idxUrl++
        if (idxUrl < listaUrls.size) {
            binding.tvUrl.text = listaUrls[idxUrl].first
        } else {
            Toast.makeText(requireContext(), "Fase URL completada", Toast.LENGTH_SHORT).show()
            removerUrlDragAndDrop()
            etapaActual = Etapa.COMBATE
            mostrarEtapa(Etapa.COMBATE)
            iniciarCombate()
        }
    }

    /** INICIA la fase COMBATE */
    private fun iniciarCombate() = with(binding) {
        // Reiniciar variables
        vidaJugador = VIDA_INICIAL_JUGADOR
        vidaEnemigo = VIDA_INICIAL_ENEMIGO
        idxCombate = 0
        correctasCombate = 0

        pbVidaJugador.progress = vidaJugador
        pbVidaEnemigo.progress = vidaEnemigo

        cargarPreguntaCombate()
    }

    /** Carga en pantalla la pregunta de combate actual */
    private fun cargarPreguntaCombate() = with(binding) {
        val q = preguntasCombate[idxCombate]
        tvCombatePregunta.text = "Pregunta ${idxCombate + 1} de ${preguntasCombate.size}: ${q.texto}"
        btnOpcion0.text = q.opciones[0]
        btnOpcion1.text = q.opciones[1]
        btnOpcion2.text = q.opciones[2]

        // Habilitar botones en cada nueva pregunta
        listOf(btnOpcion0, btnOpcion1, btnOpcion2).forEach { it.isEnabled = true }
    }

    /** Se ejecuta al pulsar una opción de combate */
    private fun evaluarCombate(seleccion: Int) = with(binding) {
        val correcta = preguntasCombate[idxCombate].correcta
        if (seleccion == correcta) {
            vidaEnemigo = (vidaEnemigo - DANIO_ACIERTO).coerceAtLeast(0)
            Toast.makeText(context, "¡Buen ataque! -$DANIO_ACIERTO HP", Toast.LENGTH_SHORT).show()
            correctasCombate++
        } else {
            vidaJugador = (vidaJugador - DANIO_FALLIDO).coerceAtLeast(0)
            Toast.makeText(context, "Fallaste… -$DANIO_FALLIDO HP", Toast.LENGTH_SHORT).show()
        }
        pbVidaEnemigo.progress = vidaEnemigo
        pbVidaJugador.progress = vidaJugador

        // Deshabilitar opciones durante el delay
        listOf(btnOpcion0, btnOpcion1, btnOpcion2).forEach { it.isEnabled = false }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            // Si murió el jugador o ya se respondieron todas las preguntas → terminar
            if (vidaJugador == 0 || idxCombate + 1 >= preguntasCombate.size) {
                terminarCombate()
            } else {
                idxCombate++
                cargarPreguntaCombate()
            }
        }
    }

    /** Final del combate: muestra recompensa o pantalla de derrota */
    private fun terminarCombate() = with(binding) {
        val vencioEnemigo = (vidaEnemigo == 0) || (idxCombate + 1 >= preguntasCombate.size && vidaJugador > 0)
        if (vencioEnemigo) {
            Toast.makeText(context, "¡Has derrotado al secuaz!", Toast.LENGTH_LONG).show()
            etapaActual = Etapa.RECOMPENSA
            mostrarEtapa(Etapa.RECOMPENSA)
        } else {
            etapaActual = Etapa.DERROTA
            mostrarEtapa(Etapa.DERROTA)
        }
    }

    /** Muestra únicamente la pantalla que corresponda según la etapa */
    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        // Ocultar todas
        listOf(
            pantallaIntro,
            pantallaDetec,
            pantallaUrl,
            pantallaCombate,
            pantallaRecomp,
            pantallaDerrota
        ).forEach { it.isVisible = false }

        // Mostrar la solicitada
        when (etapa) {
            Etapa.INTRO -> {
                pantallaIntro.isVisible = true
                // Animación de fade‐in al aparecer
                val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                pantallaIntro.startAnimation(fadeIn)
            }
            Etapa.DETECT -> pantallaDetec.isVisible = true
            Etapa.URL -> pantallaUrl.isVisible = true
            Etapa.COMBATE -> pantallaCombate.isVisible = true
            Etapa.RECOMPENSA -> {
                pantallaRecomp.isVisible = true
                // Mostrar estadísticas finales
                tvRecompensaSubtitulo.text = """
                    Has superado el nivel de phishing
                    Mensajes correctos: $correctasDetect / ${ejemplos.size}
                    URLs correctas: $correctasUrl / ${listaUrls.size}
                    Preguntas de combate: $correctasCombate / ${preguntasCombate.size}
                """.trimIndent()
            }
            Etapa.DERROTA -> pantallaDerrota.isVisible = true
        }
    }
}
