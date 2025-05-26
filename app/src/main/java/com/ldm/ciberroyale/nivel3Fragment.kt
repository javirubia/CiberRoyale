package com.ldm.ciberroyale

import android.content.ClipData
import android.os.Bundle
import android.os.CountDownTimer
import android.view.DragEvent
import android.view.Gravity
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
        FILTRAR_SOLICITUDES,
        COMBATE_MATCH3,
        RECOMPENSA
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

    // --- Modelo Match-3 ---
    private enum class Tile { CANDADO, GLOBO, ENGRANAJE;
        companion object {
            fun random() = values().random()
        }
    }

    // --- Vistas ---
    private lateinit var pantallaIntro: View
    private lateinit var pantallaDetect: View
    private lateinit var pantallaAjustes: View
    private lateinit var pantallaFiltrar: View
    private lateinit var pantallaCombate: View

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

    // Combate Match-3
    private lateinit var pbVidaEnemigo: ProgressBar
    private lateinit var pbEscudoJugador: ProgressBar
    private lateinit var tvObjetivo: TextView
    private lateinit var tvMovsRest: TextView
    private lateinit var gvTablero: GridView
    private lateinit var btnPista: Button
    private var firstSelectedPos: Int? = null
    private var vidaEnemigo = 100
    private var escudoJugador = 100
    private var movRestantes = 20
    private val tamañoTablero = 8 * 8
    private val tablero = mutableListOf<Tile>()
    private val objetivoHP = 100

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind pantallas
        pantallaIntro   = view.findViewById(R.id.pantalla_intro)
        pantallaDetect  = view.findViewById(R.id.pantalla_detect_oversharing)
        pantallaAjustes = view.findViewById(R.id.pantalla_ajustes)
        pantallaFiltrar = view.findViewById(R.id.pantalla_filtrar_solicitudes)
        pantallaCombate = view.findViewById(R.id.pantalla_combate_match3)

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
                iniciarCombateMatch3()
            }.show()
        }

        // COMBATE MATCH-3
        pbVidaEnemigo   = view.findViewById(R.id.pbVidaEnemigo)
        pbEscudoJugador = view.findViewById(R.id.pbEscudoJugador)
        tvObjetivo      = view.findViewById(R.id.tvObjetivoCombate)
        tvMovsRest      = view.findViewById(R.id.tvMovimientosRestantes)
        gvTablero       = view.findViewById(R.id.gvTablero)
        btnPista        = view.findViewById(R.id.btnPista)
        btnPista.setOnClickListener { usarPista() }
        gvTablero.adapter = TableroAdapter(tablero) { pos -> onTileSelected(pos) }


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
                pbTimer.progress = (millisUntilFinished * 100 / 30000).toInt()
            }
            override fun onFinish() {
                pbTimer.progress = 0
                btnFinalizar.isEnabled = true
            }
        }.start()

        mostrarEtapa(Etapa.FILTRAR_SOLICITUDES)
    }

    private fun evaluarSolicitud(aceptada: Boolean) {
        val sol      = solicitudes[idxSolicitud]
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

    // --- Combate Match-3 ---
    private fun iniciarCombateMatch3() {
        vidaEnemigo    = 100
        escudoJugador  = 100
        movRestantes   = 20
        tablero.clear()

        pbVidaEnemigo.progress   = vidaEnemigo
        pbEscudoJugador.progress = escudoJugador
        tvObjetivo.text          = "Inflige $objetivoHP HP en ≤ $movRestantes movimientos"
        tvMovsRest.text          = "Mov: $movRestantes"

        iniciarTablero()
        mostrarEtapa(Etapa.COMBATE_MATCH3)
    }

    private fun iniciarTablero() {
        repeat(tamañoTablero) { tablero.add(Tile.random()) }
        gvTablero.adapter = TableroAdapter(tablero) { pos -> onTileSelected(pos) }
    }

    private fun onTileSelected(pos: Int) {
        if (firstSelectedPos == null) {
            // guardamos primera selección
            firstSelectedPos = pos
        } else {
            val second = pos
            val first  = firstSelectedPos!!
            // sólo permitimos swap con casillas adyacentes
            if (isAdjacent(first, second)) {
                swapTiles(first, second)
                // procesamos matches: si no hay, revertimos swap
                if (!processMatches()) swapTiles(first, second)
                else {
                    movRestantes--
                    tvMovsRest.text = "Mov: $movRestantes"
                    // si no quedan movimientos o enemigo muerto, terminamos
                    if (movRestantes == 0 || vidaEnemigo <= 0) {
                        terminarCombate(vidaEnemigo <= 0)
                    }
                }
            }
            firstSelectedPos = null
        }
    }

    private fun usarPista() {
        escudoJugador = maxOf(0, escudoJugador - 5)
        pbEscudoJugador.progress = escudoJugador
        // TODO: resaltar un match sugerido en gvTablero

    }

    private fun terminarCombate(victoria: Boolean) {
        if (victoria) {
            mostrarEtapa(Etapa.RECOMPENSA)
        } else {
            Toast.makeText(context, "Has sido derrotado… inténtalo de nuevo.", Toast.LENGTH_LONG).show()
            iniciarCombateMatch3()
        }
    }

    private fun isAdjacent(a: Int, b: Int): Boolean {
        val cols = 8
        return when (b) {
            a - 1 -> a % cols != 0          // izquierda
            a + 1 -> b % cols != 0          // derecha
            a - cols, a + cols -> true      // arriba/abajo
            else -> false
        }
    }

    private fun swapTiles(a: Int, b: Int) {
        val tmp = tablero[a]
        tablero[a] = tablero[b]
        tablero[b] = tmp
        (gvTablero.adapter as BaseAdapter).notifyDataSetChanged()
    }

    private fun processMatches(): Boolean {
        val matches = findMatches()
        if (matches.isEmpty()) return false

        // aplicamos efectos
        matches.forEach { idx ->
            when (tablero[idx]) {
                Tile.CANDADO   -> vidaEnemigo = (vidaEnemigo - 10).coerceAtLeast(0)
                Tile.GLOBO     -> escudoJugador = (escudoJugador + 8).coerceAtMost(100)
                Tile.ENGRANAJE -> escudoJugador = (escudoJugador + 5).coerceAtMost(100)
            }
        }
        // actualizamos barras
        pbVidaEnemigo.progress   = vidaEnemigo
        pbEscudoJugador.progress = escudoJugador

        // refill: nuevas fichas aleatorias en las posiciones eliminadas
        matches.forEach { tablero[it] = Tile.random() }
        (gvTablero.adapter as BaseAdapter).notifyDataSetChanged()
        return true
    }

    private fun findMatches(): Set<Int> {
        val matches = mutableSetOf<Int>()
        val cols = 8
        val rows = 8

        // horizontal
        for (r in 0 until rows) {
            var streak = 1
            for (c in 1 until cols) {
                val prev = r*cols + (c-1)
                val curr = r*cols + c
                if (tablero[curr] == tablero[prev]) {
                    streak++
                } else {
                    if (streak >= 3) {
                        for (k in 0 until streak) matches += r*cols + (c-1 - k)
                    }
                    streak = 1
                }
            }
            if (streak >= 3) {
                for (k in 0 until streak) matches += r*cols + (cols-1 - k)
            }
        }

        // vertical
        for (c in 0 until cols) {
            var streak = 1
            for (r in 1 until rows) {
                val prev = (r-1)*cols + c
                val curr = r*cols + c
                if (tablero[curr] == tablero[prev]) {
                    streak++
                } else {
                    if (streak >= 3) {
                        for (k in 0 until streak) matches += (r-1-k)*cols + c
                    }
                    streak = 1
                }
            }
            if (streak >= 3) {
                for (k in 0 until streak) matches += (rows-1-k)*cols + c
            }
        }

        return matches
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
        pantallaCombate.visibility = View.GONE
        when (etapa) {
            Etapa.INTRO               -> pantallaIntro.visibility   = View.VISIBLE
            Etapa.DETECT_OVERSHARING  -> pantallaDetect.visibility  = View.VISIBLE
            Etapa.AJUSTES             -> pantallaAjustes.visibility = View.VISIBLE
            Etapa.FILTRAR_SOLICITUDES -> pantallaFiltrar.visibility = View.VISIBLE
            Etapa.COMBATE_MATCH3      -> pantallaCombate.visibility = View.VISIBLE
            Etapa.RECOMPENSA          -> { /* implementar recompensa */ }
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

    // --- Adapter interno para el Tablero Match-3 ---
    private inner class TableroAdapter(
        private val items: List<Tile>,
        private val callback: (Int) -> Unit
    ) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = TextView(parent.context).apply {
                text = items[position].name
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                setOnClickListener { callback(position) }
            }
            return tv
        }
    }
}
