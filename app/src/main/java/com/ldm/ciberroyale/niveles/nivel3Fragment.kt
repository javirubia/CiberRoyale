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
import android.animation.ObjectAnimator
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat

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

    enum class Difficulty { EASY, MEDIUM, HARD }

    data class Post(
        val texto: String,
        val imagenRes: Int?,
        val esPublico: Boolean,
        val explicacion: String,
        val difficulty: Difficulty
    )

    private val easyPosts = listOf(
        Post(
            "Foto de mis vacaciones con geolocalización",
            R.drawable.ic_heroe,
            false,
            "Incluye geolocalización...",
            Difficulty.EASY
        ),
        Post(
            "Foto de mi mascota jugando",
            R.drawable.ic_heroe,
            true,
            "No revela...",
            Difficulty.EASY
        ),
        Post(
            "Mi hermana cumple hoy 10 años y está sola en casa",
            null,
            false,
            "Revela ubicación...",
            Difficulty.EASY
        ),
        Post(
            "Fui al parque y fue muy divertido",
            null,
            true,
            "No contiene datos sensibles.",
            Difficulty.EASY
        )
    )
    private val mediumPosts = listOf(
        Post(
            "Estoy en el aeropuerto a punto de irme de vacaciones!!",
            R.drawable.ic_heroe,
            false,
            "Oversharing...",
            Difficulty.MEDIUM
        ),
        Post("Amo a mi perro", R.drawable.ic_heroe, true, "No revela...", Difficulty.MEDIUM),
        Post("Hoy es el cumple de mi mejor amigo", null, false, "Revela...", Difficulty.MEDIUM),
        Post("Ayer fui al cine", null, true, "No contiene...", Difficulty.MEDIUM)
    )
    private val hardPosts = listOf(
        Post(
            "Esta es la direccion de mi casa y estoy solo",
            R.drawable.ic_heroe,
            false,
            "Oversharing...",
            Difficulty.HARD
        ),
        Post("Amo a mi familia", R.drawable.ic_heroe, true, "No revela...", Difficulty.HARD),
        Post(
            "Hoy es el cumple de mi hijo y esta en casa solo",
            null,
            false,
            "Revela...",
            Difficulty.HARD
        ),
        Post("Ayer fui al banco y retire 10000 euros", null, false, "No contiene...", Difficulty.HARD)
    )

    private var currentDifficulty = Difficulty.EASY
    private var posts = easyPosts

    private val ajustesEsperados = mapOf("Fotos" to false, "Ubicacion" to false, "Lista" to true)
    private val explicacionesAjustes = mapOf(
        "Fotos" to "Si dejas las fotos públicas...",
        "Ubicacion" to "Compartir ubicación...",
        "Lista" to "Mostrar tu lista..."
    )

    data class Solicitud(val nombre: String, val avatarRes: Int, val esConocido: Boolean)
    private val solicitudes = listOf(
        Solicitud("Ana Pérez", R.drawable.ic_heroe, true),
        Solicitud("Carlos Gómez", R.drawable.ic_heroe, false),
        Solicitud("María Ruiz", R.drawable.ic_heroe, true),
        Solicitud("Desconocido123", R.drawable.ic_actor, false)
    )

    private var idxPost = 0
    private var idxSolicitud = 0
    private var puntosFiltrar = 0
    private lateinit var timer: CountDownTimer

    // Motor de combate Match-3
    private val engine = Match3Engine(FILAS, COLS)
    private var selectedPos: Pos? = null

    private val KEY_IDX_POST = "idx_post"

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_IDX_POST, idxPost)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)

        if (savedInstanceState != null) {
            idxPost = savedInstanceState.getInt(KEY_IDX_POST)
        }

        setupUI()
        mostrarEtapa(Etapa.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::timer.isInitialized) {
            timer.cancel()
        }
        _binding = null
    }

    private fun setupUI() = with(binding) {
        // 1. INTRO
        val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        imgByteIntro3.startAnimation(fadeInAnimation)
        tvIntroNivel3.startAnimation(fadeInAnimation)

        // Play intro sound (liberar recurso al terminar)
        val playerIntro = MediaPlayer.create(requireContext(), R.raw.correct)
        playerIntro.setOnCompletionListener { it.release() }
        playerIntro.start()

        btnIntro3Siguiente.setOnClickListener {
            mostrarEtapa(Etapa.DETECT)
            iniciarDetect()
        }

        // 2. DETECT OVERSHARING
        cardPost.setDragAndDrop(
            onStart = { v ->
                v.startDragAndDrop(
                    ClipData.newPlainText("", ""),
                    View.DragShadowBuilder(v),
                    v,
                    0
                )
            },
            targets = listOf(targetPublico, targetPrivado),
            onDrop = ::handleDetectDrop
        )

        // 3. AJUSTES
        btnInfoFotos.setOnClickListener { mostrarDialogo("Fotos", explicacionesAjustes["Fotos"]!!) }
        btnInfoUbicacion.setOnClickListener { mostrarDialogo("Ubicación", explicacionesAjustes["Ubicacion"]!!) }
        btnInfoLista.setOnClickListener { mostrarDialogo("Lista", explicacionesAjustes["Lista"]!!) }
        btnInfoDificultad.setOnClickListener { mostrarDialogo("Dificultad", "Selecciona la dificultad del juego") }
        btnVerificarAjustes.setOnClickListener { evaluarAjustes() }

        switchDificultad.setOnCheckedChangeListener { _, isChecked ->
            currentDifficulty = if (isChecked) Difficulty.HARD else Difficulty.EASY
            posts = when (currentDifficulty) {
                Difficulty.EASY -> easyPosts
                Difficulty.MEDIUM -> mediumPosts
                Difficulty.HARD -> hardPosts
            }
            idxPost = 0
            iniciarDetect()
        }

        switchDificultad.text = "Dificultad: ${currentDifficulty.name}"

        // 4. FILTRAR SOLICITUDES: RecyclerView y botón
        rvSolicitudes.layoutManager = LinearLayoutManager(requireContext())
        rvSolicitudes.adapter = SolicitudAdapter(solicitudes) { acepta -> evalSolicitud(acepta) }

        btnFinalizarFiltrar.setOnClickListener {
            timer.cancel()
            mostrarResultadoFiltrar()
        }

        // 5. COMBATE MATCH-3: Botón pista
        btnPista.setOnClickListener { usarPista() }

        // 6. RECOMPENSA: Botón continuar
        btnRecompContinuar.setOnClickListener {
            // Navegar al siguiente fragmento/pantalla de juego
            // findNavController().navigate(R.id.action_nivel3_to_juegoFragment)
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
        tvPost.text = post.texto
        imgPost.isVisible = post.imagenRes != null
        post.imagenRes?.let { imgPost.setImageResource(it) }
        tvPostCounter.text = "Post: ${idxPost + 1}/${posts.size}"
    }

    private fun handleDetectDrop(targetId: Int) = with(binding) {
        val post = posts[idxPost]
        val correcto = (targetId == targetPublico.id) == post.esPublico

        val feedbackMessage = if (correcto) "¡Correcto!" else "Incorrecto…"
        val feedbackSound = if (correcto) R.raw.correct else R.raw.incorrect

        // Play sound y liberar recurso
        val player = MediaPlayer.create(requireContext(), feedbackSound)
        player.setOnCompletionListener { it.release() }
        player.start()

        // Animar color del CardView
        val colorFrom = ContextCompat.getColor(requireContext(), R.color.white)
        val colorTo = ContextCompat.getColor(
            requireContext(),
            if (correcto) R.color.correct_color else R.color.incorrect_color
        )
        ObjectAnimator.ofArgb(cardPost, "cardBackgroundColor", colorFrom, colorTo).apply {
            duration = 200
        }.start()

        // Animación acierto/fallo en el Card
        val animRes = if (correcto) R.anim.correct_answer else R.anim.incorrect_answer
        cardPost.startAnimation(AnimationUtils.loadAnimation(requireContext(), animRes))

        Snackbar.make(root, feedbackMessage, Snackbar.LENGTH_SHORT)
            .setAction("¿Por qué?") {
                mostrarDialogo("Explicación", post.explicacion)
            }
            .show()

        // Restaurar color tras 1 segundo
        root.postDelayed({
            ObjectAnimator.ofArgb(cardPost, "cardBackgroundColor", colorTo, colorFrom).apply {
                duration = 200
            }.start()
        }, 1000)

        idxPost++
        if (idxPost < posts.size) {
            mostrarPost()
        } else {
            iniciarAjustes()
        }
    }

    // --- AJUSTES ---
    private fun iniciarAjustes() = with(binding) {
        switchDificultad.text = "Dificultad: ${currentDifficulty.name}"
        switchFotos.isChecked = !ajustesEsperados["Fotos"]!!
        switchUbicacion.isChecked = !ajustesEsperados["Ubicacion"]!!
        switchLista.isChecked = ajustesEsperados["Lista"]!!

        tvAjustesCounter.text = "Configuración: 0/3"

        switchDificultad.isChecked = currentDifficulty == Difficulty.HARD
        switchDificultad.text = "Dificultad: ${currentDifficulty.name}"

        mostrarEtapa(Etapa.AJUSTES)
    }

    private fun evaluarAjustes() = with(binding) {
        val okF = switchFotos.isChecked != ajustesEsperados["Fotos"]!!
        val okU = switchUbicacion.isChecked != ajustesEsperados["Ubicacion"]!!
        val okL = switchLista.isChecked == ajustesEsperados["Lista"]!!
        val allOk = listOf(okF, okU, okL).all { it }

        Snackbar.make(
            root,
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
        idxSolicitud = 0
        puntosFiltrar = 0
        binding.btnFinalizarFiltrar.isEnabled = false

        // Iniciar temporizador
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

    private fun evalSolicitud(acepta: Boolean) = with(binding) {
        val sol = solicitudes[idxSolicitud]
        val correcto = (acepta == sol.esConocido)

        val feedbackMessage = if (correcto) "¡Correcto!" else "Incorrecto"
        val feedbackSound = if (correcto) R.raw.correct else R.raw.incorrect

        // Play sound y liberar recurso
        val player = MediaPlayer.create(requireContext(), feedbackSound)
        player.setOnCompletionListener { it.release() }
        player.start()

        // Animación de la fila actual
        rvSolicitudes.findViewHolderForAdapterPosition(idxSolicitud)?.itemView?.apply {
            val anim = AnimationUtils.loadAnimation(
                context,
                if (correcto) R.anim.correct_answer else R.anim.incorrect_answer
            )
            startAnimation(anim)
        }

        Snackbar.make(root, feedbackMessage, Snackbar.LENGTH_SHORT).show()

        if (correcto) puntosFiltrar++
        idxSolicitud++
        if (idxSolicitud >= solicitudes.size) {
            btnFinalizarFiltrar.isEnabled = true
            timer.cancel()
        } else {
            rvSolicitudes.apply {
                adapter?.notifyItemChanged(idxSolicitud - 1)
                scrollToPosition(idxSolicitud)
            }
        }
    }

    private fun mostrarResultadoFiltrar() = with(binding) {
        Snackbar.make(root, "Filtrado: $puntosFiltrar/${solicitudes.size}", Snackbar.LENGTH_LONG)
            .setAction("Continuar") {
                iniciarCombateMatch3()
            }
            .show()
    }

    // --- COMBATE MATCH-3 ---
    private fun iniciarCombateMatch3() = with(binding) {
        engine.reset(OBJETIVO_HP, MOVS_INICIALES)
        selectedPos = null

        // Configurar tablero
        gvTablero.apply {
            rowCount = FILAS
            columnCount = COLS
            removeAllViews()
        }
        tvObjetivoCombate.text = "Inflige $OBJETIVO_HP HP en ≤ $MOVS_INICIALES movimientos"
        tvMovimientosRestantes.text = "Mov: $MOVS_INICIALES"

        // Inicializar ProgressBars (si están proclives)
        pbVidaEnemigo.max = OBJETIVO_HP
        pbEscudoJugador.max = OBJETIVO_HP
        pbVidaEnemigo.progress = engine.vidaEnemigo
        pbEscudoJugador.progress = engine.escudoJugador

        // Dibujar fichas
        drawBoard()

        mostrarEtapa(Etapa.COMBATE)
    }

    private fun drawBoard() {
        val boardView = binding.gvTablero
        boardView.removeAllViews()

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
                    width = 0
                    height = 0
                    setMargins(4, 4, 4, 4)
                }
                boardView.addView(iv, lp)
            }
        }
    }

    private fun onTileClick(pos: Pos, view: View) {
        if (selectedPos == null) {
            selectedPos = pos
            view.alpha = 0.5f
        } else {
            val first = selectedPos!!
            // Restaurar alpha de la tile seleccionada si no es swap válido
            if (!engine.canSwap(first, pos)) {
                binding.gvTablero.getChildAt(first.first * COLS + first.second).alpha = 1f
                selectedPos = null
                return
            }
            view.alpha = 1f
            animateSwap(first, pos)
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
                // Swap de vuelta si no hay match
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

        // Animar fade-out de las fichas que coinciden
        val animations = matches.map { (r, c) ->
            binding.gvTablero.getChildAt(r * COLS + c)
                .animate().alpha(0f).setDuration(200)
        }
        animations.forEach { it.start() }

        // Tras la animación, limpiar y actualizar
        binding.root.postDelayed({
            engine.clearMatches(matches)
            // Si quisieras gravedad/colapso, implementa collapse() y refill() aquí
            engine.collapse()
            engine.refill()
            actualizarBoard()
            actualizarBarras()
            handleMatchesStep() // Repetir hasta no hallar más matches
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
        pbVidaEnemigo.progress = engine.vidaEnemigo
        pbEscudoJugador.progress = engine.escudoJugador
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

    // Extensión para arrastrar y soltar en un CardView
    private fun CardView.setDragAndDrop(
        onStart: (View) -> Unit,
        targets: List<View>,
        onDrop: (targetId: Int) -> Unit
    ) {
        setOnLongClickListener { view ->
            onStart(view)
            true
        }
        val listener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP) {
                onDrop(v.id)
            }
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
            repeat(rows) { r ->
                repeat(cols) { c ->
                    board[r][c] = Tile(randomType())
                }
            }
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

        fun canSwap(a: Pos, b: Pos) =
            abs(a.first - b.first) + abs(a.second - b.second) == 1

        fun swap(a: Pos, b: Pos): Boolean {
            board[a.first][a.second].also {
                board[a.first][a.second] = board[b.first][b.second]
                board[b.first][b.second] = it
            }
            val ok = findMatches().isNotEmpty()
            if (ok) {
                movimientosRestantes--
            } else {
                // Swap de vuelta
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
                    if (board[r][c].type == board[r][c - 1].type) streak++
                    else {
                        if (streak >= 3) repeat(streak) { k ->
                            found += (r to (c - 1 - k))
                        }
                        streak = 1
                    }
                }
                if (streak >= 3) repeat(streak) { k ->
                    found += (r to (cols - 1 - k))
                }
            }
            // verticales
            for (c in 0 until cols) {
                var streak = 1
                for (r in 1 until rows) {
                    if (board[r][c].type == board[r - 1][c].type) streak++
                    else {
                        if (streak >= 3) repeat(streak) { k ->
                            found += ((r - 1 - k) to c)
                        }
                        streak = 1
                    }
                }
                if (streak >= 3) repeat(streak) { k ->
                    found += ((rows - 1 - k) to c)
                }
            }
            return found
        }

        fun clearMatches(matches: Set<Pos>) {
            matches.forEach { (r, c) ->
                when (board[r][c].type) {
                    Type.ROJO -> vidaEnemigo = (vidaEnemigo - 10).coerceAtLeast(0)
                    else -> escudoJugador = (escudoJugador + 5).coerceAtMost(OBJETIVO_HP)
                }
                board[r][c] = Tile(randomType())
            }
        }

        fun collapse() = Unit   // Si quieres gravedad, implementa aquí
        fun refill() = Unit     // Si quieres rellenar por arriba, implementa aquí
        fun useHint() = findMatches().firstOrNull()
    }

    // Adapter para la lista de solicitudes
    inner class SolicitudAdapter(
        private val items: List<Solicitud>,
        private val cb: (Boolean) -> Unit
    ) : RecyclerView.Adapter<SolicitudAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val ivAvatar = v.findViewById<ImageView>(R.id.ivAvatar)
            private val tvNombre = v.findViewById<TextView>(R.id.tvNombre)
            private val btnA = v.findViewById<View>(R.id.btnAceptar)
            private val btnR = v.findViewById<View>(R.id.btnRechazar)
            fun bind(s: Solicitud) {
                ivAvatar.setImageResource(s.avatarRes)
                tvNombre.text = s.nombre
                btnA.setOnClickListener { cb(true) }
                btnR.setOnClickListener { cb(false) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_solicitud, parent, false)
            )

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size
    }
}
