package com.ldm.ciberroyale

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.ldm.ciberroyale.databinding.FragmentMenuBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

        // 1) Animación de entrada de botones
        val slide = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.btnJugar.startAnimation(slide)
        binding.btnTemario.startAnimation(slide.apply { startOffset = 100 })
        binding.btnLogros.startAnimation(slide.apply { startOffset = 200 })

        // 2) Micro-feedback táctil
        binding.btnJugar.applyTouchFeedback()
        binding.btnTemario.applyTouchFeedback()
        binding.btnLogros.applyTouchFeedback()

        // 3) Float animation para los Bytes
        val floatAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.float_up_down)
        // puedes retrasar cada uno para no tenerlos sincronizados:
        binding.byte1.startAnimation(floatAnim.apply { startOffset = 0 })
        binding.byte2.startAnimation(floatAnim.apply { startOffset = 800 })
        binding.byte3.startAnimation(floatAnim.apply { startOffset = 400 })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun MaterialButton.applyTouchFeedback() {
        setOnClickListener { view ->
            view.animate()
                .scaleX(0.95f).scaleY(0.95f)
                .setDuration(50)
                .withEndAction {
                    view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(50)
                        .withEndAction {
                            // Lanzamos la acción real después del feedback
                            when (id) {
                                R.id.btnJugar   -> findNavController().navigate(R.id.action_menuFragment_to_juegoFragment)
                                R.id.btnTemario -> findNavController().navigate(R.id.action_menuFragment_to_temarioFragment)
                                R.id.btnLogros  -> findNavController().navigate(R.id.action_menuFragment_to_logrosFragment)
                            }
                        }
                        .start()
                }.start()
        }
    }


}
