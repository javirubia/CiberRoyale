package com.ldm.ciberroyale

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ldm.ciberroyale.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.Exception

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var linkLauncher: ActivityResultLauncher<Intent>
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        auth = Firebase.auth

        binding.tvHeaderTitle.text = getString(R.string.mi_cuenta)

        setupGoogleSignInLaunchers()
        setupListeners()
        actualizarUI()
    }
    override fun onResume() {
        super.onResume()
        // Mantenemos la música del menú
        SoundManager.playMusic(requireContext(), R.raw.music_menu)
    }

    override fun onPause() {
        super.onPause()
        SoundManager.stopMusic()
    }

    private fun setupGoogleSignInLaunchers() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        linkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data, isLinking = true)
            }
        }

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleGoogleSignInResult(result.data, isLinking = false)
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?, isLinking: Boolean) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)

            // La gestión del ProgressBar se hace ahora dentro de cada función con corrutina
            if (isLinking) {
                linkFirebaseWithGoogle(credential)
            } else {
                signInWithGoogle(credential)
            }

        } catch (e: ApiException) {
            // Manejo de errores mejorado para dar feedback más útil al usuario
            val message = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Has cancelado la operación."
                CommonStatusCodes.NETWORK_ERROR -> "Error de red. Revisa tu conexión."
                else -> "Error con la autenticación de Google. (Código: ${e.statusCode})"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            findNavController().navigate(R.id.action_loginFragment_to_menuFragment)
        }
        binding.btnVincularGoogle.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            linkLauncher.launch(googleSignInClient.signInIntent)
        }
        binding.btnIniciarSesionGoogle.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
        binding.btnCerrarSesion.setOnClickListener {
            SoundManager.playSfx(R.raw.sfx_button_click) // <-- AÑADIDO
            mostrarDialogoDeCierreDeSesion()
        }
    }
    private fun actualizarUI() {
        val user = auth.currentUser ?: return
        if (user.isAnonymous) {
            binding.cardAnonimo.isVisible = true
            binding.cardPermanente.isVisible = false
        } else {
            binding.cardAnonimo.isVisible = false
            binding.cardPermanente.isVisible = true
            binding.tvInfoEmail.text = getString(R.string.sesion_iniciada_como, user.email.orEmpty())
        }
    }



    private fun linkFirebaseWithGoogle(credential: AuthCredential) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                // Intenta vincular la cuenta anónima actual
                auth.currentUser?.linkWithCredential(credential)?.await()

                // Si llegamos aquí, la vinculación fue un éxito (era una cuenta de Google nueva)
                Toast.makeText(requireContext(), "¡Progreso protegido con éxito!", Toast.LENGTH_SHORT).show()
                ProgresoManager.saveProgressToFirestore() // Guardamos el progreso del invitado en la nueva cuenta
                actualizarUI()

            } catch (e: Exception) {
                // AQUÍ ESTÁ LA MAGIA: Comprobamos si el error es porque la cuenta ya existe
                if (e is FirebaseAuthUserCollisionException) {
                    // La cuenta de Google ya está en uso. Preguntamos al usuario qué hacer.
                    AlertDialog.Builder(requireContext())
                        .setTitle("Cuenta ya existente")
                        .setMessage("Esa cuenta de Google ya tiene progreso guardado. ¿Quieres abandonar el progreso de invitado actual y cargar tu partida guardada?")
                        .setPositiveButton("Sí, cargar mi cuenta") { _, _ ->
                            // El usuario quiere iniciar sesión. Llamamos a la función de signIn.
                            // Esto descartará al usuario anónimo y cargará el progreso de la nube.
                            signInWithGoogle(credential)
                        }
                        .setNegativeButton("Cancelar", null) // El usuario cancela, no se hace nada.
                        .show()
                } else {
                    // Otro tipo de error (red, etc.)
                    Toast.makeText(requireContext(), "Ha ocurrido un error inesperado al vincular la cuenta.", Toast.LENGTH_LONG).show()
                }
            } finally {
                // El ProgressBar siempre se oculta, pase lo que pase.
                setLoading(false)
            }
        }
    }

    /**
     * Inicia sesión con una credencial de Google, reemplazando al usuario anónimo.
     */
    private fun signInWithGoogle(credential: AuthCredential) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                // Intenta iniciar sesión y ESPERA a que la operación termine
                auth.signInWithCredential(credential).await()
                Toast.makeText(requireContext(), "¡Bienvenido/a de vuelta!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()

            } catch (e: Exception) {
                // Captura cualquier error durante el inicio de sesión
                Toast.makeText(requireContext(), "Error al iniciar sesión.", Toast.LENGTH_LONG).show()
            } finally {
                // Se asegura de que el ProgressBar siempre se oculte
                setLoading(false)
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual, guarda su progreso en la nube y crea una nueva sesión anónima.
     * El proceso es secuencial y a prueba de fallos gracias a las corrutinas.
     */
    private fun cerrarSesion() {
        lifecycleScope.launch {
            setLoading(true)
            try {
                // Paso 1: Guarda el progreso y ESPERA a que la tarea termine.
                ProgresoManager.saveProgressToFirestore()

                // Paso 2: Ahora que el guardado ha terminado, borra el progreso local de forma segura.
                ProgresoManager.clearLocalProgress()

                // Paso 3: Cierra sesión en Firebase Auth y Google Client, esperando a que cada uno termine.
                auth.signOut()
                googleSignInClient.signOut().await()

                // Paso 4: Vuelve a iniciar sesión como anónimo y espera.
                auth.signInAnonymously().await()

                // Paso 5: Si todo ha ido bien, informa al usuario y actualiza la UI.
                Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                actualizarUI()

            } catch (e: Exception) {
                // Si CUALQUIER paso de arriba falla, el flujo salta aquí.
                Toast.makeText(context, "Error al cerrar sesión. Revisa tu conexión.", Toast.LENGTH_LONG).show()
            } finally {
                // Este bloque se ejecuta siempre, asegurando que la barra de progreso desaparezca.
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnVincularGoogle.isEnabled = !isLoading
        binding.btnIniciarSesionGoogle.isEnabled = !isLoading
        binding.btnCerrarSesion.isEnabled = !isLoading
    }

    private fun mostrarDialogoDeCierreDeSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro? Tu progreso se guardará en la nube antes de salir.")
            .setPositiveButton("Sí, cerrar sesión") { _, _ -> cerrarSesion() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
