package com.ldm.ciberroyale.niveles

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import kotlinx.coroutines.launch
import kotlin.random.Random

class Nivel2Fragment : Fragment(R.layout.fragment_nivel2) {

    private var _binding: FragmentNivel2Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val VIDA_INICIAL_JUGADOR = 100
        private const val DURACION_BATALLA_MS = 30_000L   // 30 segundos
        private const val INTERVALO_TOPO_MS = 800L        // 0.8 s entre moles
    }

    private enum class Etapa { INTRO, DETECT, URL, BATALLA, RECOMPENSA, DERROTA }
    private var etapaActual: Etapa = Etapa.INTRO

    // Contadores de aciertos en DETECT y URL
    private var correctasDetect = 0
    private var correctasUrl = 0

    // Lista de ejemplos para DETECT (texto → esSeguro?)
    private val ejemplos = listOf(
        "Tu banco te envía un correo solicitando que confirmes tu PIN" to true,
        "Un mensaje de WhatsApp dice que has ganado un móvil, pide tus datos" to false,
        "Email desde soporte técnico pidiendo tu contraseña" to false,
        "Notificación de tu UPS: tu paquete está en camino con enlace oficial" to true,
        "SMS de tu operadora con enlace raro y faltas de ortografía" to false
    )
    private var idxMensaje = 0

    // Lista de URLs para URL (texto → esValida?)
    private val listaUrls = listOf(
        "https://mi-banco.com/login" to true,
        "http://secure-mi-banco.net/confirmar" to false,
        "https://www.faceboook.com" to false,
        "https://support.google.com" to true
    )
    private var idxUrl = 0

    // Estado de la batalla “Whack‐a‐Phishing”
    private var vidaJugador = VIDA_INICIAL_JUGADOR
    private var scoreBatalla = 0
    private var tiempoRestante = 30 // segundos
    private var moleIndex = -1      // índice del hoyo donde está actualmente el mole (-1 = ninguno)

    private lateinit var holes: List<ImageButton>

    private var batallaJob: Job? = null
    private var topoJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel2Binding.bind(view)
        setupListeners()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        batallaJob?.cancel()
        topoJob?.cancel()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // Botón Intro → pasar a DETECT
        btnIntroSiguiente.setOnClickListener {
            etapaActual = Etapa.DETECT
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // Botón “Continuar” en pantalla de recompensa → salir al menú
        btnRecompContinuar.setOnClickListener {
            findNavController().navigate(R.id.action_nivel2Fragment_to_juegoFragment)
        }

        // Botones de DERROTA: reintentar → BATALLA / salir
        btnDerrotaReintentar.setOnClickListener {
            etapaActual = Etapa.BATALLA
            mostrarEtapa(Etapa.BATALLA)
            iniciarBatalla()
        }
        btnDerrotaSalir.setOnClickListener {
            findNavController().popBackStack()
        }

        // Info Detect
        btnInfoDetect.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Cómo jugar (Detect)")
                .setMessage("Arrastra el mensaje al área “Seguro” si confías en él, o a “Phishing” si es sospechoso.")
                .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
                .show()
        }

        // Info URL
        btnInfoUrl.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Cómo jugar (URL)")
                .setMessage("Arrastra la URL al área “Válida” si confías en ella, o a “Inválida” si no es segura.")
                .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
                .show()
        }

        // Info Batalla
        btnInfoBatalla.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Cómo jugar (Whack‐a‐Phishing)")
                .setMessage("Durante 30 segundos, aparecerán “phishings” en distintos hoyos. Tócalos antes de que desaparezcan para obtener puntos.")
                .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
                .show()
        }
    }

    // ------------------------------------------------------------
    //                          DETECT
    // ------------------------------------------------------------
    private fun iniciarDetect() = with(binding) {
        idxMensaje = 0
        correctasDetect = 0
        tvMensaje.text = ejemplos[idxMensaje].first
        configurarDetectDragAndDrop()
    }

    private fun configurarDetectDragAndDrop() = with(binding) {
        // Al arrastrar el cardMensaje
        cardMensaje.setOnLongClickListener { v ->
            v.startDragAndDrop(
                ClipData.newPlainText("", ""),
                View.DragShadowBuilder(v),
                v,
                0
            )
            true
        }

        // Listener común para los targets Seguro/Phishing
        val listener = View.OnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENTERED -> {
                    v.setBackgroundColor(Color.LTGRAY)
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.setBackgroundResource(R.drawable.bg_drop_target)
                    true
                }
                DragEvent.ACTION_DROP -> {
                    handleDetectDrop(v.id)
                    true
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
    }

    private fun handleDetectDrop(targetId: Int) = with(binding) {
        val esSeguro = ejemplos[idxMensaje].second
        val respuestaUsuario = (targetId == binding.targetSeguro.id)
        if (respuestaUsuario == esSeguro) correctasDetect++
        Toast.makeText(
            requireContext(),
            if (respuestaUsuario == esSeguro) "¡Correcto!" else "Incorrecto…",
            Toast.LENGTH_SHORT
        ).show()

        idxMensaje++
        if (idxMensaje < ejemplos.size) {
            tvMensaje.text = ejemplos[idxMensaje].first
        } else {
            // Cambio a URL
            removerDetectDragAndDrop()
            etapaActual = Etapa.URL
            mostrarEtapa(Etapa.URL)
            iniciarUrl()
        }
    }

    // ------------------------------------------------------------
    //                           URL
    // ------------------------------------------------------------
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
                    v.alpha = 0.5f
                    true
                }
                DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                    v.alpha = 1f
                    true
                }
                DragEvent.ACTION_DROP -> {
                    handleUrlDrop(v.id)
                    true
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
    }

    private fun handleUrlDrop(targetId: Int) = with(binding) {
        val esValida = listaUrls[idxUrl].second
        val respuestaUsuario = (targetId == binding.targetValida.id)
        if (respuestaUsuario == esValida) correctasUrl++
        Toast.makeText(
            requireContext(),
            if (respuestaUsuario == esValida) "¡Correcto!" else "Incorrecto…",
            Toast.LENGTH_SHORT
        ).show()

        idxUrl++
        if (idxUrl < listaUrls.size) {
            tvUrl.text = listaUrls[idxUrl].first
        } else {
            // Cambio a BATALLA
            removerUrlDragAndDrop()
            etapaActual = Etapa.BATALLA
            mostrarEtapa(Etapa.BATALLA)
            iniciarBatalla()
        }
    }

    // ------------------------------------------------------------
    //                    BATALLA (“Whack‐a‐Phishing”)
    // ------------------------------------------------------------
    private fun iniciarBatalla() = with(binding) {
        // Reiniciar estado
        vidaJugador = VIDA_INICIAL_JUGADOR
        scoreBatalla = 0
        tiempoRestante = 30
        tvScore.text = "Score: $scoreBatalla"
        tvTimer.text = "Tiempo: $tiempoRestante"

        // Inicializar lista de hoyos
        holes = listOf(
            hoyo0, hoyo1, hoyo2,
            hoyo3, hoyo4, hoyo5,
            hoyo6, hoyo7, hoyo8,
            hoyo9, hoyo10, hoyo11
        )

        // Poner todos los hoyos en estado “vacío” y escala normal
        holes.forEachIndexed { index, hole ->
            hole.setImageResource(R.drawable.ic_hole)
            hole.scaleY = 1f
            hole.translationY = 0f
            hole.setOnClickListener {
                if (index == moleIndex) {
                    // Acertó al tocar el “mole”
                    scoreBatalla++
                    tvScore.text = "Score: $scoreBatalla"
                    disparaMole(hole)
                }
                // Si toca hoyo vacío → no penaliza, simplemente no hace nada
            }
        }

        // Cancelar jobs previos si existían
        batallaJob?.cancel()
        topoJob?.cancel()

        // Coroutine para hacer aparecer/desaparecer moles cada INTERVALO_TOPO_MS
        topoJob = viewLifecycleOwner.lifecycleScope.launch {
            moleIndex = -1
            while (tiempoRestante > 0) {
                // Si hay un mole visible → animarlo hacia abajo (pop‐down)
                if (moleIndex in holes.indices) {
                    val prevHole = holes[moleIndex]
                    prevHole.animate()
                        .scaleY(0f)
                        .setDuration(200)
                        .setInterpolator(AccelerateInterpolator())
                        .withEndAction {
                            prevHole.setImageResource(R.drawable.ic_hole)
                            prevHole.scaleY = 1f
                        }.start()
                }
                // Elegir un nuevo hoyo al azar
                moleIndex = Random.nextInt(holes.size)
                val nuevoHole = holes[moleIndex]

                // Cambiar a “mole” y hacerlo emerger (pop‐up)
                nuevoHole.setImageResource(R.drawable.viper)
                nuevoHole.scaleY = 0f
                nuevoHole.animate()
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                // Esperar un intervalo antes de la siguiente aparición
                delay(INTERVALO_TOPO_MS)
            }
        }

        // Coroutine para llevar el contador de tiempo
        batallaJob = viewLifecycleOwner.lifecycleScope.launch {
            while (tiempoRestante > 0) {
                delay(1000)
                tiempoRestante--
                tvTimer.text = "Tiempo: $tiempoRestante"
            }
            // Cuando el tiempo llega a 0, aseguramos que el “mole” desaparezca si está en pantalla
            if (moleIndex in holes.indices) {
                val lastHole = holes[moleIndex]
                lastHole.animate()
                    .scaleY(0f)
                    .setDuration(200)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction {
                        lastHole.setImageResource(R.drawable.ic_hole)
                        lastHole.scaleY = 1f
                    }.start()
            }
            // Terminamos batalla
            terminarBatalla()
        }
    }

    /**
     * Animación extra cuando el usuario acierta al tocar el mole:
     *  - El mole “sale disparado” hacia arriba (translationY negativo) en ~300 ms y luego desaparece.
     */
    private fun disparaMole(hole: ImageButton) {
        hole.animate()
            .translationY(-hole.height.toFloat()) // sube fuera
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    hole.setImageResource(R.drawable.ic_hole)
                    hole.translationY = 0f
                    moleIndex = -1
                }
            }).start()
    }

    private fun terminarBatalla() = with(binding) {
        // Cancelar cualquier job en curso
        topoJob?.cancel()
        batallaJob?.cancel()

        Toast.makeText(requireContext(), "¡Batalla terminada!", Toast.LENGTH_LONG).show()
        etapaActual = Etapa.RECOMPENSA
        mostrarEtapa(Etapa.RECOMPENSA)
        tvRecompensaSubtitulo.text = "Phishings derribados: $scoreBatalla"
    }

    // ------------------------------------------------------------
    //                MOSTRAR U OCULTAR PANTALLAS
    // ------------------------------------------------------------
    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        listOf(
            pantallaIntro,
            pantallaDetec,
            pantallaUrl,
            pantallaCombate,
            pantallaRecomp,
            pantallaDerrota
        ).forEach { it.isVisible = false }

        when (etapa) {
            Etapa.INTRO -> pantallaIntro.isVisible = true
            Etapa.DETECT -> pantallaDetec.isVisible = true
            Etapa.URL -> pantallaUrl.isVisible = true
            Etapa.BATALLA -> pantallaCombate.isVisible = true
            Etapa.RECOMPENSA -> pantallaRecomp.isVisible = true
            Etapa.DERROTA -> pantallaDerrota.isVisible = true
        }
    }
}
