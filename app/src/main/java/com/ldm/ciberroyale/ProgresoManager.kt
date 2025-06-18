package com.ldm.ciberroyale


import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.Toast   // <-- AÑADE ESTA IMPORTACIÓN
object ProgresoManager {
    private const val PREFS_NAME = "CiberRoyaleProgreso"
    private lateinit var prefs: SharedPreferences
    private val db = Firebase.firestore

    private const val KEY_NIVEL_DESBLOQUEADO = "nivelDesbloqueado"
    private const val KEY_LOGROS_DESBLOQUEADOS = "logrosDesbloqueados"

    private var progressListener: ListenerRegistration? = null
    val allAchievements: List<Achievement> by lazy {
        // Tu lista completa de logros aquí
        listOf(
            Achievement("TEMA1_LEIDO", "Tema 1 completado", "Has leído todo lo relativo a Contraseñas Seguras", AchievementType.TEMA, 1, null, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("TEMA2_LEIDO", "Tema 2 completado", "Has leído todo lo relativo a Phishing", AchievementType.TEMA, 2, null, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("TEMA3_LEIDO", "Tema 3 completado", "Has leído todo lo relativo a Privacidad en RRSS", AchievementType.TEMA, 3, null, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("TEMA4_LEIDO", "Tema 4 completado", "Has leído todo lo relativo a Seguridad en Dispositivos Móviles", AchievementType.TEMA, 4, null, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL1_COMPLETADO", "Hacker Novato", "Has superado el Nivel 1.", AchievementType.NIVEL, 1, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL1_EXCELENTE", "Maestro de Contraseñas", "Has superado el Nivel 1 con más de 90 puntos.", AchievementType.NIVEL, 1, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL1_INTACTO", "Intocable", "Has derrotado al jefe del Nivel 1 sin recibir daño.", AchievementType.NIVEL, 1, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL2_COMPLETADO", "Anti-Phishing", "Has superado el Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL2_PERFECCIONISTA", "Ojo de Halcón", "Has acertado todos los mensajes y URLs en el Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL2_PESCADOR_PRO", "Pescador Pro", "Has pescado más de 15 Vipers en la batalla del Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL3_COMPLETADO", "Guardián de la Privacidad", "Has completado los 3 desafíos del Nivel 3.", AchievementType.NIVEL, 3, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL3_ESTRATEGIA_PERFECTA", "Estrategia Perfecta", "Has configurado la privacidad del perfil correctamente al primer intento.", AchievementType.NIVEL, 3, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL4_COMPLETADO", "Cerrajero Digital", "Has superado el Nivel 4.", AchievementType.NIVEL, 4, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL4_MENTE_MAESTRA", "Mente Maestra", "Has descifrado el PIN del Nivel 4 en 3 intentos o menos.", AchievementType.NIVEL, 4, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("JUEGO_COMPLETADO", "¡ViperWare Derrotado!", "Has completado los 4 niveles.", AchievementType.NIVEL, 5, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked)
        )
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadProgressFromPrefs()
    }

    fun attachProgressListener(onUpdate: () -> Unit) {
        detachProgressListener()
        val user = Firebase.auth.currentUser
        if (user == null || user.isAnonymous) {
            loadProgressFromPrefs()
            onUpdate()
            return
        }

        val docRef = db.collection("usuarios").document(user.uid)
        // 1) Fetch inicial
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                applySnapshotToPrefs(snapshot)
            } else {
                // No existía: guardamos el progreso local en la nube
                saveProgressToFirestore()
            }
            loadProgressFromPrefs()
            onUpdate()

            // 2) Listener continuo para futuras actualizaciones
            progressListener = docRef.addSnapshotListener { snap, err ->
                if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                applySnapshotToPrefs(snap)
                loadProgressFromPrefs()
                onUpdate()
            }
        }
    }

    fun detachProgressListener() {
        progressListener?.remove()
        progressListener = null
    }

    fun saveProgressToFirestore() {
        val user = Firebase.auth.currentUser
        if (user == null || user.isAnonymous) return

        val unlockedIds = allAchievements.filter { it.unlocked }.map { it.id }
        val data = mapOf(
            KEY_NIVEL_DESBLOQUEADO to getNivelDesbloqueado(),
            KEY_LOGROS_DESBLOQUEADOS to unlockedIds
        )
        db.collection("usuarios").document(user.uid)
            .set(data, SetOptions.merge())
    }

    private fun applySnapshotToPrefs(snapshot: DocumentSnapshot) {
        val nivelCloud = snapshot.getLong(KEY_NIVEL_DESBLOQUEADO)?.toInt() ?: 1
        val logrosCloud = snapshot.get(KEY_LOGROS_DESBLOQUEADOS) as? List<String> ?: emptyList()

        val editor = prefs.edit()
        editor.putInt(KEY_NIVEL_DESBLOQUEADO, nivelCloud)
        // Limpiar antiguos flags de logros
        allAchievements.forEach { editor.remove(it.id) }
        // Marcar los logros actuales
        logrosCloud.forEach { id -> editor.putBoolean(id, true) }
        editor.apply()
    }

    private fun loadProgressFromPrefs() {
        allAchievements.forEach { ach ->
            ach.unlocked = prefs.getBoolean(ach.id, false)
        }
    }

    fun clearLocalProgress() {
        prefs.edit().clear().apply()
        allAchievements.forEach { it.unlocked = false }
    }

    fun getNivelDesbloqueado(): Int {
        return prefs.getInt(KEY_NIVEL_DESBLOQUEADO, 1)
    }

    fun desbloquearSiguienteNivel(nivelActual: Int) {
        if (nivelActual >= getNivelDesbloqueado()) {
            prefs.edit().putInt(KEY_NIVEL_DESBLOQUEADO, nivelActual + 1).apply()
            saveProgressToFirestore()
        }
    }

    fun unlockAchievement(context: Context, achievementId: String) {
        val ach = allAchievements.find { it.id == achievementId } ?: return
        if (!ach.unlocked) {
            ach.unlocked = true
            prefs.edit().putBoolean(achievementId, true).apply()
            saveProgressToFirestore()

            // ¡AQUÍ ESTÁ LA NUEVA LÓGICA DE FEEDBACK!
            // 1. Reproducimos el sonido especial
            SoundManager.playSfx(R.raw.sfx_achievement_unlocked)
            // 2. Mostramos el Toast con el nombre del logro
            val message = "¡Logro Desbloqueado: ${ach.title}!"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

}
