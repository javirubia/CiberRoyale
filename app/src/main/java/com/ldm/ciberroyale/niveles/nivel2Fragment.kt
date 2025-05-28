package com.ldm.ciberroyale.niveles

import android.content.ClipData
import android.os.Bundle
import android.view.DragEvent
import android.view.View
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

class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val VIDA_INICIAL_JUGADOR = 100
        private const val VIDA_INICIAL_ENEMIGO = 100
        private const val DANIO_FALLIDO = 15
        private const val DANIO_ACIERTO = 20
    }

    // Fases de la pantalla
    private enum class Etapa { INTRO, DETECT, URL, COMBATE, RECOMPENSA }

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
        // INTRO
        btnIntroSiguiente.setOnClickListener {
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // DETECT drag & drop
        cardMensaje.setDragFor(
            onStart = { view -> view.startDragAndDrop(ClipData.newPlainText("", ""),
                View.DragShadowBuilder(view), view, 0) },
            onDrop = ::handleDetectDrop
        )

        // URL drag & drop
        cardUrl.setDragFor(
            onStart = { view -> view.startDragAndDrop(ClipData.newPlainText("", ""),
                View.DragShadowBuilder(view), view, 0) },
            onDrop = ::handleUrlDrop
        )

        // COMBATE
        listOf(btnOpcion0, btnOpcion1, btnOpcion2).forEachIndexed { index, btn ->
            btn.setOnClickListener { evaluarCombate(index) }
        }

        // RECOMPENSA
        btnRecompContinuar.setOnClickListener {
            findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        }
    }

    private fun iniciarDetect() {
        idxMensaje = 0
        binding.tvMensaje.text = ejemplos[idxMensaje].first
    }

    private fun handleDetectDrop(targetId: Int) {
        val correcto = ejemplos[idxMensaje].second
        val esSeguro = targetId == binding.targetSeguro.id
        Toast.makeText(requireContext(), if (esSeguro == correcto) "¡Correcto!" else "Incorrecto…", Toast.LENGTH_SHORT).show()
        idxMensaje++
        if (idxMensaje < ejemplos.size) binding.tvMensaje.text = ejemplos[idxMensaje].first
        else {
            Toast.makeText(requireContext(), "Fase DETECT completada", Toast.LENGTH_SHORT).show()
            mostrarEtapa(Etapa.URL)
            iniciarUrl()
        }
    }

    private fun iniciarUrl() {
        idxUrl = 0
        binding.tvUrl.text = listaUrls[idxUrl].first
    }

    private fun handleUrlDrop(targetId: Int) {
        val correcto = listaUrls[idxUrl].second
        val esValida = targetId == binding.targetValida.id
        Toast.makeText(requireContext(), if (esValida == correcto) "¡Correcto!" else "Incorrecto…", Toast.LENGTH_SHORT).show()
        idxUrl++
        if (idxUrl < listaUrls.size) binding.tvUrl.text = listaUrls[idxUrl].first
        else {
            Toast.makeText(requireContext(), "Fase URL completada", Toast.LENGTH_SHORT).show()
            mostrarEtapa(Etapa.COMBATE)
            iniciarCombate()
        }
    }

    private fun iniciarCombate() = with(binding) {
        vidaJugador = VIDA_INICIAL_JUGADOR
        vidaEnemigo = VIDA_INICIAL_ENEMIGO
        idxCombate = 0
        pbVidaJugador.progress = vidaJugador
        pbVidaEnemigo.progress = vidaEnemigo
        cargarPreguntaCombate()
    }

    private fun cargarPreguntaCombate() = with(binding) {
        val q = preguntasCombate[idxCombate]
        tvCombatePregunta.text = q.texto
        btnOpcion0.text = q.opciones[0]
        btnOpcion1.text = q.opciones[1]
        btnOpcion2.text = q.opciones[2]
    }

    private fun evaluarCombate(seleccion: Int) {
        val correcta = preguntasCombate[idxCombate].correcta
        if (seleccion == correcta) {
            vidaEnemigo = (vidaEnemigo - DANIO_ACIERTO).coerceAtLeast(0)
            Toast.makeText(context, "¡Buen ataque! -$DANIO_ACIERTO HP", Toast.LENGTH_SHORT).show()
        } else {
            vidaJugador = (vidaJugador - DANIO_FALLIDO).coerceAtLeast(0)
            Toast.makeText(context, "Fallaste… -$DANIO_FALLIDO HP", Toast.LENGTH_SHORT).show()
        }
        binding.pbVidaEnemigo.progress = vidaEnemigo
        binding.pbVidaJugador.progress = vidaJugador

        lifecycleScope.launch {
            delay(500)
            if (vidaJugador == 0 || idxCombate + 1 >= preguntasCombate.size) {
                terminarCombate()
            } else {
                idxCombate++
                cargarPreguntaCombate()
            }
        }
    }

    private fun terminarCombate() {
        if (vidaEnemigo == 0) {
            Toast.makeText(context, "¡Has derrotado al secuaz!", Toast.LENGTH_LONG).show()
            mostrarEtapa(Etapa.RECOMPENSA)
        } else {
            Toast.makeText(context, "Has sido derrotado… inténtalo otra vez.", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        listOf(pantallaIntro, pantallaDetec, pantallaUrl, pantallaCombate, pantallaRecomp)
            .forEach { it.isVisible = false }
        when (etapa) {
            Etapa.INTRO -> pantallaIntro.isVisible = true
            Etapa.DETECT -> pantallaDetec.isVisible = true
            Etapa.URL -> pantallaUrl.isVisible = true
            Etapa.COMBATE -> pantallaCombate.isVisible = true
            Etapa.RECOMPENSA -> pantallaRecomp.isVisible = true
        }
    }

    // Extensión para drag & drop genérico
    private fun CardView.setDragFor(
        onStart: (View) -> Unit,
        onDrop: (targetId: Int) -> Unit
    ) {
        setOnLongClickListener { view -> onStart(view); true }
        val listener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) onDrop(v.id)
            true
        }
        binding.targetSeguro.setOnDragListener(listener)
        binding.targetPhishing.setOnDragListener(listener)
        binding.targetValida.setOnDragListener(listener)
        binding.targetInvalida.setOnDragListener(listener)
    }
}
