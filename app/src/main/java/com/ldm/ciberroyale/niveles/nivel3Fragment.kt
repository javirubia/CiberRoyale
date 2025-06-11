package com.ldm.ciberroyale.niveles

import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel3Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.google.android.material.snackbar.Snackbar


class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    private var _binding: FragmentNivel3Binding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TARGET_PASSWORD = "BYTE-SEGURA-2025"
    }

    private enum class Screen { INTRO, CONTROL, GAME1, GAME2, GAME3, RECOMP }

    // estado actual
    private var currentScreen = Screen.INTRO

    private var doneGame1 = false
    private var doneGame2 = false
    private var doneGame3 = false

    // — MEJORA — listas barajadas
    private val statementsOrig = listOf(
        "Tu ubicación siempre está oculta por defecto en redes sociales."  to false,
        "Es seguro publicar tu correo electrónico en redes públicas."      to false,
        "Puedes revisar quién visitó tu perfil (en la mayoría de apps)."   to false,
        "Los mensajes directos privados los lee solo el destinatario."     to true,
        "Los ajustes de privacidad pueden cambiarse en cualquier momento." to true
    )
    private lateinit var statements: MutableList<Pair<String, Boolean>>
    private val pending2 = mutableListOf<Int>()

    private val contentsOrig = listOf(
        "👁 Ver tu ubicación"          to false,
        "📸 Publicar foto"             to true,
        "👥 Mostrar lista de amigos"   to false,
        "🔒 Establecer perfil privado" to true
    )
    private lateinit var contents: MutableList<Pair<String, Boolean>>
    private val pending3 = mutableListOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)

        // Restaurar estado tras rotación
        savedInstanceState?.let {
            doneGame1       = it.getBoolean("done1", false)
            doneGame2       = it.getBoolean("done2", false)
            doneGame3       = it.getBoolean("done3", false)
            currentScreen   = Screen.valueOf(it.getString("screen") ?: "INTRO")
        }

        setupListeners()
        initGame2()
        initGame3()
        refreshGame1State()                           // — MEJORA — inicializa estado botón
        showScreen(currentScreen)
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putBoolean("done1", doneGame1)
        out.putBoolean("done2", doneGame2)
        out.putBoolean("done3", doneGame3)
        out.putString("screen", currentScreen.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // INTRO
        btnIntroSiguiente.setOnClickListener {
            currentScreen = Screen.CONTROL
            showScreen(Screen.CONTROL)
        }

        // CONTROL
        btnGame1.setOnClickListener { switchToGame(Screen.GAME1) }
        btnGame2.setOnClickListener { switchToGame(Screen.GAME2) }
        btnGame3.setOnClickListener { switchToGame(Screen.GAME3) }

        btnCheckPassword.setOnClickListener {
            val input = etPassword.text.toString().trim()
            if (input.equals(TARGET_PASSWORD, ignoreCase = true)) {
                tvFinalPassword.text = TARGET_PASSWORD
                switchToGame(Screen.RECOMP)
            } else {
                etPassword.error = getString(R.string.error_password)
            }
        }

        // JUEGO 1
        listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts)
            .forEach { tg ->
                tg.addOnButtonCheckedListener { _, _, _ ->
                    refreshGame1State()
                }
            }

        btnInfoGame1.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game1_title))
                .setMessage(getString(R.string.info_game1))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        btnCompleteGame1.setOnClickListener {
            doneGame1 = true
            listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts)
                .forEach { it.isEnabled = false }
            btnCompleteGame1.isEnabled = false
            binding.tvGame1Piece.isVisible = true
            Snackbar.make(root, getString(R.string.game1_piece), Snackbar.LENGTH_SHORT).show() // — MEJORA — Snackbar
            updateControlState()
        }

        btnBackGame1.setOnClickListener { switchToGame(Screen.CONTROL) }

        // JUEGO 2
        btnInfoGame2.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game2_title))
                .setMessage(getString(R.string.info_game2))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }
        btnTrue.setOnClickListener  { processAnswer2(true) }
        btnFalse.setOnClickListener { processAnswer2(false) }
        btnBackGame2.setOnClickListener { switchToGame(Screen.CONTROL) }

        // JUEGO 3
        btnInfoGame3.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game3_title))
                .setMessage(getString(R.string.info_game3))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        binding.cardEmoji.setOnLongClickListener { v ->
            v.startDragAndDrop(null, View.DragShadowBuilder(v), v, 0)
            true
        }

        val dropListener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP && !doneGame3) {
                processDrop3(v.id)
            }
            true
        }
        targetPublic3.setOnDragListener(dropListener)
        targetPrivate3.setOnDragListener(dropListener)
        btnBackGame3.setOnClickListener { switchToGame(Screen.CONTROL) }

        // RECOMPENSA
        btnFinish.setOnClickListener {
            findNavController().navigate(R.id.action_nivel3Fragment_to_juegoFragment)
        }
    }

    // — MEJORA — Helper para cambiar de pantalla
    private fun switchToGame(screen: Screen) {
        currentScreen = screen
        showScreen(screen)
        updateControlState()
    }

    // — MEJORA — Juego1: refresca estado del botón
    private fun refreshGame1State() = with(binding) {
        val photoOk   = (togglePhoto.checkedButtonId == R.id.btnPhotoPublic)
        val birthOk   = (toggleBirthdate.checkedButtonId == R.id.btnBirthdatePublic)
        val friendsOk = (toggleFriends.checkedButtonId == R.id.btnFriendsPublic)
        val postsOk   = (togglePosts.checkedButtonId == R.id.btnPostsPublic)
        btnCompleteGame1.isEnabled = photoOk && !birthOk && !friendsOk && postsOk && !doneGame1
    }

    // — MEJORA — Juego2: inicializa y baraja
    private fun initGame2() {
        statements = statementsOrig.shuffled().toMutableList()
        pending2.clear()
        pending2 += statements.indices
        loadStatement()
    }

    private fun loadStatement() {
        val idx = pending2.first()
        binding.tvStatement.text = statements[idx].first
    }

    private fun processAnswer2(isTrue: Boolean) {
        if (doneGame2) return
        val idx = pending2.removeAt(0)
        val correct = statements[idx].second == isTrue
        if (!correct) pending2.add(idx)
        val msg = if (correct) R.string.correcto else R.string.incorrecto
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500.milliseconds)
            if (pending2.isEmpty()) {
                doneGame2 = true
                binding.btnTrue.isEnabled = false
                binding.btnFalse.isEnabled = false
                binding.tvGame2Piece.isVisible = true
                updateControlState()
                Snackbar.make(binding.root, R.string.game2_piece, Snackbar.LENGTH_SHORT).show()
            } else {
                loadStatement()
            }
        }
    }

    // — MEJORA — Juego3: inicializa barajado y accesibilidad
    private fun initGame3() {
        contents = contentsOrig.shuffled().toMutableList()
        pending3.clear()
        pending3 += contents.indices
        loadEmoji()
    }

    private fun loadEmoji() {
        val idx = pending3.first()
        binding.tvEmoji.text = contents[idx].first
        binding.tvEmoji.contentDescription = getString(
            R.string.desc_emoji_drag,
            contents[idx].first
        )
    }

    private fun processDrop3(viewId: Int) {
        val idx = pending3.removeAt(0)
        val droppedPublic = viewId == binding.targetPublic3.id
        val correct = contents[idx].second == droppedPublic
        Snackbar.make(binding.root, if (correct) R.string.correcto else R.string.incorrecto, Snackbar.LENGTH_SHORT).show()
        if (!correct) pending3.add(idx)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500.milliseconds)
            if (pending3.isEmpty()) {
                doneGame3 = true
                binding.tvGame3Piece.isVisible = true
                updateControlState()
                Snackbar.make(binding.root, R.string.game3_piece, Snackbar.LENGTH_SHORT).show()
            } else {
                loadEmoji()
            }
        }
    }

    // — MEJORA — habilita contraseña solo cuando todo esté completo
    private fun updateControlState() = with(binding) {
        if (doneGame1 && doneGame2 && doneGame3) {
            etPassword.isEnabled = true
            btnCheckPassword.isEnabled = true
        }
    }

    private fun showScreen(screen: Screen) = with(binding) {
        listOf(
            pantallaIntro, pantallaControl,
            pantallaGame1, pantallaGame2,
            pantallaGame3, pantallaRecomp
        ).forEach { it.isVisible = false }

        when (screen) {
            Screen.INTRO   -> pantallaIntro.isVisible = true
            Screen.CONTROL -> pantallaControl.isVisible = true
            Screen.GAME1   -> pantallaGame1.isVisible = true
            Screen.GAME2   -> pantallaGame2.isVisible = true
            Screen.GAME3   -> pantallaGame3.isVisible = true
            Screen.RECOMP  -> pantallaRecomp.isVisible = true
        }
    }
}

