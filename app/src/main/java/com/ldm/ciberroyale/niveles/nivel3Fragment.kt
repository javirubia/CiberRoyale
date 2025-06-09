package com.ldm.ciberroyale.niveles

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ldm.ciberroyale.R
import com.ldm.ciberroyale.databinding.FragmentNivel3Binding

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
            btnCompleteGame1.isEnabled = photoOk && birthOk && friendsOk && postsOk
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
            tvGame1Piece.isVisible = true
            btnCompleteGame1.isEnabled = false
            updateControlState()
            Toast.makeText(requireContext(), getString(R.string.game1_piece), Toast.LENGTH_SHORT).show()
        }

        btnBackGame1.setOnClickListener { showScreen(Screen.CONTROL) }

        // ---- DESAFÍO 2: Verdadero o Falso ----
        val statements = listOf(
            "Tu ubicación siempre está oculta por defecto en redes sociales."          to false,
            "Es seguro publicar tu correo electrónico en redes públicas."              to false,
            "Puedes revisar quién visitó tu perfil (en la mayoría de apps)."           to false,
            "Los mensajes directos privados los lee solo el destinatario."             to true,
            "Los ajustes de privacidad pueden cambiarse en cualquier momento."         to true
        )
        var idx2 = 0
        var correct2 = 0

        fun loadStatement() {
            val (text, _) = statements[idx2]
            tvStatement.text = text
            btnNextGame2.isEnabled = false
        }
        // Inicializa primera afirmación
        loadStatement()

        btnInfoGame2.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.game2_title))
                .setMessage(getString(R.string.info_game2))
                .setPositiveButton(getString(R.string.entendido), null)
                .show()
        }

        btnTrue.setOnClickListener {
            val correct = statements[idx2].second
            if (correct) correct2++
            Toast.makeText(
                requireContext(),
                if (correct) getString(R.string.correcto) else getString(R.string.incorrecto),
                Toast.LENGTH_SHORT
            ).show()
            btnNextGame2.isEnabled = true
        }
        btnFalse.setOnClickListener {
            val correct = !statements[idx2].second
            if (correct) correct2++
            Toast.makeText(
                requireContext(),
                if (correct) getString(R.string.correcto) else getString(R.string.incorrecto),
                Toast.LENGTH_SHORT
            ).show()
            btnNextGame2.isEnabled = true
        }

        btnNextGame2.setOnClickListener {
            idx2++
            if (idx2 < statements.size) {
                loadStatement()
            } else {
                // Fin del desafío 2
                doneGame2 = true
                tvGame2Piece.isVisible = true
                btnNextGame2.isEnabled = false
                updateControlState()
                Toast.makeText(requireContext(), getString(R.string.game2_piece), Toast.LENGTH_SHORT).show()
            }
        }

        btnBackGame2.setOnClickListener { showScreen(Screen.CONTROL) }

        // ---- DESAFÍO 3 (pendiente) ----
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
            Screen.GAME1   -> pantallaGame1.isVisible = true
            Screen.GAME2   -> pantallaGame2.isVisible = true
            Screen.GAME3   -> pantallaGame3.isVisible = true
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
