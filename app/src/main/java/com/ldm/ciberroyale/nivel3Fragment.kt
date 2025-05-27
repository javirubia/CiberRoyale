package com.ldm.ciberroyale

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.ClipData
import android.graphics.Point
import android.os.Bundle
import android.os.CountDownTimer
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.R.drawable
import androidx.gridlayout.widget.GridLayout

typealias Pos = Pair<Int, Int>

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

    // --- Datos para oversharing, ajustes y solicitudes (sin cambios) ---
    private data class Post(val texto: String, val imagenRes: Int?, val esPublico: Boolean, val explicacion: String)
    private val posts = listOf(
        Post("Foto de mis vacaciones con geolocalización", R.drawable.ic_heroe, false, "Incluye geolocalización en los metadatos de la imagen."),
        Post("Foto de mi mascota jugando", R.drawable.ic_heroe, true, "No revela información personal ni ubicación."),
        Post("Texto: Mi hermana cumple hoy 10 años y está sola en casa", null, false, "Revela ubicación y situación familiar vulnerable."),
        Post("Publicación: Fui al parque y fue muy divertido", null, true, "No contiene datos sensibles.")
    )
    private val ajustesEsperados = mapOf("Fotos" to false, "Ubicacion" to false, "Lista" to true)
    private val explicacionesAjustes = mapOf(
        "Fotos" to "Si dejas las fotos públicas, cualquier persona verá tus imágenes sin control.",
        "Ubicacion" to "Compartir ubicación en tiempo real puede poner en riesgo tu privacidad.",
        "Lista" to "Mostrar tu lista de amigos ayuda a verificar solicitudes de personas conocidas."
    )
    private data class Solicitud(val nombre: String, val avatarRes: Int, val esConocido: Boolean)
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
    private lateinit var pantallaCombate: View

    // Detect oversharing
    private lateinit var btnIntro: View
    private lateinit var cardPost: CardView
    private lateinit var imgPost: ImageView
    private lateinit var tvPost: TextView
    private lateinit var targetPublico: View
    private lateinit var targetPrivado: View
    private var idxPost = 0

    // Ajustes
    private lateinit var switchFotos: SwitchMaterial
    private lateinit var switchUbicacion: SwitchMaterial
    private lateinit var switchLista: SwitchMaterial
    private lateinit var btnInfoFotos: View
    private lateinit var btnInfoUbicacion: View
    private lateinit var btnInfoLista: View
    private lateinit var btnVerificarAjustes: View

    // Filtrar solicitudes
    private lateinit var rvSolicitudes: RecyclerView
    private lateinit var pbTimer: ProgressBar
    private lateinit var btnFinalizar: View
    private var idxSolicitud = 0
    private var puntosFiltrar = 0
    private lateinit var timer: CountDownTimer

    // Match-3
    private lateinit var gridLayout: GridLayout
    private lateinit var pbVidaEnemigo: ProgressBar
    private lateinit var pbEscudoJugador: ProgressBar
    private lateinit var tvObjetivo: TextView
    private lateinit var tvMovsRest: TextView
    private lateinit var btnPista: View

    private val filas = 8
    private val cols = 8
    private val objetivoHP = 100
    private val movimientosIniciales = 20

    private val engine = Match3Engine(filas, cols)
    private var primeraPos: Pos? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bind pantallas
        pantallaIntro = view.findViewById(R.id.pantalla_intro)
        pantallaDetect = view.findViewById(R.id.pantalla_detect_oversharing)
        pantallaAjustes = view.findViewById(R.id.pantalla_ajustes)
        pantallaFiltrar = view.findViewById(R.id.pantalla_filtrar_solicitudes)
        pantallaCombate = view.findViewById(R.id.pantalla_combate_match3)

        setupIntro(view)
        setupDetect(view)
        setupAjustes(view)
        setupFiltrar(view)
        setupCombate(view)

        mostrarEtapa(Etapa.INTRO)
    }

    // --- Intro ---
    private fun setupIntro(view: View) {
        btnIntro = view.findViewById(R.id.btnIntro3Siguiente)
        btnIntro.setOnClickListener {
            iniciarDetect()
        }
    }

    // --- Detect oversharing (igual que antes) ---
    private fun setupDetect(view: View) {
        cardPost = view.findViewById(R.id.cardPost)
        imgPost = view.findViewById(R.id.imgPost)
        tvPost = view.findViewById(R.id.tvPost)
        targetPublico = view.findViewById(R.id.targetPublico)
        targetPrivado = view.findViewById(R.id.targetPrivado)
        cardPost.setOnLongClickListener { v ->
            v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            true
        }
        val listener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) handleDetectDrop(v.id)
            true
        }
        targetPublico.setOnDragListener(listener)
        targetPrivado.setOnDragListener(listener)
    }

    private fun iniciarDetect() {
        idxPost = 0
        mostrarPost()
        mostrarEtapa(Etapa.DETECT_OVERSHARING)
    }
    private fun mostrarPost() {
        val post = posts[idxPost]
        tvPost.text = post.texto
        imgPost.visibility = if (post.imagenRes != null) View.VISIBLE else View.GONE
        post.imagenRes?.let { imgPost.setImageResource(it) }
    }
    private fun handleDetectDrop(targetId: Int) {
        val post = posts[idxPost]
        val correcto = (targetId == if (post.esPublico) R.id.targetPublico else R.id.targetPrivado)
        Snackbar.make(requireView(), if (correcto) "¡Correcto!" else "Incorrecto…", Snackbar.LENGTH_SHORT)
            .setAction("¿Por qué?") { mostrarDialogo("Explicación", post.explicacion) }
            .show()
        idxPost++
        if (idxPost < posts.size) mostrarPost()
        else iniciarAjustes()
    }

    // --- Ajustes ---
    private fun setupAjustes(view: View) {
        switchFotos = view.findViewById(R.id.switchFotos)
        switchUbicacion = view.findViewById(R.id.switchUbicacion)
        switchLista = view.findViewById(R.id.switchLista)
        btnInfoFotos = view.findViewById(R.id.btnInfoFotos)
        btnInfoUbicacion = view.findViewById(R.id.btnInfoUbicacion)
        btnInfoLista = view.findViewById(R.id.btnInfoLista)
        btnVerificarAjustes = view.findViewById(R.id.btnVerificarAjustes)
        btnInfoFotos.setOnClickListener { mostrarDialogo("Fotos", explicacionesAjustes["Fotos"]!!) }
        btnInfoUbicacion.setOnClickListener { mostrarDialogo("Ubicación", explicacionesAjustes["Ubicacion"]!!) }
        btnInfoLista.setOnClickListener { mostrarDialogo("Lista de amigos", explicacionesAjustes["Lista"]!!) }
        btnVerificarAjustes.setOnClickListener { evaluarAjustes() }
    }
    private fun iniciarAjustes() {
        switchFotos.isChecked = !ajustesEsperados["Fotos"]!!
        switchUbicacion.isChecked = !ajustesEsperados["Ubicacion"]!!
        switchLista.isChecked = !ajustesEsperados["Lista"]!!
        mostrarEtapa(Etapa.AJUSTES)
    }
    private fun evaluarAjustes() {
        val okFotos = switchFotos.isChecked == ajustesEsperados["Fotos"]
        val okUbic = switchUbicacion.isChecked == ajustesEsperados["Ubicacion"]
        val okLista = switchLista.isChecked == ajustesEsperados["Lista"]
        val todo = listOf(okFotos, okUbic, okLista).all { it }
        Snackbar.make(requireView(), if (todo) "¡Muy bien!" else "Corrige los ajustes…", Snackbar.LENGTH_LONG)
            .setAction(if (todo) "Continuar" else "¿Por qué?") {
                if (todo) iniciarFiltrar() else mostrarErroresAjustes(okFotos, okUbic, okLista)
            }.show()
    }
    private fun mostrarErroresAjustes(okF: Boolean, okU: Boolean, okL: Boolean) {
        val msg = buildString {
            if (!okF) append("• Fotos: ${explicacionesAjustes["Fotos"]}\n\n")
            if (!okU) append("• Ubicación: ${explicacionesAjustes["Ubicacion"]}\n\n")
            if (!okL) append("• Lista: ${explicacionesAjustes["Lista"]}")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Explicaciones")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    // --- Filtrar solicitudes ---
    private fun setupFiltrar(view: View) {
        rvSolicitudes = view.findViewById(R.id.rvSolicitudes)
        pbTimer = view.findViewById(R.id.pbTimerFiltrar)
        btnFinalizar = view.findViewById(R.id.btnFinalizarFiltrar)
        rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        rvSolicitudes.adapter = SolicitudAdapter(solicitudes) { evalSolicitud(it) }
        btnFinalizar.setOnClickListener { timer.cancel(); mostrarResultadoFiltrar() }
    }
    private fun iniciarFiltrar() {
        idxSolicitud = 0; puntosFiltrar = 0
        btnFinalizar.isEnabled = false
        timer = object : CountDownTimer(30_00, 30) {
            override fun onTick(millis: Long) = run { pbTimer.progress = (millis * 100 / 30000).toInt() }
            override fun onFinish() { pbTimer.progress = 0; btnFinalizar.isEnabled = true }
        }.start()
        mostrarEtapa(Etapa.FILTRAR_SOLICITUDES)
    }
    private fun evalSolicitud(acepta: Boolean) {
        val sol = solicitudes[idxSolicitud]
        if (acepta == sol.esConocido) puntosFiltrar++
        Snackbar.make(requireView(), if (acepta == sol.esConocido) "¡Correcto!" else "Incorrecto", Snackbar.LENGTH_SHORT).show()
        idxSolicitud++
        if (idxSolicitud >= solicitudes.size) { btnFinalizar.isEnabled = true; timer.cancel() }
        else { rvSolicitudes.adapter?.notifyItemChanged(idxSolicitud-1); rvSolicitudes.scrollToPosition(idxSolicitud) }
    }
    private fun mostrarResultadoFiltrar() {
        Snackbar.make(requireView(), "Filtrado: $puntosFiltrar/${solicitudes.size} correctas", Snackbar.LENGTH_LONG)
            .setAction("Continuar") { iniciarCombateMatch3() }
            .show()
    }

    // --- Combate Match-3 ---
    private fun setupCombate(view: View) {
        gridLayout = view.findViewById(R.id.gvTablero) as GridLayout
        pbVidaEnemigo = view.findViewById(R.id.pbVidaEnemigo)
        pbEscudoJugador = view.findViewById(R.id.pbEscudoJugador)
        tvObjetivo = view.findViewById(R.id.tvObjetivoCombate)
        tvMovsRest = view.findViewById(R.id.tvMovimientosRestantes)
        btnPista = view.findViewById(R.id.btnPista)
        btnPista.setOnClickListener { usarPista() }
    }
    private fun iniciarCombateMatch3() {
        engine.reset(objetivoHP, movimientosIniciales)
        gridLayout.removeAllViews()
        gridLayout.rowCount = filas
        gridLayout.columnCount = cols
        primeraPos = null
        actualizarBarras()
        tvObjetivo.text = "Inflige $objetivoHP HP en ≤ $movimientosIniciales movimientos"
        tvMovsRest.text = "Mov: $movimientosIniciales"
        drawBoard()
        mostrarEtapa(Etapa.COMBATE_MATCH3)
    }
    private fun drawBoard() {
        gridLayout.removeAllViews()

        // Recorremos cada fila y columna
        for (r in 0 until filas) {
            for (c in 0 until cols) {
                val iv = ImageView(requireContext()).apply {
                    setImageResource(engine.getDrawableAt(r, c))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setOnClickListener { onTileClick(r to c, this) }
                }

                // ¡Este es el truco! Le damos un LayoutParams que
                // especifique su fila (r), columna (c) y peso 1f
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(r, 1, 1f),   // fila r, ocupa 1 fila, peso 1
                    GridLayout.spec(c, 1, 1f)    // col c, ocupa 1 columna, peso 1
                ).apply {
                    width = 0    // con peso, width=0 para repartir
                    height = 0   // idem para height
                    setMargins(4, 4, 4, 4)
                }

                gridLayout.addView(iv, lp)
            }
        }
    }

    private fun onTileClick(pos: Pos, view: View) {
        if (primeraPos == null) { primeraPos = pos; view.alpha = 0.5f }
        else {
            val segunda = pos
            val primera = primeraPos!!
            val firstView = gridLayout.getChildAt(primera.first*cols + primera.second)
            firstView.alpha = 1f
            if (engine.canSwap(primera, segunda)) animateSwap(primera, segunda)
            primeraPos = null
        }
    }
    private fun animateSwap(a: Pos, b: Pos) {
        val idxA = a.first*cols + a.second; val idxB = b.first*cols + b.second
        val viewA = gridLayout.getChildAt(idxA) as ImageView
        val viewB = gridLayout.getChildAt(idxB) as ImageView
        val xA = viewA.x; val yA = viewA.y; val xB = viewB.x; val yB = viewB.y
        viewA.animate().x(xB).y(yB).setDuration(200).start()
        viewB.animate().x(xA).y(yA).setDuration(200).withEndAction {
            if (!engine.swap(a, b)) {
                viewA.animate().x(xA).y(yA).setDuration(200).start()
                viewB.animate().x(xB).y(yB).setDuration(200).start()
            } else handleMatches()
        }.start()
    }
    private fun handleMatches() {
        val matches = engine.findMatches()
        if (matches.isEmpty()) return
        // animar fade-out
        matches.forEach { (r, c) -> gridLayout.getChildAt(r*cols + c).animate().alpha(0f).setDuration(200).start() }
        // tras delay, clear, cascade y refill
        gridLayout.postDelayed({
            engine.clearMatches(matches)
            engine.collapse()
            engine.refill()
            actualizarBoardViews()
            actualizarBarras()
            handleMatches()
        }, 250)
    }
    private fun actualizarBoardViews() {
        for (i in 0 until filas*cols) {
            val (r, c) = i/cols to i%cols
            (gridLayout.getChildAt(i) as ImageView).apply {
                setImageResource(engine.getDrawableAt(r, c))
                alpha = 1f
                x = (i%cols) * width.toFloat() + gridLayout.paddingLeft
                y = (i/cols) * height.toFloat() + gridLayout.paddingTop
            }
        }
    }
    private fun actualizarBarras() {
        pbVidaEnemigo.progress = engine.vidaEnemigo
        pbEscudoJugador.progress = engine.escudoJugador
        tvMovsRest.text = "Mov: ${engine.movimientosRestantes}"
    }
    private fun usarPista() {
        engine.useHint()?.let { pos ->
            val view = gridLayout.getChildAt(pos.first*cols + pos.second)
            view.animate().alpha(0.3f).setDuration(200).withEndAction { view.animate().alpha(1f).setDuration(200).start() }.start()
        }
        actualizarBarras()
    }

    // --- Helpers comunes ---
    private fun mostrarDialogo(t: String, m: String) = MaterialAlertDialogBuilder(requireContext()).setTitle(t).setMessage(m).setPositiveButton("OK", null).show()
    private fun mostrarEtapa(e: Etapa) {
        pantallaIntro.visibility = View.GONE; pantallaDetect.visibility = View.GONE; pantallaAjustes.visibility = View.GONE; pantallaFiltrar.visibility = View.GONE; pantallaCombate.visibility = View.GONE
        when(e) {
            Etapa.INTRO -> pantallaIntro.visibility = View.VISIBLE
            Etapa.DETECT_OVERSHARING -> pantallaDetect.visibility = View.VISIBLE
            Etapa.AJUSTES -> pantallaAjustes.visibility = View.VISIBLE
            Etapa.FILTRAR_SOLICITUDES -> pantallaFiltrar.visibility = View.VISIBLE
            Etapa.COMBATE_MATCH3 -> pantallaCombate.visibility = View.VISIBLE
            else -> {}
        }
    }

    // --- Motor Match-3 integrado ---
    private class Match3Engine(val rows: Int, val cols: Int) {
        enum class Type { ROJO, AZUL, VERDE, AMARILLO }
        data class Tile(var type: Type)

        val board = Array(rows) { Array(cols) { Tile(randomType()) } }
        var vidaEnemigo = 100
        var escudoJugador = 100
        var movimientosRestantes = 0

        fun reset(objetivoHP: Int, movs: Int) {
            repeat(rows) { r -> repeat(cols) { c -> board[r][c] = Tile(randomType()) }}
            vidaEnemigo = objetivoHP
            escudoJugador = objetivoHP
            movimientosRestantes = movs
        }
        private fun randomType() = Type.values().random()
        fun getDrawableAt(r: Int, c: Int): Int = when(board[r][c].type) {
            Type.ROJO -> R.drawable.ic_no_compartir
            Type.AZUL -> R.drawable.ic_shield
            Type.VERDE -> R.drawable.ic_warning_email
            Type.AMARILLO -> R.drawable.ic_candado
        }
        fun canSwap(a: Pos, b: Pos) = (Math.abs(a.first-b.first)+Math.abs(a.second-b.second)==1)
        fun swap(a: Pos, b: Pos): Boolean {
            val tmp = board[a.first][a.second]; board[a.first][a.second] = board[b.first][b.second]; board[b.first][b.second] = tmp
            val ok = findMatches().isNotEmpty()
            if (!ok) swap(a, b)
            else movimientosRestantes--
            return ok
        }
        fun findMatches(): Set<Pos> {
            val matches = mutableSetOf<Pos>()
            // horizontal
            for(r in 0 until rows) {
                var streak = 1
                for(c in 1 until cols) {
                    if(board[r][c].type==board[r][c-1].type) streak++ else {
                        if(streak>=3) for(k in 0 until streak) matches += r to (c-1-k)
                        streak=1
                    }
                }
                if(streak>=3) for(k in 0 until streak) matches += r to (cols-1-k)
            }
            // vertical
            for(c in 0 until cols) {
                var streak=1
                for(r in 1 until rows) {
                    if(board[r][c].type==board[r-1][c].type) streak++ else {
                        if(streak>=3) for(k in 0 until streak) matches += (r-1-k) to c
                        streak=1
                    }
                }
                if(streak>=3) for(k in 0 until streak) matches += (rows-1-k) to c
            }
            return matches
        }
        fun clearMatches(matches: Set<Pos>) {
            matches.forEach { (r,c) -> when(board[r][c].type) {
                Type.ROJO -> vidaEnemigo = (vidaEnemigo-10).coerceAtLeast(0)
                else -> escudoJugador = (escudoJugador+5).coerceAtMost(100)
            }; board[r][c] = Tile(randomType()) }
        }
        fun collapse() { /* no gravity needed si refill directo */ }
        fun refill() { /* ya rellenado en clearMatches */ }
        fun useHint(): Pos? = findMatches().firstOrNull()
    }

    // --- Adapters para solicitudes ---
    private inner class SolicitudAdapter(val items: List<Solicitud>, val cb: (Boolean)->Unit) : RecyclerView.Adapter<SolicitudAdapter.VH>() {
        inner class VH(v: View): RecyclerView.ViewHolder(v) {
            val iv = v.findViewById<ImageView>(R.id.ivAvatar)
            val tv = v.findViewById<TextView>(R.id.tvNombre)
            val btnA = v.findViewById<View>(R.id.btnAceptar)
            val btnR = v.findViewById<View>(R.id.btnRechazar)
            fun bind(s: Solicitud) {
                iv.setImageResource(s.avatarRes); tv.text = s.nombre
                btnA.setOnClickListener { cb(true) }
                btnR.setOnClickListener { cb(false) }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        override fun getItemCount() = items.size
    }
}
