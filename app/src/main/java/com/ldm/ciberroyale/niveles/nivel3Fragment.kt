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

class Nivel3Fragment : Fragment(R.layout.fragment_nivel3) {

    private var _binding: FragmentNivel3Binding? = null
    private val binding get() = _binding!!

    private enum class Screen { INTRO, CONTROL, GAME1, GAME2, GAME3, RECOMP }
    private var doneGame1 = false
    private var doneGame2 = false
    private var doneGame3 = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNivel3Binding.bind(view)
        setupListeners()
        showScreen(Screen.INTRO)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() = with(binding) {
        // ---- INTRO ----
        btnIntroSiguiente.setOnClickListener {
            showScreen(Screen.CONTROL)
        }

        // ---- CONTROL ROOM ----
        btnGame1.setOnClickListener { showScreen(Screen.GAME1) }
        btnGame2.setOnClickListener { showScreen(Screen.GAME2) }
        btnGame3.setOnClickListener { showScreen(Screen.GAME3) }

        btnCheckPassword.setOnClickListener {
            val input = etPassword.text.toString().trim()
            val target = listOf("BYTE", "SEGURA", "2025").joinToString("-")
            if (input.equals(target, ignoreCase = true)) {
                tvFinalPassword.text = target
                showScreen(Screen.RECOMP)
            } else {
                etPassword.error = getString(R.string.error_password)
            }
        }

        // ---- DESAFÍO 1: Privacidad de Perfil ----
        val correctPhotoPublic     = true
        val correctBirthdatePublic = false
        val correctFriendsPublic   = false
        val correctPostsPublic     = true

        fun refreshGame1State() {
            val photoOk   = (togglePhoto.checkedButtonId == R.id.btnPhotoPublic)    == correctPhotoPublic
            val birthOk   = (toggleBirthdate.checkedButtonId == R.id.btnBirthdatePublic) == correctBirthdatePublic
            val friendsOk = (toggleFriends.checkedButtonId == R.id.btnFriendsPublic)  == correctFriendsPublic
            val postsOk   = (togglePosts.checkedButtonId == R.id.btnPostsPublic)     == correctPostsPublic
            btnCompleteGame1.isEnabled = photoOk && birthOk && friendsOk && postsOk && !doneGame1
        }

        listOf(togglePhoto, toggleBirthdate, toggleFriends, togglePosts)
            .forEach { tg ->
                tg.addOnButtonCheckedListener { _, _, _ ->
                    refreshGame1State()
                }
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
            togglePhoto.isEnabled     = false
            toggleBirthdate.isEnabled = false
            toggleFriends.isEnabled   = false
            togglePosts.isEnabled     = false
            btnCompleteGame1.isEnabled = false
            tvGame1Piece.isVisible = true
            updateControlState()
            Toast.makeText(requireContext(), getString(R.string.game1_piece), Toast.LENGTH_SHORT).show()
        }

        btnBackGame1.setOnClickListener { showScreen(Screen.CONTROL) }

        // ---- DESAFÍO 2: Verdadero o Falso automático ----
        val statements = listOf(
            "Tu ubicación siempre está oculta por defecto en redes sociales."  to false,
            "Es seguro publicar tu correo electrónico en redes públicas."      to false,
            "Puedes revisar quién visitó tu perfil (en la mayoría de apps)."   to false,
            "Los mensajes directos privados los lee solo el destinatario."     to true,
            "Los ajustes de privacidad pueden cambiarse en cualquier momento." to true
        )
        val pending2 = statements.indices.toMutableList()

        fun loadStatement() {
            val idx = pending2.first()
            tvStatement.text = statements[idx].first
        }
        loadStatement()

        btnInfoGame2.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game2_title))
                .setMessage(getString(R.string.info_game2))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        fun processAnswer2(isTrue: Boolean) {
            if (doneGame2) return
            val idx = pending2.removeAt(0)
            val correct = statements[idx].second == isTrue
            if (!correct) pending2.add(idx)
            Toast.makeText(
                requireContext(),
                if (correct) getString(R.string.correcto) else getString(R.string.incorrecto),
                Toast.LENGTH_SHORT
            ).show()

            lifecycleScope.launch {
                delay(500.milliseconds)
                if (pending2.isEmpty()) {
                    doneGame2 = true
                    btnTrue.isEnabled      = false
                    btnFalse.isEnabled     = false
                    tvGame2Piece.isVisible = true
                    updateControlState()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.game2_piece),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    loadStatement()
                }
            }
        }

        btnTrue.setOnClickListener  { processAnswer2(true) }
        btnFalse.setOnClickListener { processAnswer2(false) }

        btnBackGame2.setOnClickListener { showScreen(Screen.CONTROL) }

        // ---- DESAFÍO 3: Rompecabezas de Emoticonos ----
        val contents = listOf(
            "👁 Ver tu ubicación"          to false,
            "📸 Publicar foto"             to true,
            "👥 Mostrar lista de amigos"   to false,
            "🔒 Establecer perfil privado" to true
        )
        val pending3 = contents.indices.toMutableList()

        fun loadEmoji() {
            val idx = pending3.first()
            tvEmoji.text = contents[idx].first
        }
        loadEmoji()

        btnInfoGame3.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game3_title))
                .setMessage(getString(R.string.info_game3))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        cardEmoji.setOnLongClickListener { v ->
            v.startDragAndDrop(null, View.DragShadowBuilder(v), v, 0)
            true
        }

        val dropListener = View.OnDragListener { v, event ->
            if (event.action == DragEvent.ACTION_DROP && !doneGame3) {
                val idx = pending3.removeAt(0)
                val droppedPublic = v.id == R.id.targetPublic3
                val correct = contents[idx].second == droppedPublic
                if (!correct) pending3.add(idx)
                Toast.makeText(
                    requireContext(),
                    if (correct) getString(R.string.correcto) else getString(R.string.incorrecto),
                    Toast.LENGTH_SHORT
                ).show()

                lifecycleScope.launch {
                    delay(500.milliseconds)
                    if (pending3.isEmpty()) {
                        doneGame3 = true
                        tvGame3Piece.isVisible = true
                        updateControlState()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.game3_piece),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        loadEmoji()
                    }
                }
                true
            } else true
        }

        targetPublic3.setOnDragListener(dropListener)
        targetPrivate3.setOnDragListener(dropListener)

        btnBackGame3.setOnClickListener { showScreen(Screen.CONTROL) }

        // ---- RECOMPENSA ----
        btnFinish.setOnClickListener {
            findNavController().navigate(R.id.action_nivel3Fragment_to_juegoFragment)
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
            Screen.CONTROL -> {
                pantallaControl.isVisible = true
                updateControlState()
            }
            Screen.GAME1   -> {
                pantallaGame1.isVisible = true
                if (doneGame1) {
                    togglePhoto.isEnabled     = false
                    toggleBirthdate.isEnabled = false
                    toggleFriends.isEnabled   = false
                    togglePosts.isEnabled     = false
                    btnCompleteGame1.isEnabled = false
                    tvGame1Piece.isVisible = true
                }
            }
            Screen.GAME2   -> {
                pantallaGame2.isVisible = true
                if (doneGame2) {
                    btnTrue.isEnabled      = false
                    btnFalse.isEnabled     = false
                    tvGame2Piece.isVisible = true
                }
            }
            Screen.GAME3   -> {
                pantallaGame3.isVisible = true
                if (doneGame3) {
                    // asegurar vista de pieza al reentrar
                    tvGame3Piece.isVisible = true
                }
            }
            Screen.RECOMP  -> pantallaRecomp.isVisible = true
        }
    }

    private fun updateControlState() = with(binding) {
        if (doneGame1 && doneGame2 && doneGame3) {
            etPassword.isEnabled = true
            btnCheckPassword.isEnabled = true
        }
    }
}
