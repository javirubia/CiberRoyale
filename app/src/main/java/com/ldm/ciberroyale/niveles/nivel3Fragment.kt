package com.ldm.ciberroyale.niveles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.ldm.ciberroyale.ProgresoManager
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.SoundManager
import com.ldm.ciberroyale.databinding.FragmentNivel3Binding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    private var _binding: FragmentNivel3Binding? = null
    private val binding get() = _binding!!

    //region Estado del Nivel
    private enum class Screen { INTRO, CONTROL, GAME1, GAME2, GAME3, RECOMP }
    private var currentScreen = Screen.INTRO
    private var doneGame1 = false
    private var doneGame2 = false
    private var doneGame3 = false
    private var firstTryGame1 = true // Para el logro de "Estrategia Perfecta"

    // Juego 2
    private lateinit var statements: MutableList<Pair<String, Boolean>>
    private val pending2 = mutableListOf<Int>()
    private var mistakesGame2 = 0 // Para el logro sin errores

    // Juego 3
    private lateinit var cardViews: List<View>
    private var firstSelected = -1
    private var matchedCount = 0
    private val revealed = mutableSetOf<Int>()
    private val matched = mutableSetOf<Int>()
    //endregion

    //region Ciclo de Vida
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)

        savedInstanceState?.let {
            doneGame1 = it.getBoolean("done1", false)
            doneGame2 = it.getBoolean("done2", false)
            doneGame3 = it.getBoolean("done3", false)
            firstTryGame1 = it.getBoolean("firstTryGame1", true)
            mistakesGame2 = it.getInt("mistakesGame2", 0)
            currentScreen = Screen.valueOf(it.getString("screen") ?: "INTRO")
        }

        setupListeners()
        initGame2()
        setupMemory()
        updateControlState()
        showScreen(currentScreen)
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putBoolean("done1", doneGame1)
        out.putBoolean("done2", doneGame2)
        out.putBoolean("done3", doneGame3)
        out.putBoolean("firstTryGame1", firstTryGame1)
        out.putInt("mistakesGame2", mistakesGame2)
        out.putString("screen", currentScreen.name)
    }
    override fun onResume() {
        super.onResume()
        // Ponemos la música de juego
        SoundManager.playMusic(requireContext(), R.raw.music_ingame) // <-- AÑADIDO
    }

    override fun onPause() {
        super.onPause()
        // Paramos la música al salir
        SoundManager.stopMusic() // <-- AÑADIDO
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    //endregion

    //region Setup y Navegación
    private fun setupListeners() = with(binding) {
        btnIntroSiguiente.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); switchTo(Screen.CONTROL) }
        btnGame1.setOnClickListener { SoundManager.playSfx(R.raw.sfx_navigate); switchTo(Screen.GAME1) }
        btnGame2.setOnClickListener { SoundManager.playSfx(R.raw.sfx_navigate); switchTo(Screen.GAME2) }
        btnGame3.setOnClickListener { SoundManager.playSfx(R.raw.sfx_navigate); switchTo(Screen.GAME3) }

        btnInfoSala.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfoDialog(R.string.dialog_info_nivel3_control_titulo, R.string.dialog_info_nivel3_control_mensaje) }
        btnInfoGame1.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfoDialog(R.string.dialog_info_nivel3_game1_titulo, R.string.dialog_info_nivel3_game1_mensaje) }
        btnInfoGame2.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfoDialog(R.string.dialog_info_nivel3_game2_titulo, R.string.dialog_info_nivel3_game2_mensaje) }
        btnInfoGame3.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); showInfoDialog(R.string.dialog_info_nivel3_game3_titulo, R.string.dialog_info_nivel3_game3_mensaje) }

        btnCheckPassword.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); checkFinalPassword() }

        listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts).forEach { tg ->
            tg.addOnButtonCheckedListener { _, _, isChecked ->
                if(isChecked) SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
                firstTryGame1 = false
                refreshGame1State()
            }
        }
        btnCompleteGame1.setOnClickListener { completeGame1() }
        btnBackGame1.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); switchTo(Screen.CONTROL) }

        btnTrue.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); processAnswer2(true) }
        btnFalse.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); processAnswer2(false) }
        btnBackGame2.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); switchTo(Screen.CONTROL) }

        btnBackGame3.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); switchTo(Screen.CONTROL) }
        btnFinish.setOnClickListener { SoundManager.playSfx(R.raw.sfx_button_click); findNavController().navigate(R.id.action_nivel3Fragment_to_juegoFragment) }
    }

    private fun switchTo(screen: Screen) {
        currentScreen = screen
        showScreen(screen)
        updateControlState()
    }

    private fun showScreen(screen: Screen) = with(binding) {
        listOf(pantallaIntro, pantallaControl, pantallaGame1, pantallaGame2, pantallaGame3, pantallaRecomp)
            .forEach { it.isVisible = false }

        when (screen) {
            Screen.INTRO -> pantallaIntro.isVisible = true
            Screen.CONTROL -> pantallaControl.isVisible = true
            Screen.GAME1 -> { pantallaGame1.isVisible = true; refreshGame1State() }
            Screen.GAME2 -> pantallaGame2.isVisible = true
            Screen.GAME3 -> pantallaGame3.isVisible = true
            Screen.RECOMP -> pantallaRecomp.isVisible = true
        }
    }
    //endregion

    //region Lógica de los Minijuegos
    private fun refreshGame1State() = with(binding) {
        if (doneGame1) {
            listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts).forEach { it.isEnabled = false }
            btnCompleteGame1.isEnabled = false
            tvGame1Piece.isVisible = true
        } else {
            val photoOk = togglePhoto.checkedButtonId == R.id.btnPhotoPublic
            val birthOk = toggleBirthdate.checkedButtonId == R.id.btnBirthdatePrivate
            val friendsOk = toggleFriends.checkedButtonId == R.id.btnFriendsPrivate
            val postsOk = togglePosts.checkedButtonId == R.id.btnPostsPublic
            btnCompleteGame1.isEnabled = photoOk && birthOk && friendsOk && postsOk
        }
    }

    private fun completeGame1() {
        SoundManager.playSfx(R.raw.sfx_correct_answer) // <-- AÑADIDO
        if (firstTryGame1) {
            ProgresoManager.unlockAchievement(requireContext(), "NIVEL3_ESTRATEGIA_PERFECTA")
        }
        doneGame1 = true
        refreshGame1State()
        Snackbar.make(binding.root, R.string.nivel3_game1_pieza_obtenida, Snackbar.LENGTH_SHORT).show()
    }

    private fun initGame2() {
        statements = STATEMENTS.shuffled().toMutableList()
        pending2.clear()
        pending2.addAll(statements.indices)
        if(pending2.isNotEmpty() && !doneGame2) loadStatement()

        binding.btnTrue.isEnabled = !doneGame2
        binding.btnFalse.isEnabled = !doneGame2
        binding.tvGame2Piece.isVisible = doneGame2
    }

    private fun loadStatement() {
        binding.tvStatement.text = statements[pending2.first()].first
    }

    private fun processAnswer2(isTrue: Boolean) {
        if (doneGame2 || pending2.isEmpty()) return
        val idx = pending2.removeAt(0)
        val correct = statements[idx].second == isTrue
        if(correct) SoundManager.playSfx(R.raw.sfx_correct_answer) else SoundManager.playSfx(R.raw.sfx_wrong_answer) // <-- AÑADIDO

        val feedback = if (correct) R.string.common_respuesta_correcta else R.string.common_respuesta_incorrecta
        Snackbar.make(binding.root, feedback, Snackbar.LENGTH_SHORT).show()
        if (!correct) {
            pending2.add(idx)
            mistakesGame2++
        }
        lifecycleScope.launch {
            delay(500)
            if (pending2.isEmpty()) {
                onGame2Completed()
            } else {
                loadStatement()
            }
        }
    }

    private fun onGame2Completed() {
        doneGame2 = true
        SoundManager.playSfx(R.raw.sfx_correct_answer)
        binding.btnTrue.isEnabled = false
        binding.btnFalse.isEnabled = false
        binding.tvGame2Piece.isVisible = true
        updateControlState()
        Snackbar.make(binding.root, R.string.nivel3_game2_pieza_obtenida, Snackbar.LENGTH_SHORT).show()
    }

    //region Lógica Juego 3: Memory
    private fun setupMemory() {
        if(doneGame3) { // Si el juego ya está hecho, solo muestra la pieza.
            binding.gridLayoutGame3.isVisible = false
            binding.tvGame3Piece.isVisible = true
            return
        }

        val deck = EMOJIS.shuffled()
        val container = binding.gridLayoutGame3
        container.removeAllViews()

        firstSelected = -1
        matchedCount = 0
        revealed.clear()
        matched.clear()

        cardViews = deck.mapIndexed { index, emoji ->
            val card = LayoutInflater.from(requireContext()).inflate(R.layout.item_memory_card, container, false)
            val emojiView = card.findViewById<TextView>(R.id.tvEmojiCard)
            emojiView.text = emoji

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            ).apply {
                width = 0
                height = 0
                setMargins(8,8,8,8)
            }
            card.layoutParams = params

            flipCard(card, showEmoji = false)

            card.setOnClickListener {
                if (matched.contains(index) || revealed.contains(index) || !it.isClickable) return@setOnClickListener
                SoundManager.playSfx(R.raw.sfx_navigate)

                flipCard(card, showEmoji = true)
                revealed.add(index)

                if (firstSelected < 0) {
                    firstSelected = index
                } else {
                    setBoardClickable(false)
                    val secondSelected = index
                    val isMatch = deck[firstSelected] == deck[secondSelected]

                    lifecycleScope.launch {
                        delay(500)
                        if (isMatch) {
                            SoundManager.playSfx(R.raw.sfx_correct_answer)
                            matched.add(firstSelected)
                            matched.add(secondSelected)
                            revealed.remove(firstSelected)
                            revealed.remove(secondSelected)
                            matchedCount++
                            if (matchedCount == deck.size / 2) onGame3Completed()
                        } else {
                            SoundManager.playSfx(R.raw.sfx_wrong_answer)
                            revealed.remove(firstSelected)
                            revealed.remove(secondSelected)
                            flipCard(cardViews[firstSelected], showEmoji = false)
                            flipCard(cardViews[secondSelected], showEmoji = false)
                        }
                        firstSelected = -1
                        if (!doneGame3) setBoardClickable(true)
                    }
                }
            }
            container.addView(card)
            card
        }
    }

    private fun flipCard(card: View, showEmoji: Boolean) {
        card.findViewById<ImageView>(R.id.ivBack).isVisible = !showEmoji
        card.findViewById<TextView>(R.id.tvEmojiCard).isVisible = showEmoji
    }

    private fun setBoardClickable(isClickable: Boolean) {
        if (::cardViews.isInitialized) {
            cardViews.forEachIndexed { index, view ->
                if(!matched.contains(index)) view.isClickable = isClickable
            }
        }
    }


    private fun onGame3Completed() {
        doneGame3 = true
        SoundManager.playSfx(R.raw.sfx_correct_answer)
        binding.tvGame3Piece.isVisible = true
        updateControlState()
        Snackbar.make(binding.root, R.string.nivel3_game3_pieza_obtenida, Snackbar.LENGTH_SHORT).show()
    }
    //endregion

    //region Lógica de la Sala de Control y Final
    private fun updateControlState() = with(binding) {
        btnGame1.isActivated = doneGame1
        btnGame2.isActivated = doneGame2
        btnGame3.isActivated = doneGame3
        if (doneGame1 && doneGame2 && doneGame3) {
            etPassword.isEnabled = true
            btnCheckPassword.isEnabled = true
        }
    }


    private fun checkFinalPassword() = with(binding) {
        val input = etPassword.text.toString().trim()
        if (input.equals(TARGET_PASSWORD, ignoreCase = true)) {
            SoundManager.stopMusic() // <-- AÑADIDO
            SoundManager.playJingle(requireContext(), R.raw.music_victory) // <-- AÑADIDO

            ProgresoManager.unlockAchievement(requireContext(), "NIVEL3_COMPLETADO")
            ProgresoManager.desbloquearSiguienteNivel(3)
            tvFinalPassword.text = TARGET_PASSWORD
            switchTo(Screen.RECOMP)
        } else {
            SoundManager.playSfx(R.raw.sfx_wrong_answer) // <-- AÑADIDO
            etPassword.error = getString(R.string.common_error_password)
        }
    }

    private fun showInfoDialog(titleRes: Int, msgRes: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setMessage(msgRes)
            .setPositiveButton(R.string.common_dialog_entendido, null)
            .show()
    }
    //endregion

    //region Datos y Constantes del Nivel
    companion object {
        private const val TARGET_PASSWORD = "BYTE-SEGURA-2025"
        private val STATEMENTS = listOf(
            "Tu ubicación siempre está oculta por defecto en redes sociales." to false,
            "Es seguro publicar tu correo electrónico en perfiles públicos." to false,
            "Puedes revisar quién visitó tu perfil (en la mayoría de apps)." to false,
            "Los mensajes directos privados solo los puede leer el destinatario." to true,
            "Los ajustes de privacidad se pueden cambiar en cualquier momento." to true
        )
        private val EMOJIS = listOf("👁", "👁", "📸", "📸", "👥", "👥", "🔒", "🔒", "🔓", "🔓", "💬", "💬")
    }
    //endregion
}