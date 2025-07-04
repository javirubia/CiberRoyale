package com.ldm.ciberroyale

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.ldm.ciberroyale.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- TU CÓDIGO EXISTENTE ---
        // 1) Animación de entrada de botones
        val slideAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.btnJugar.startAnimation(slideAnim)
        binding.btnTemario.startAnimation(slideAnim.apply { startOffset = 100 })
        binding.btnLogros.startAnimation(slideAnim.apply { startOffset = 200 })

        // 2) Micro-feedback táctil y navegación
        binding.btnJugar.applyTouchFeedback {
            findNavController().navigate(R.id.action_menuFragment_to_juegoFragment)
        }
        binding.btnTemario.applyTouchFeedback {
            findNavController().navigate(R.id.action_menuFragment_to_temarioFragment)
        }
        binding.btnLogros.applyTouchFeedback {
            findNavController().navigate(R.id.action_menuFragment_to_logrosFragment)
        }

        binding.btnCuenta.applyIconTouchFeedback {
            findNavController().navigate(R.id.action_menuFragment_to_loginFragment)
        }
        binding.btnAjustes.applyIconTouchFeedback {
            findNavController().navigate(R.id.action_menuFragment_to_ajustesFragment)
        }

        // 3) Float animation para los Bytes
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        binding.byte1.startAnimation(floatAnim.apply { startOffset =   0 })
        binding.byte2.startAnimation(floatAnim.apply { startOffset =  400 })
        binding.byte3.startAnimation(floatAnim.apply { startOffset =  800 })

        // AÑADIDO: Llama a la función de actualización de UI la primera vez que se crea la vista
        actualizarUIConProgreso()
    }

    // AÑADIDO: Ciclo de vida onResume para empezar a escuchar el progreso
    override fun onResume() {
        super.onResume()
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
        ProgresoManager.attachProgressListener {
            // Este código se ejecutará cada vez que el progreso cambie en Firestore
            if (isAdded) { // Buena práctica: Asegurarse de que el Fragmento sigue activo
                actualizarUIConProgreso()
            }
        }
    }

    // AÑADIDO: Ciclo de vida onPause para dejar de escuchar
    override fun onPause() {
        super.onPause()
        // Para la música al salir del menú
        SoundManager.stopMusic()
        // Y desadjuntamos el listener
        ProgresoManager.detachProgressListener()
    }

    // AÑADIDO: Función para actualizar la UI
    /**
     * Esta función se encarga de actualizar los elementos visuales del menú
     * basándose en el progreso actual del jugador.
     */
    private fun actualizarUIConProgreso() {
        val nivelDesbloqueado = ProgresoManager.getNivelDesbloqueado()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Extensión para aplicar un pequeño pulsado (scale) y ejecutar la acción real.
     */
    private fun MaterialButton.applyTouchFeedback(action: () -> Unit) {
        setOnClickListener { view ->
            // Añadimos el sonido del clic aquí
            SoundManager.playSfx(R.raw.sfx_button_click)
            view.animate()
                .scaleX(0.95f).scaleY(0.95f)
                .setDuration(50)
                .withEndAction {
                    view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(50)
                        .withEndAction(action)
                        .start()
                }
                .start()
        }
    }

    private fun ImageButton.applyIconTouchFeedback(action: () -> Unit) {
        setOnClickListener { view ->
            // Y aquí también
            SoundManager.playSfx(R.raw.sfx_button_click)
            view.animate()
                .scaleX(0.85f).scaleY(0.85f)
                .setDuration(50)
                .withEndAction {
                    view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(50)
                        .withEndAction(action)
                        .start()
                }
                .start()
        }
    }
}