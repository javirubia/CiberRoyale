package com.ldm.ciberroyale

import android.content.ClipData
import android.os.Bundle
import android.os.CountDownTimer
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial

class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    // Etapas de este nivel
    private enum class Etapa {
        INTRO,
        DETECT_OVERSHARING,
        AJUSTES,
        FILTRAR_SOLICITUDES
        /* luego COMBATE, RECOMPENSA */
    }

    // --- Modelo Detecta oversharing ---
    private data class Post(
        val texto: String,
        val imagenRes: Int?,
        val esPublico: Boolean,
        val explicacion: String
    )
    private val posts = listOf(
        Post(
            "Foto de mis vacaciones con geolocalización",
            R.drawable.ic_heroe,
            false,
            "Incluye geolocalización en los metadatos de la imagen."
        ),
        Post(
            "Foto de mi mascota jugando",
            R.drawable.ic_heroe,
            true,
            "No revela información personal ni ubicación."
        ),
        Post(
            "Texto: Mi hermana cumple hoy 10 años y está sola en casa",
            null,
            false,
            "Revela ubicación y situación familiar vulnerable."
        ),
        Post(
            "Publicación: Fui al parque y fue muy divertido",
            null,
            true,
            "No contiene datos sensibles."
        )
    )

    // --- Modelo Ajusta tu privacidad ---
    private val ajustesEsperados = mapOf(
        "Fotos"     to false,
        "Ubicacion" to false,
        "Lista"     to true
    )
    private val explicacionesAjustes = mapOf(
        "Fotos"     to "Si dejas las fotos públicas, cualquier persona verá tus imágenes sin control.",
        "Ubicacion" to "Compartir ubicación en tiempo real puede poner en riesgo tu privacidad.",
        "Lista"     to "Mostrar tu lista de amigos ayuda a verificar solicitudes de personas conocidas."
    )

    // --- Modelo Filtra solicitudes ---
    private data class Solicitud(
        val nombre: String,
        val avatarRes: Int,
        val esConocido: Boolean
    )
    private val solicitudes = listOf(
        Solicitud("Ana Pérez", R.drawable.ic_heroe, true),
        Solicitud("Carlos Gómez", R.drawable.ic_heroe, false),
        Solicitud("María Ruiz", R.drawable.ic_heroe, true),
        Solicitud("Desconocido123", R.drawable.ic_actor, false)
    )

    // --- Vistas ---
    private lateinit var pantallaIntro: View
    private lateinit var pantallaDetect: View
    private lateinit var pantallaAjustes: View
    private lateinit var pantallaFiltrar: View

    // Intro
    private lateinit var btnIntro: Button

    // Detect oversharing
    private lateinit var cardPost: CardView
    private lateinit var imgPost: ImageView
    private lateinit var tvPost: TextView
    private lateinit var targetPublico: View
    private lateinit var targetPrivado: View
    private var idxPost = 0

    // Ajusta tu privacidad
    private lateinit var switchFotos: SwitchMaterial
    private lateinit var switchUbicacion: SwitchMaterial
    private lateinit var switchLista: SwitchMaterial
    private lateinit var btnInfoFotos: ImageView
    private lateinit var btnInfoUbicacion: ImageView
    private lateinit var btnInfoLista: ImageView
    private lateinit var btnVerificarAjustes: Button

    // Filtra solicitudes
    private lateinit var rvSolicitudes: RecyclerView
    private lateinit var pbTimer: ProgressBar
    private lateinit var btnFinalizar: Button
    private var idxSolicitud = 0
    private var puntosFiltrar = 0
    private lateinit var timer: CountDownTimer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind pantallas
        pantallaIntro   = view.findViewById(R.id.pantalla_intro)
        pantallaDetect  = view.findViewById(R.id.pantalla_detect_oversharing)
        pantallaAjustes = view.findViewById(R.id.pantalla_ajustes)
        pantallaFiltrar = view.findViewById(R.id.pantalla_filtrar_solicitudes)

        // INTRO
        btnIntro = view.findViewById(R.id.btnIntro3Siguiente)
        btnIntro.setOnClickListener {
            mostrarEtapa(Etapa.DETECT_OVERSHARING)
            iniciarDetect()
        }

        // DETECT OVERSHARING
        cardPost      = view.findViewById(R.id.cardPost)
        imgPost       = view.findViewById(R.id.imgPost)
        tvPost        = view.findViewById(R.id.tvPost)
        targetPublico = view.findViewById(R.id.targetPublico)
        targetPrivado = view.findViewById(R.id.targetPrivado)

        cardPost.setOnLongClickListener { v ->
            v.startDragAndDrop(
                ClipData.newPlainText("", ""),
                View.DragShadowBuilder(v),
                v,
                0
            )
            true
        }
        val detectListener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) handleDetectDrop(v.id)
            true
        }
        targetPublico.setOnDragListener(detectListener)
        targetPrivado.setOnDragListener(detectListener)

        // AJUSTES
        switchFotos         = view.findViewById(R.id.switchFotos)
        switchUbicacion     = view.findViewById(R.id.switchUbicacion)
        switchLista         = view.findViewById(R.id.switchLista)
        btnInfoFotos        = view.findViewById(R.id.btnInfoFotos)
        btnInfoUbicacion    = view.findViewById(R.id.btnInfoUbicacion)
        btnInfoLista        = view.findViewById(R.id.btnInfoLista)
        btnVerificarAjustes = view.findViewById(R.id.btnVerificarAjustes)

        btnInfoFotos.setOnClickListener {
            mostrarDialogo("Fotos", explicacionesAjustes["Fotos"]!!)
        }
        btnInfoUbicacion.setOnClickListener {
            mostrarDialogo("Ubicación", explicacionesAjustes["Ubicacion"]!!)
        }
        btnInfoLista.setOnClickListener {
            mostrarDialogo("Lista de amigos", explicacionesAjustes["Lista"]!!)
        }
        btnVerificarAjustes.setOnClickListener {
            evaluarAjustes()
        }

        // FILTRAR SOLICITUDES
        rvSolicitudes = view.findViewById(R.id.rvSolicitudes)
        pbTimer       = view.findViewById(R.id.pbTimerFiltrar)
        btnFinalizar  = view.findViewById(R.id.btnFinalizarFiltrar)

        rvSolicitudes.setHasFixedSize(true)

        rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        rvSolicitudes.adapter = SolicitudAdapter(solicitudes) { aceptada ->
            evaluarSolicitud(aceptada)
        }
        btnFinalizar.setOnClickListener {
            timer.cancel()
            Snackbar.make(
                requireView(),
                "Filtrado: $puntosFiltrar/${solicitudes.size} correctas",
                Snackbar.LENGTH_LONG
            ).setAction("Continuar") {
                // mostrarEtapa(Etapa.COMBATE); iniciarCombate()
            }.show()
        }

        // Arrancamos en INTRO
        mostrarEtapa(Etapa.INTRO)
    }

    // --- Detect oversharing ---
    private fun iniciarDetect() {
        idxPost = 0
        mostrarPost()
    }

    private fun mostrarPost() {
        val post = posts[idxPost]
        tvPost.text = post.texto
        if (post.imagenRes != null) {
            imgPost.visibility = View.VISIBLE
            imgPost.setImageResource(post.imagenRes)
        } else {
            imgPost.visibility = View.GONE
        }
    }

    private fun handleDetectDrop(targetId: Int) {
        val post       = posts[idxPost]
        val correctoId = if (post.esPublico) R.id.targetPublico else R.id.targetPrivado
        val acierto    = (targetId == correctoId)

        Snackbar.make(
            requireView(),
            if (acierto) "¡Correcto!" else "Incorrecto…",
            Snackbar.LENGTH_SHORT
        ).setAction("¿Por qué?") {
            mostrarDialogo("Explicación", post.explicacion)
        }.show()

        idxPost++
        if (idxPost < posts.size) {
            mostrarPost()
        } else {
            Snackbar.make(requireView(), "Fase oversharing completada", Snackbar.LENGTH_LONG).show()
            mostrarEtapa(Etapa.AJUSTES)
            iniciarAjustes()
        }
    }

    // --- Ajusta tu privacidad ---
    private fun iniciarAjustes() {
        switchFotos.isChecked     = !ajustesEsperados["Fotos"]!!
        switchUbicacion.isChecked = !ajustesEsperados["Ubicacion"]!!
        switchLista.isChecked     = !ajustesEsperados["Lista"]!!
        mostrarEtapa(Etapa.AJUSTES)
    }

    private fun evaluarAjustes() {
        val okFotos     = switchFotos.isChecked     == ajustesEsperados["Fotos"]
        val okUbicacion = switchUbicacion.isChecked == ajustesEsperados["Ubicacion"]
        val okLista     = switchLista.isChecked     == ajustesEsperados["Lista"]
        val todoBien    = listOf(okFotos, okUbicacion, okLista).all { it }

        Snackbar.make(
            requireView(),
            if (todoBien) "¡Muy bien!" else "Corrige los ajustes…",
            Snackbar.LENGTH_LONG
        ).setAction(if (todoBien) "Continuar" else "¿Por qué?") {
            if (todoBien) {
                mostrarEtapa(Etapa.FILTRAR_SOLICITUDES)
                iniciarFiltrar()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Explicaciones")
                    .setMessage(buildString {
                        if (!okFotos)     append("• Fotos: ${explicacionesAjustes["Fotos"]}\n\n")
                        if (!okUbicacion) append("• Ubicación: ${explicacionesAjustes["Ubicacion"]}\n\n")
                        if (!okLista)     append("• Lista: ${explicacionesAjustes["Lista"]}")
                    })
                    .setPositiveButton("OK", null)
                    .show()
            }
        }.show()
    }

    // --- Filtra solicitudes ---
    private fun iniciarFiltrar() {
        idxSolicitud = 0
        puntosFiltrar = 0
        btnFinalizar.isEnabled = false

        timer = object : CountDownTimer(30000, 30) {
            override fun onTick(millisUntilFinished: Long) {
                pbTimer.progress = (millisUntilFinished * 100 / 3000).toInt()
            }
            override fun onFinish() {
                pbTimer.progress = 0
                btnFinalizar.isEnabled = true
            }
        }.start()

        mostrarEtapa(Etapa.FILTRAR_SOLICITUDES)
    }

    private fun evaluarSolicitud(aceptada: Boolean) {
        val sol = solicitudes[idxSolicitud]
        val correcta = (aceptada == sol.esConocido)
        if (correcta) puntosFiltrar++

        Snackbar.make(
            requireView(),
            if (correcta) "¡Correcto!" else "Incorrecto",
            Snackbar.LENGTH_SHORT
        ).show()

        idxSolicitud++
        if (idxSolicitud >= solicitudes.size) {
            btnFinalizar.isEnabled = true
            timer.cancel()
        } else {
            rvSolicitudes.adapter?.notifyItemChanged(idxSolicitud - 1)
            rvSolicitudes.scrollToPosition(idxSolicitud)
        }
    }

    // --- Helpers ---
    private fun mostrarDialogo(titulo: String, mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun mostrarEtapa(etapa: Etapa) {
        pantallaIntro.visibility   = View.GONE
        pantallaDetect.visibility  = View.GONE
        pantallaAjustes.visibility = View.GONE
        pantallaFiltrar.visibility = View.GONE

        when (etapa) {
            Etapa.INTRO               -> pantallaIntro.visibility = View.VISIBLE
            Etapa.DETECT_OVERSHARING  -> pantallaDetect.visibility = View.VISIBLE
            Etapa.AJUSTES            -> pantallaAjustes.visibility = View.VISIBLE
            Etapa.FILTRAR_SOLICITUDES -> pantallaFiltrar.visibility = View.VISIBLE
        }
    }

    // --- Adapter interno para Filtrar solicitudes ---
    private inner class SolicitudAdapter(
        val items: List<Solicitud>,
        val callback: (Boolean) -> Unit
    ) : RecyclerView.Adapter<SolicitudAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            private val tvNombre: TextView  = view.findViewById(R.id.tvNombre)
            private val btnAceptar: Button  = view.findViewById(R.id.btnAceptar)
            private val btnRechazar: Button = view.findViewById(R.id.btnRechazar)

            fun bind(sol: Solicitud) {
                ivAvatar.setImageResource(sol.avatarRes)
                tvNombre.text = sol.nombre
                btnAceptar.setOnClickListener { callback(true) }
                btnRechazar.setOnClickListener { callback(false) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_solicitud, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }
}
