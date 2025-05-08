package com.ldm.ciberroyale

import android.content.ClipData
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    // Etapas del flujo
    private enum class Etapa { INTRO, DETECT, URL, COMBATE, RECOMPENSA }

    // Data class para preguntas de combate
    private data class CombateQuestion(
        val texto: String,
        val opciones: List<String>,
        val correcta: Int
    )

    // Vistas principales
    private lateinit var pantallaIntro   : View
    private lateinit var pantallaDetec   : View
    private lateinit var pantallaUrl     : View
    private lateinit var pantallaCombate : View
    private lateinit var pantallaRecomp  : View

    // INTRO
    private lateinit var btnIntroSiguiente: Button

    // Fase DETECT (clasifica mensaje)
    private lateinit var cardMensaje    : CardView
    private lateinit var tvMensaje      : TextView
    private lateinit var targetSeguro   : View
    private lateinit var targetPhishing : View
    private val ejemplos = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to true,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña para actualizar sistema" to false,
        "Notificación de tu UPS: tu paquete está en camino. Con enlace a tracking oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private var idxMensaje = 0

    // Fase URL (clasifica URL)
    private lateinit var cardUrl        : CardView
    private lateinit var tvUrl          : TextView
    private lateinit var targetValida   : View
    private lateinit var targetInvalida : View
    private val listaUrls = listOf(
        "https://mi-banco.com/login" to true,
        "http://secure-mi-banco.net/confirmar" to false,
        "https://www.faceboook.com" to false,
        "https://support.google.com" to true
    )
    private var idxUrl = 0

    // Fase COMBATE (Q&A RPG)
    private lateinit var pbVidaJugador : ProgressBar
    private lateinit var pbVidaEnemigo : ProgressBar
    private lateinit var tvCombatePreg : TextView
    private lateinit var btnOpcion0    : Button
    private lateinit var btnOpcion1    : Button
    private lateinit var btnOpcion2    : Button

    private var vidaJugador = 100
    private var vidaEnemigo = 100
    private var idxCombate  = 0

    // Preguntas de combate
    private val preguntasCombate = listOf(
        CombateQuestion(
            "Recibes un correo que te pide confirmar tu contraseña haciendo clic en un enlace. ¿Qué haces?",
            listOf("Hago clic y escribo mi contraseña", "Ignoro y reporto al soporte", "Reenvío a un amigo"),
            1
        ),
        CombateQuestion(
            "Un SMS de tu banco te pide enviar tu PIN. ¿Cuál es la mejor acción?",
            listOf("Responder con mi PIN", "Ignorar y llamar al número oficial", "Guardar el SMS"),
            1
        ),
        CombateQuestion(
            "Recibes un mensaje de Facebook que dice que alguien accedió a tu cuenta. Te pide tu contraseña. ¿Qué haces?",
            listOf("Confirmo mi contraseña", "Voy a la app de Facebook directamente y reviso notificaciones", "Compartir mensaje con mis amigos"),
            1
        )
    )

    // Fase RECOMPENSA
    private lateinit var btnRecompContinuar: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind pantallas
        pantallaIntro   = view.findViewById(R.id.pantalla_intro)
        pantallaDetec   = view.findViewById(R.id.pantalla_detec)
        pantallaUrl     = view.findViewById(R.id.pantalla_url)
        pantallaCombate = view.findViewById(R.id.pantalla_combate)
        pantallaRecomp  = view.findViewById(R.id.pantalla_recomp)

        // INTRO
        btnIntroSiguiente = view.findViewById(R.id.btnIntroSiguiente)
        btnIntroSiguiente.setOnClickListener {
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // DETECT
        cardMensaje    = view.findViewById(R.id.cardMensaje)
        tvMensaje      = view.findViewById(R.id.tvMensaje)
        targetSeguro   = view.findViewById(R.id.targetSeguro)
        targetPhishing = view.findViewById(R.id.targetPhishing)
        cardMensaje.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val detectListener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) handleDetectDrop(v.id)
            true
        }
        targetSeguro.setOnDragListener(detectListener)
        targetPhishing.setOnDragListener(detectListener)

        // URL
        cardUrl        = view.findViewById(R.id.cardUrl)
        tvUrl          = view.findViewById(R.id.tvUrl)
        targetValida   = view.findViewById(R.id.targetValida)
        targetInvalida = view.findViewById(R.id.targetInvalida)
        cardUrl.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val urlListener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) handleUrlDrop(v.id)
            true
        }
        targetValida.setOnDragListener(urlListener)
        targetInvalida.setOnDragListener(urlListener)

        // COMBATE
        pbVidaJugador = view.findViewById(R.id.pbVidaJugador)
        pbVidaEnemigo = view.findViewById(R.id.pbVidaEnemigo)
        tvCombatePreg = view.findViewById(R.id.tvCombatePregunta)
        btnOpcion0    = view.findViewById(R.id.btnOpcion0)
        btnOpcion1    = view.findViewById(R.id.btnOpcion1)
        btnOpcion2    = view.findViewById(R.id.btnOpcion2)
        val combateListener = View.OnClickListener { btn ->
            val opcion = when (btn.id) {
                R.id.btnOpcion0 -> 0
                R.id.btnOpcion1 -> 1
                else            -> 2
            }
            evaluarCombate(opcion)
        }
        btnOpcion0.setOnClickListener(combateListener)
        btnOpcion1.setOnClickListener(combateListener)
        btnOpcion2.setOnClickListener(combateListener)

        // RECOMPENSA
        btnRecompContinuar = view.findViewById(R.id.btnRecompContinuar)
        btnRecompContinuar.setOnClickListener {
            findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        }

        // Mostrar pantalla inicial
        mostrarEtapa(Etapa.INTRO)
    }

    // DETECT handlers
    private fun iniciarDetect() {
        idxMensaje = 0
        tvMensaje.text = ejemplos[idxMensaje].first
    }

    private fun handleDetectDrop(targetId: Int) {
        val correctoId = if (ejemplos[idxMensaje].second) R.id.targetSeguro else R.id.targetPhishing
        Toast.makeText(requireContext(), if (targetId == correctoId) "¡Correcto!" else "Incorrecto…", Toast.LENGTH_SHORT).show()
        idxMensaje++
        if (idxMensaje < ejemplos.size) tvMensaje.text = ejemplos[idxMensaje].first
        else {
            Toast.makeText(requireContext(), "Fase DETECT completada", Toast.LENGTH_SHORT).show()
            mostrarEtapa(Etapa.URL)
            iniciarUrl()
        }
    }

    // URL handlers
    private fun iniciarUrl() {
        idxUrl = 0
        tvUrl.text = listaUrls[idxUrl].first
    }

    private fun handleUrlDrop(targetId: Int) {
        val correctoId = if (listaUrls[idxUrl].second) R.id.targetValida else R.id.targetInvalida
        Toast.makeText(requireContext(), if (targetId == correctoId) "¡Correcto!" else "Incorrecto…", Toast.LENGTH_SHORT).show()
        idxUrl++
        if (idxUrl < listaUrls.size) tvUrl.text = listaUrls[idxUrl].first
        else {
            Toast.makeText(requireContext(), "Fase URL completada", Toast.LENGTH_SHORT).show()
            mostrarEtapa(Etapa.COMBATE)
            iniciarCombate()
        }
    }

    // COMBATE logic
    private fun iniciarCombate() {
        vidaJugador = 100; vidaEnemigo = 100; idxCombate = 0
        pbVidaJugador.progress = vidaJugador
        pbVidaEnemigo.progress = vidaEnemigo
        cargarPreguntaCombate()
    }

    private fun cargarPreguntaCombate() {
        val q = preguntasCombate[idxCombate]
        tvCombatePreg.text = q.texto
        btnOpcion0.text = q.opciones[0]
        btnOpcion1.text = q.opciones[1]
        btnOpcion2.text = q.opciones[2]
    }

    private fun evaluarCombate(seleccion: Int) {
        val q = preguntasCombate[idxCombate]
        if (seleccion == q.correcta) {
            vidaEnemigo = (vidaEnemigo - 20).coerceAtLeast(0)
            Toast.makeText(context, "¡Buen ataque! -20 HP", Toast.LENGTH_SHORT).show()
        } else {
            vidaJugador = (vidaJugador - 15).coerceAtLeast(0)
            Toast.makeText(context, "Fallaste… -15 HP", Toast.LENGTH_SHORT).show()
        }
        idxCombate++
        pbVidaEnemigo.progress = vidaEnemigo
        pbVidaJugador.progress = vidaJugador
        Handler(Looper.getMainLooper()).postDelayed({
            if (vidaJugador == 0 || idxCombate >= preguntasCombate.size) terminarCombate()
            else cargarPreguntaCombate()
        }, 500)
    }

    private fun terminarCombate() {
        if (vidaEnemigo == 0) {
            Toast.makeText(context, "¡Has derrotado al secuaz!", Toast.LENGTH_LONG).show()
            mostrarEtapa(Etapa.RECOMPENSA)
        } else {
            Toast.makeText(context, "Has sido derrotado… inténtalo otra vez.", Toast.LENGTH_LONG).show()
        }
    }

    // Muestra solo la pantalla actual
    private fun mostrarEtapa(etapa: Etapa) {
        pantallaIntro.visibility   = View.GONE
        pantallaDetec.visibility   = View.GONE
        pantallaUrl.visibility     = View.GONE
        pantallaCombate.visibility = View.GONE
        pantallaRecomp.visibility  = View.GONE

        when (etapa) {
            Etapa.INTRO      -> pantallaIntro.visibility   = View.VISIBLE
            Etapa.DETECT     -> pantallaDetec.visibility   = View.VISIBLE
            Etapa.URL        -> pantallaUrl.visibility     = View.VISIBLE
            Etapa.COMBATE    -> pantallaCombate.visibility = View.VISIBLE
            Etapa.RECOMPENSA -> pantallaRecomp.visibility  = View.VISIBLE
        }
    }
}
