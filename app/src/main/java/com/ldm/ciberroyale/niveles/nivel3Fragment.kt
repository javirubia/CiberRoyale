package com.ldm.ciberroyale.niveles

import android.content.ClipData
import android.os.Bundle
import android.os.CountDownTimer
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel3Binding
import kotlin.math.abs
import android.media.MediaPlayer
import android.view.animation.AnimationUtils

typealias Pos = Pair<Int, Int>

class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    private var _binding: FragmentNivel3Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val FILAS = 8
        private const val COLS = 8
        private const val OBJETIVO_HP = 100
        private const val MOVS_INICIALES = 20
        private const val TIMER_MS = 30_000L
        private const val TIMER_TICK_MS = 30L
    }

    private enum class Etapa { INTRO, DETECT, AJUSTES, FILTRAR, COMBATE, RECOMPENSA }

    data class Post(val texto: String, val imagenRes: Int?, val esPublico: Boolean, val explicacion: String)
    private val posts = listOf(
        Post("Foto de mis vacaciones con geolocalización",            R.drawable.ic_heroe, false, "Incluye geolocalización..."),
        Post("Foto de mi mascota jugando",                            R.drawable.ic_heroe, true,  "No revela..."),
        Post("Mi hermana cumple hoy 10 años y está sola en casa",     null,                   false, "Revela ubicación..."),
        Post("Fui al parque y fue muy divertido",                    null,                   true,  "No contiene datos sensibles.")
    )

    private val ajustesEsperados = mapOf("Fotos" to false, "Ubicacion" to false, "Lista" to true)
    private val explicacionesAjustes = mapOf(
        "Fotos"     to "Si dejas las fotos públicas...",
        "Ubicacion" to "Compartir ubicación...",
        "Lista"     to "Mostrar tu lista..."
    )

    data class Solicitud(val nombre: String, val avatarRes: Int, val esConocido: Boolean)
    private val solicitudes = listOf(
        Solicitud("Ana Pérez",      R.drawable.ic_heroe, true),
        Solicitud("Carlos Gómez",   R.drawable.ic_heroe, false),
        Solicitud("María Ruiz",     R.drawable.ic_heroe, true),
        Solicitud("Desconocido123", R.drawable.ic_actor, false)
    )

    private var idxPost = 0
    private var idxSolicitud = 0
    private var puntosFiltrar = 0
    private lateinit var timer: CountDownTimer

    // Motor de combate Match-3
    private val engine = Match3Engine(FILAS, COLS)
    private var selectedPos: Pos? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)
        setupUI()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() = with(binding) {
        // 1. INTRO
        btnIntro3Siguiente.setOnClickListener {
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // 2. DETECT OVERSHARING
        cardPost.setDragAndDrop(
            onStart = { v ->
                v.startDragAndDrop(ClipData.newPlainText("", ""), View.DragShadowBuilder(v), v, 0)
            },
            targets = listOf(targetPublico, targetPrivado),
            onDrop = ::handleDetectDrop
        )

        // 3. AJUSTES
        btnInfoFotos.setOnClickListener     { mostrarDialogo("Fotos",      explicacionesAjustes["Fotos"]!!) }
        btnInfoUbicacion.setOnClickListener { mostrarDialogo("Ubicación",  explicacionesAjustes["Ubicacion"]!!) }
        btnInfoLista.setOnClickListener     { mostrarDialogo("Lista",      explicacionesAjustes["Lista"]!!) }
        btnVerificarAjustes.setOnClickListener { evaluarAjustes() }

        // 4. FILTRAR SOLICITUDES
        rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        rvSolicitudes.adapter       = SolicitudAdapter(solicitudes) { acepto -> evalSolicitud(acepto) }
        btnFinalizarFiltrar.setOnClickListener {
            timer.cancel()
            mostrarResultadoFiltrar()
        }

        // 5. COMBATE MATCH-3
        btnPista.setOnClickListener { usarPista() }

        // 6. RECOMPENSA
        btnRecompContinuar.setOnClickListener {
            // Navega al siguiente fragmento / pantalla de juego
        }
    }

    // --- DETECT OVERSHARING ---
    private fun iniciarDetect() {
        idxPost = 0
        mostrarPost()
        mostrarEtapa(Etapa.DETECT)
    }

    private fun mostrarPost() = with(binding) {
        val post = posts[idxPost]
        tvPost.text       = post.texto
        imgPost.isVisible = post.imagenRes != null
        post.imagenRes?.let { imgPost.setImageResource(it) }
    }

    private fun handleDetectDrop(targetId: Int) {
        val post = posts[idxPost]
        val correcto = (targetId == binding.targetPublico.id) == post.esPublico

        val feedbackMessage = if (correcto) "¡Correcto!" else "Incorrecto…"
        val feedbackSound = if (correcto) R.raw.correct else R.raw.incorrect

        // Play sound
        MediaPlayer.create(requireContext(), feedbackSound).start()

        // Animate card
        binding.cardPost.apply {
            val anim = AnimationUtils.loadAnimation(context, if (correcto) R.anim.correct_answer else R.anim.incorrect_answer)
            startAnimation(anim)
        }

        Snackbar.make(binding.root,
            feedbackMessage,
            Snackbar.LENGTH_SHORT
        ).setAction("¿Por qué?") {
            mostrarDialogo("Explicación", post.explicacion)
        }.show()

        idxPost++
        if (idxPost < posts.size) {
            mostrarPost()
        } else {
            iniciarAjustes()
        }
    }

    // --- AJUSTES ---
    private fun iniciarAjustes() = with(binding) {
        switchFotos.isChecked     = !ajustesEsperados["Fotos"]!!
        switchUbicacion.isChecked = !ajustesEsperados["Ubicacion"]!!
        switchLista.isChecked     =  ajustesEsperados["Lista"]!!
        mostrarEtapa(Etapa.AJUSTES)
    }

    private fun evaluarAjustes() = with(binding) {
        val okF = switchFotos.isChecked     != ajustesEsperados["Fotos"]!!
        val okU = switchUbicacion.isChecked != ajustesEsperados["Ubicacion"]!!
        val okL = switchLista.isChecked     == ajustesEsperados["Lista"]!!
        val allOk = listOf(okF, okU, okL).all { it }

        Snackbar.make(root,
            if (allOk) "¡Muy bien!" else "Corrige…",
            Snackbar.LENGTH_LONG
        ).setAction(if (allOk) "Continuar" else "¿Por qué?") {
            if (allOk) iniciarFiltrar() else mostrarErrores(okF, okU, okL)
        }.show()
    }

    private fun mostrarErrores(f: Boolean, u: Boolean, l: Boolean) {
        val msg = buildString {
            if (!f) append("• Fotos: ${explicacionesAjustes["Fotos"]}\n\n")
            if (!u) append("• Ubicación: ${explicacionesAjustes["Ubicacion"]}\n\n")
            if (!l) append("• Lista: ${explicacionesAjustes["Lista"]}")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Errores")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    // --- FILTRAR SOLICITUDES ---
    private fun iniciarFiltrar() {
        idxSolicitud   = 0
        puntosFiltrar  = 0
        binding.btnFinalizarFiltrar.isEnabled = false

        timer = object : CountDownTimer(TIMER_MS, TIMER_TICK_MS) {
            override fun onTick(millis: Long) {
                binding.pbTimerFiltrar.progress = (millis * 100 / TIMER_MS).toInt()
            }
            override fun onFinish() {
                binding.pbTimerFiltrar.progress = 0
                binding.btnFinalizarFiltrar.isEnabled = true
            }
        }.start()

        mostrarEtapa(Etapa.FILTRAR)
    }

    private fun evalSolicitud(acepta: Boolean) {
        val sol = solicitudes[idxSolicitud]
        val correcto = (acepta == sol.esConocido)

        val feedbackMessage = if (correcto) "¡Correcto!" else "Incorrecto"
        val feedbackSound = if (correcto) R.raw.correct else R.raw.incorrect

        // Play sound
        MediaPlayer.create(requireContext(), feedbackSound).start()

        // Animate item
        binding.rvSolicitudes.findViewHolderForAdapterPosition(idxSolicitud)?.itemView?.apply {
            val anim = AnimationUtils.loadAnimation(context, if (correcto) R.anim.correct_answer else R.anim.incorrect_answer)
            startAnimation(anim)
        }

        Snackbar.make(binding.root,
            feedbackMessage,
            Snackbar.LENGTH_SHORT
        ).show()

        idxSolicitud++
        if (idxSolicitud >= solicitudes.size) {
            binding.btnFinalizarFiltrar.isEnabled = true
            timer.cancel()
        } else {
            binding.rvSolicitudes.apply {
                adapter?.notifyItemChanged(idxSolicitud - 1)
                scrollToPosition(idxSolicitud)
            }
        }
    }

    private fun mostrarResultadoFiltrar() {
        Snackbar.make(binding.root,
            "Filtrado: $puntosFiltrar/${solicitudes.size}",
            Snackbar.LENGTH_LONG
        ).setAction("Continuar") {
            iniciarCombateMatch3()
        }.show()
    }

    // --- COMBATE MATCH-3 ---
    private fun iniciarCombateMatch3() {
        engine.reset(OBJETIVO_HP, MOVS_INICIALES)
        selectedPos = null

        // 1. Configurar tablero
        binding.gvTablero.apply {
            rowCount    = FILAS
            columnCount = COLS
            removeAllViews()
        }
        binding.tvObjetivoCombate.text      = "Inflige $OBJETIVO_HP HP en ≤ $MOVS_INICIALES movimientos"
        binding.tvMovimientosRestantes.text = "Mov: $MOVS_INICIALES"

        // 2. Pintar y actualizar estado
        actualizarBarras()
        drawBoard()
        mostrarEtapa(Etapa.COMBATE)
    }

    private fun drawBoard() {
        val board = binding.gvTablero
        board.removeAllViews()

        for (r in 0 until FILAS) {
            for (c in 0 until COLS) {
                val iv = ImageView(requireContext()).apply {
                    setImageResource(engine.getDrawableAt(r, c))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setOnClickListener { onTileClick(r to c, this) }
                }
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(r, 1, 1f),
                    GridLayout.spec(c, 1, 1f)
                ).apply {
                    width  = 0
                    height = 0
                    setMargins(4, 4, 4, 4)
                }
                board.addView(iv, lp)
            }
        }
    }

    private fun onTileClick(pos: Pos, view: View) {
        if (selectedPos == null) {
            selectedPos = pos
            view.alpha = 0.5f
        } else {
            val first = selectedPos!!
            view.alpha = 1f
            if (engine.canSwap(first, pos)) animateSwap(first, pos)
            selectedPos = null
        }
    }

    private fun animateSwap(a: Pos, b: Pos) {
        val idxA = a.first * COLS + a.second
        val idxB = b.first * COLS + b.second
        val vA = binding.gvTablero.getChildAt(idxA)
        val vB = binding.gvTablero.getChildAt(idxB)
        val xA = vA.x; val yA = vA.y
        val xB = vB.x; val yB = vB.y

        vA.animate().x(xB).y(yB).setDuration(200).start()
        vB.animate().x(xA).y(yA).setDuration(200).withEndAction {
            if (!engine.swap(a, b)) {
                vA.animate().x(xA).y(yA).setDuration(200).start()
                vB.animate().x(xB).y(yB).setDuration(200).start()
            } else {
                handleMatches()
            }
        }.start()
    }

   private fun handleMatches() {
        handleMatchesStep()
    }

    private fun handleMatchesStep() {
        val matches = engine.findMatches()
        if (matches.isEmpty()) return

        // Animar fade-out
        val animations = matches.map { (r, c) ->
            binding.gvTablero.getChildAt(r * COLS + c).animate().alpha(0f).setDuration(200)
        }

        // After animations complete, clear and update the board
        animations.forEach { it.start() }

        // Use a Handler to post the update to the main thread after a delay
        binding.root.postDelayed({
            engine.clearMatches(matches)
            engine.collapse()
            engine.refill()
            actualizarBoard()
            actualizarBarras()
            handleMatchesStep() // Call the next step
        }, 250)
    }

    private fun actualizarBoard() {
        for (i in 0 until FILAS * COLS) {
            val (r, c) = i / COLS to i % COLS
            (binding.gvTablero.getChildAt(i) as ImageView).apply {
                setImageResource(engine.getDrawableAt(r, c))
                alpha = 1f
            }
        }
    }

    private fun actualizarBarras() = with(binding) {
        pbVidaEnemigo.progress    = engine.vidaEnemigo
        pbEscudoJugador.progress   = engine.escudoJugador
        tvMovimientosRestantes.text = "Mov: ${engine.movimientosRestantes}"
    }

    private fun usarPista() {
        engine.useHint()?.let { (r, c) ->
            val v = binding.gvTablero.getChildAt(r * COLS + c)
            v.animate().alpha(0.3f).setDuration(200)
                .withEndAction { v.animate().alpha(1f).setDuration(200).start() }
                .start()
        }
        actualizarBarras()
    }

    private fun mostrarDialogo(titulo: String, mensaje: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun mostrarEtapa(etapa: Etapa) = with(binding) {
        listOf(
            pantallaIntro,
            pantallaDetectOversharing,
            pantallaAjustes,
            pantallaFiltrarSolicitudes,
            pantallaCombateMatch3,
            pantallaRecomp
        ).forEach { it.isVisible = false }

        when (etapa) {
            Etapa.INTRO      -> pantallaIntro.isVisible = true
            Etapa.DETECT     -> pantallaDetectOversharing.isVisible = true
            Etapa.AJUSTES    -> pantallaAjustes.isVisible = true
            Etapa.FILTRAR    -> pantallaFiltrarSolicitudes.isVisible = true
            Etapa.COMBATE    -> pantallaCombateMatch3.isVisible = true
            Etapa.RECOMPENSA -> pantallaRecomp.isVisible = true
        }
    }

    // Extensión para drag & drop
    private fun CardView.setDragAndDrop(
        onStart: (View) -> Unit,
        targets: List<View>,
        onDrop: (targetId: Int) -> Unit
    ) {
        setOnLongClickListener { view -> onStart(view); true }
        val listener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) onDrop(v.id)
            true
        }
        targets.forEach { it.setOnDragListener(listener) }
    }

    // Motor Match-3 integrado
    class Match3Engine(private val rows: Int, private val cols: Int) {
        enum class Type { ROJO, AZUL, VERDE, AMARILLO }
        data class Tile(var type: Type)
        val board = Array(rows) { Array(cols) { Tile(randomType()) } }
        var vidaEnemigo = OBJETIVO_HP
        var escudoJugador = OBJETIVO_HP
        var movimientosRestantes = 0

        private fun randomType() = Type.values().random()

        fun reset(obj: Int, movs: Int) {
            repeat(rows) { r -> repeat(cols) { c -> board[r][c] = Tile(randomType()) }}
            vidaEnemigo = obj
            escudoJugador = obj
            movimientosRestantes = movs
        }

        fun getDrawableAt(r: Int, c: Int) = when (board[r][c].type) {
            Type.ROJO     -> R.drawable.ic_no_compartir
            Type.AZUL     -> R.drawable.ic_shield
            Type.VERDE    -> R.drawable.ic_warning_email
            Type.AMARILLO -> R.drawable.ic_candado
        }

        fun canSwap(a: Pos, b: Pos) = abs(a.first - b.first) + abs(a.second - b.second) == 1

        fun swap(a: Pos, b: Pos): Boolean {
            board[a.first][a.second].also {
                board[a.first][a.second] = board[b.first][b.second]
                board[b.first][b.second] = it
            }
            val ok = findMatches().isNotEmpty()
           if (ok) movimientosRestantes-- else {
                // Swap back
                val temp = board[a.first][a.second]
                board[a.first][a.second] = board[b.first][b.second]
                board[b.first][b.second] = temp
                return false
            }
            return ok
        }

        fun findMatches(): Set<Pos> {
            val found = mutableSetOf<Pos>()
            // horizontales
            for (r in 0 until rows) {
                var streak = 1
                for (c in 1 until cols) {
                    if (board[r][c].type == board[r][c-1].type) streak++
                    else {
                        if (streak >= 3) repeat(streak) { k -> found += (r to (c-1-k)) }
                        streak = 1
                    }
                }
                if (streak >= 3) repeat(streak) { k -> found += (r to (cols-1-k)) }
            }
            // verticales
            for (c in 0 until cols) {
                var streak = 1
                for (r in 1 until rows) {
                    if (board[r][c].type == board[r-1][c].type) streak++
                    else {
                        if (streak >= 3) repeat(streak) { k -> found += ((r-1-k) to c) }
                        streak = 1
                    }
                }
                if (streak >= 3) repeat(streak) { k -> found += ((rows-1-k) to c) }
            }
            return found
        }

        fun clearMatches(matches: Set<Pos>) {
            matches.forEach { (r, c) ->
                when (board[r][c].type) {
                    Type.ROJO    -> vidaEnemigo = (vidaEnemigo - 10).coerceAtLeast(0)
                    else         -> escudoJugador = (escudoJugador + 5).coerceAtMost(OBJETIVO_HP)
                }
                board[r][c] = Tile(randomType())
            }
        }

        fun collapse() = Unit
        fun refill()   = Unit
        fun useHint()  = findMatches().firstOrNull()
    }

    // Adapter para la lista de solicitudes
    inner class SolicitudAdapter(
        private val items: List<Solicitud>,
        private val cb: (Boolean) -> Unit
    ) : RecyclerView.Adapter<SolicitudAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val ivAvatar = v.findViewById<ImageView>(R.id.ivAvatar)
            private val tvNombre = v.findViewById<TextView>(R.id.tvNombre)
            private val btnA     = v.findViewById<View>(R.id.btnAceptar)
            private val btnR     = v.findViewById<View>(R.id.btnRechazar)
            fun bind(s: Solicitud) {
                ivAvatar.setImageResource(s.avatarRes)
                tvNombre.text = s.nombre
                btnA.setOnClickListener { cb(true) }
                btnR.setOnClickListener { cb(false) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_solicitud, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size
    }
}
