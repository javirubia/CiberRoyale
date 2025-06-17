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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ldm.ciberroyale.databinding.FragmentLoginBinding

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

        setupGoogleSignInLaunchers()
        setupListeners()
        actualizarUI()
    }

    private fun setupGoogleSignInLaunchers() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail().build()
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

            setLoading(true)
            if (isLinking) {
                linkFirebaseWithGoogle(credential)
            } else {
                signInWithGoogle(credential)
            }
        } catch (e: ApiException) {
            setLoading(false)
            Toast.makeText(context, "Error con la autenticación de Google.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnVincularGoogle.setOnClickListener {
            linkLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.btnIniciarSesionGoogle.setOnClickListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoDeCierreDeSesion()
        }
    }

    private fun actualizarUI() {
        val user = auth.currentUser ?: return
        binding.layoutAnonimo.isVisible = user.isAnonymous
        binding.layoutPermanente.isVisible = !user.isAnonymous

        if (!user.isAnonymous) {
            binding.tvInfoEmail.text = "Sesión iniciada como:\n${user.email}"
        }
    }

    private fun linkFirebaseWithGoogle(credential: AuthCredential) {
        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "¡Progreso protegido con éxito!", Toast.LENGTH_SHORT).show()
                    // Al vincular, guardamos el progreso local en la nube por primera vez
                    ProgresoManager.saveProgressToFirestore()
                    setLoading(false)
                    actualizarUI()
                } else {
                    setLoading(false)
                    Toast.makeText(requireContext(), "Error: esta cuenta de Google ya está en uso.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithGoogle(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Al iniciar sesión, borramos el progreso local anterior (del anónimo)
                    ProgresoManager.clearLocalProgress()
                    // No hace falta cargar desde aquí, JuegoFragment lo hará con el listener
                    setLoading(false)
                    Toast.makeText(requireContext(), "¡Bienvenido/a de vuelta!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack() // Volvemos al menú de niveles
                } else {
                    setLoading(false)
                    Toast.makeText(requireContext(), "Error al iniciar sesión.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun cerrarSesion() {
        setLoading(true)
        ProgresoManager.saveProgressToFirestore()
        ProgresoManager.clearLocalProgress()

        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signInAnonymously().addOnCompleteListener {
                setLoading(false)
                Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                actualizarUI()
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