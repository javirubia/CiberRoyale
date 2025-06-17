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
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)
        auth = Firebase.auth

        setupGoogleSignIn()
        setupListeners()
        actualizarUI()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    linkFirebaseWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(context, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnVincularGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoDeCierreDeSesion()
        }
    }

    private fun actualizarUI() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            findNavController().popBackStack()
            return
        }

        if (currentUser.isAnonymous) {
            binding.layoutAnonimo.isVisible = true
            binding.layoutPermanente.isVisible = false
        } else {
            binding.layoutAnonimo.isVisible = false
            binding.layoutPermanente.isVisible = true
            binding.tvInfoEmail.text = "Sesión iniciada como: ${currentUser.email}"
        }
    }

    private fun linkFirebaseWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "¡Cuenta vinculada con éxito!", Toast.LENGTH_SHORT).show()
                    actualizarUI()
                } else {
                    Toast.makeText(requireContext(), "Error al vincular: esta cuenta de Google ya podría estar en uso.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun mostrarDialogoDeCierreDeSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión? Tu progreso está guardado en la nube y podrás volver a iniciar sesión con tu cuenta de Google.")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signInAnonymously().addOnCompleteListener {
                Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
                actualizarUI() // Volvemos a la vista de anónimo
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}