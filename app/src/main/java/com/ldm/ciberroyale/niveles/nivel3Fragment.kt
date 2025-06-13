package com.ldm.ciberroyale.niveles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel3Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    private var _binding: FragmentNivel3Binding? = null
    private val binding get() = _binding!!

    private enum class Screen { INTRO, CONTROL, GAME1, GAME2, GAME3, RECOMP }
    private var currentScreen = Screen.INTRO
    private var doneGame1 = false
    private var doneGame2 = false
    private var doneGame3 = false

    companion object {
        private const val TARGET_PASSWORD = "BYTE-SEGURA-2025"
    }

    // Juego 2
    private val statementsOrig = listOf(
        "Tu ubicación siempre está oculta por defecto en redes sociales." to false,
        "Es seguro publicar tu correo electrónico en redes públicas." to false,
        "Puedes revisar quién visitó tu perfil (en la mayoría de apps)." to false,
        "Los mensajes directos privados los lee solo el destinatario." to true,
        "Los ajustes de privacidad pueden cambiarse en cualquier momento." to true
    )
    private lateinit var statements: MutableList<Pair<String, Boolean>>
    private val pending2 = mutableListOf<Int>()

    // Juego 3: Memory con GridLayout
    private val emojis = listOf("👁", "👁", "📸", "📸", "👥", "👥", "🔒", "🔒", "🔓", "🔓", "💬", "💬")
    private lateinit var cardViews: List<View>
    private var firstSelected = -1
    private var matchedCount = 0
    private val revealed = mutableSetOf<Int>()
    private val matched = mutableSetOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)

        savedInstanceState?.let {
            doneGame1 = it.getBoolean("done1", false)
            doneGame2 = it.getBoolean("done2", false)
            doneGame3 = it.getBoolean("done3", false)
            currentScreen = Screen.valueOf(it.getString("screen") ?: "INTRO")
        }

        setupListeners()
        initGame2()
        setupMemory()
        refreshGame1State()
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
        btnIntroSiguiente.setOnClickListener { switchTo(Screen.CONTROL) }
        btnGame1.setOnClickListener { switchTo(Screen.GAME1) }
        btnGame2.setOnClickListener { switchTo(Screen.GAME2) }
        btnGame3.setOnClickListener { switchTo(Screen.GAME3) }
        btnInfoSala.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.control_title))
                .setMessage(getString(R.string.info_mensaje_control))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }
        btnCheckPassword.setOnClickListener {
            val input = etPassword.text.toString().trim()
            if (input.equals(TARGET_PASSWORD, ignoreCase = true)) {
                tvFinalPassword.text = TARGET_PASSWORD
                switchTo(Screen.RECOMP)
            } else {
                etPassword.error = getString(R.string.error_password)
            }
        }
        listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts)
            .forEach { tg ->
                tg.addOnButtonCheckedListener { _, _, _ -> refreshGame1State() }
            }
        btnInfoGame1.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game1_title))
                .setMessage(getString(R.string.info_game1))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }
        btnCompleteGame1.setOnClickListener {
            doneGame1 = true
            listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts).forEach { it.isEnabled = false }
            btnCompleteGame1.isEnabled = false
            tvGame1Piece.isVisible = true
            Snackbar.make(root, R.string.game1_piece, Snackbar.LENGTH_SHORT).show()
            updateControlState()
        }
        btnBackGame1.setOnClickListener { switchTo(Screen.CONTROL) }
        btnInfoGame2.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game2_title))
                .setMessage(getString(R.string.info_game2))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }
        btnTrue.setOnClickListener { processAnswer2(true) }
        btnFalse.setOnClickListener { processAnswer2(false) }
        btnBackGame2.setOnClickListener { switchTo(Screen.CONTROL) }
        btnInfoGame3.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game3_title))
                .setMessage(getString(R.string.info_game3))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }
        btnBackGame3.setOnClickListener { switchTo(Screen.CONTROL) }
        btnFinish.setOnClickListener {
            findNavController().navigate(R.id.action_nivel3Fragment_to_juegoFragment)
        }
    }

    private fun switchTo(screen: Screen) {
        currentScreen = screen
        showScreen(screen)
        updateControlState()
    }

    private fun refreshGame1State() = with(binding) {
        val photoOk = togglePhoto.checkedButtonId == R.id.btnPhotoPublic
        val birthOk = toggleBirthdate.checkedButtonId == R.id.btnBirthdatePublic
        val friendsOk = toggleFriends.checkedButtonId == R.id.btnFriendsPublic
        val postsOk = togglePosts.checkedButtonId == R.id.btnPostsPublic
        btnCompleteGame1.isEnabled = photoOk && !birthOk && !friendsOk && postsOk && !doneGame1
    }

    private fun initGame2() {
        statements = statementsOrig.shuffled().toMutableList()
        pending2.clear()
        pending2 += statements.indices
        if (pending2.isNotEmpty()) loadStatement()
    }

    private fun loadStatement() {
        binding.tvStatement.text = statements[pending2.first()].first
    }

    private fun processAnswer2(isTrue: Boolean) {
        if (doneGame2 || pending2.isEmpty()) return
        val idx = pending2.removeAt(0)
        val correct = statements[idx].second == isTrue
        if (!correct) pending2.add(idx)
        Snackbar.make(binding.root,
            if (correct) R.string.correcto else R.string.incorrecto,
            Snackbar.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {
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

    // — Juego 3: Memory con GridLayout (VERSIÓN CORREGIDA) —

    private fun setupMemory() {
        val deck = emojis.shuffled()
        val container = binding.gridLayoutGame3
        container.removeAllViews()

        firstSelected = -1
        matchedCount = 0
        revealed.clear()
        matched.clear()

        cardViews = deck.mapIndexed { index, emoji ->
            val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_memory_card, container, false)
            card.findViewById<TextView>(R.id.tvEmojiCard).text = emoji

            val row = index / container.columnCount
            val col = index % container.columnCount
            card.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(row, 1, 1f),
                GridLayout.spec(col, 1, 1f)
            ).apply {
                width = 0
                height = 0
                setMargins(8, 8, 8, 8)
            }

            // --- CORRECCIÓN CLAVE ---
            // Establecer el estado inicial directamente en la vista 'card',
            // en lugar de llamar a flipCard() cuando cardViews aún no está inicializada.
            card.findViewById<ImageView>(R.id.ivBack).visibility = View.VISIBLE
            card.findViewById<TextView>(R.id.tvEmojiCard).visibility = View.GONE
            // --- FIN DE LA CORRECCIÓN ---

            card.setOnClickListener {
                if (matched.contains(index) || revealed.contains(index) || !it.isClickable) return@setOnClickListener

                flipCard(index, showEmoji = true)
                revealed.add(index)

                if (firstSelected < 0) {
                    firstSelected = index
                } else {
                    setBoardClickable(false)
                    val secondSelected = index
                    val isMatch = deck[firstSelected] == deck[secondSelected]

                    lifecycleScope.launch {
                        delay(500.milliseconds)
                        if (isMatch) {
                            matched.add(firstSelected)
                            matched.add(secondSelected)
                            matchedCount++
                            checkGameCompletion(deck.size)
                        } else {
                            revealed.remove(firstSelected)
                            revealed.remove(secondSelected)
                            flipCard(firstSelected, showEmoji = false)
                            flipCard(secondSelected, showEmoji = false)
                        }
                        firstSelected = -1
                        setBoardClickable(true)
                    }
                }
            }
            container.addView(card)
            card
        }
    }

    private fun flipCard(index: Int, showEmoji: Boolean) {
        val card = cardViews[index] // Ahora esto es seguro, se llama después de la inicialización.
        card.findViewById<ImageView>(R.id.ivBack).visibility = if (showEmoji) View.GONE else View.VISIBLE
        card.findViewById<TextView>(R.id.tvEmojiCard).visibility = if (showEmoji) View.VISIBLE else View.GONE
    }

    private fun setBoardClickable(isClickable: Boolean) {
        if (::cardViews.isInitialized) { // Añadimos una comprobación de seguridad extra
            cardViews.forEach { it.isClickable = isClickable }
        }
    }

    private fun checkGameCompletion(deckSize: Int) {
        if (matchedCount == deckSize / 2) {
            doneGame3 = true
            binding.tvGame3Piece.isVisible = true
            updateControlState()
            Snackbar.make(binding.root, R.string.game3_piece, Snackbar.LENGTH_SHORT).show()
        }
    }

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
            Screen.INTRO -> pantallaIntro.isVisible = true
            Screen.CONTROL -> pantallaControl.isVisible = true
            Screen.GAME1 -> pantallaGame1.isVisible = true
            Screen.GAME2 -> pantallaGame2.isVisible = true
            Screen.GAME3 -> pantallaGame3.isVisible = true
            Screen.RECOMP -> pantallaRecomp.isVisible = true
        }
    }
}