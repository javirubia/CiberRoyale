package com.ldm.ciberroyale

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase



object ProgresoManager {

    private const val PREFS_NAME = "CiberRoyaleProgreso"
    private lateinit var prefs: SharedPreferences
    private val db = Firebase.firestore
    private const val KEY_NIVEL_DESBLOQUEADO = "nivel_desbloqueado"
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

        progressListener = db.collection("usuarios").document(user.uid)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    val nivel = document.getLong("nivelDesbloqueado")?.toInt() ?: 1
                    val logrosIds = document.get("logrosDesbloqueados") as? List<String> ?: emptyList()

                    clearLocalProgress() // Limpia para no mezclar datos de usuarios

                    prefs.edit().putInt(KEY_NIVEL_DESBLOQUEADO, nivel).apply()
                    logrosIds.forEach { prefs.edit().putBoolean(it, true).apply() }
                }
                loadProgressFromPrefs()
                onUpdate()
            }
    }

    fun detachProgressListener() {
        progressListener?.remove()
        progressListener = null
    }

    fun saveProgressToFirestore() {
        val user = Firebase.auth.currentUser
        if (user == null || user.isAnonymous) return

        val unlockedAchievementsIds = allAchievements.filter { it.unlocked }.map { it.id }
        val progressData = hashMapOf(
            "nivelDesbloqueado" to getNivelDesbloqueado(),
            "logrosDesbloqueados" to unlockedAchievementsIds
        )
        db.collection("usuarios").document(user.uid).set(progressData)
    }

    private fun loadProgressFromPrefs() {
        allAchievements.forEach { ach ->
            ach.unlocked = prefs.getBoolean(ach.id, false)
        }
    }

    fun clearLocalProgress() {
        val unlockedBefore = allAchievements.filter { it.unlocked }
        prefs.edit().clear().apply()
        unlockedBefore.forEach { it.unlocked = false }
    }

    fun getNivelDesbloqueado(): Int {
        return prefs.getInt(KEY_NIVEL_DESBLOQUEADO, 1)
    }

    fun desbloquearSiguienteNivel(nivelActualCompletado: Int) {
        val nivelMasAlto = getNivelDesbloqueado()
        if (nivelActualCompletado >= nivelMasAlto) {
            prefs.edit().putInt(KEY_NIVEL_DESBLOQUEADO, nivelActualCompletado + 1).apply()
            saveProgressToFirestore()
        }
    }

    fun unlockAchievement(achievementId: String) {
        val ach = allAchievements.find { it.id == achievementId }
        if (ach != null && !ach.unlocked) {
            ach.unlocked = true
            prefs.edit().putBoolean(achievementId, true).apply()
            saveProgressToFirestore()
        }
    }
}