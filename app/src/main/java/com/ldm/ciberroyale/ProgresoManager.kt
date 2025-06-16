package com.ldm.ciberroyale

import android.content.Context
import android.content.SharedPreferences
import com.ldm.ciberroyale.R


// --- OBJETO MANAGER ---
object ProgresoManager {


    private const val PREFS_NAME = "CiberRoyaleProgreso"
    private lateinit var prefs: SharedPreferences

    val allAchievements: List<Achievement> by lazy {
        listOf(
            Achievement(
                id = "TEMA1_LEIDO",
                title = "Tema 1 completado",
                description = "Has leído todo lo relativo a Contraseñas Seguras",
                type = AchievementType.TEMA,
                levelNumber = 1, // Usamos levelNumber para ordenar
                scoreCategory = null,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA2_LEIDO",
                title = "Tema 2 completado",
                description = "Has leído todo lo relativo a Phishing",
                type = AchievementType.TEMA,
                levelNumber = 2,
                scoreCategory = null,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA3_LEIDO",
                title = "Tema 3 completado",
                description = "Has leído todo lo relativo a Privacidad en RRSS",
                type = AchievementType.TEMA,
                levelNumber = 3,
                scoreCategory = null,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA4_LEIDO",
                title = "Tema 4 completado",
                description = "Has leído todo lo relativo a Seguridad en Dispositivos Móviles",
                type = AchievementType.TEMA,
                levelNumber = 4,
                scoreCategory = null,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            // --- LOGROS DE NIVEL 1 ---
            Achievement("NIVEL1_COMPLETADO", "Hacker Novato", "Has superado el Nivel 1.", AchievementType.NIVEL, 1, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL1_EXCELENTE", "Maestro de Contraseñas", "Has superado el Nivel 1 con más de 90 puntos.", AchievementType.NIVEL, 1, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL1_INTACTO", "Intocable", "Has derrotado al jefe del Nivel 1 sin recibir daño.", AchievementType.NIVEL, 1, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),

            // --- LOGROS DE NIVEL 2 ---
            Achievement("NIVEL2_COMPLETADO", "Anti-Phishing", "Has superado el Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL2_PERFECCIONISTA", "Ojo de Halcón", "Has acertado todos los mensajes y URLs en el Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL2_PESCADOR_PRO", "Pescador Pro", "Has pescado más de 15 Vipers en la batalla del Nivel 2.", AchievementType.NIVEL, 2, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),

            // --- LOGROS DE NIVEL 3 (AJUSTADOS) ---
            Achievement("NIVEL3_COMPLETADO", "Guardián de la Privacidad", "Has completado los 3 desafíos del Nivel 3.", AchievementType.NIVEL, 3, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL3_ESTRATEGIA_PERFECTA", "Estrategia Perfecta", "Has configurado la privacidad del perfil correctamente al primer intento.", AchievementType.NIVEL, 3, ScoreCategory.SILVER, R.drawable.ic_locked, R.drawable.ic_unlocked),

            // --- LOGROS DE NIVEL 4 ---
            Achievement("NIVEL4_COMPLETADO", "Cerrajero Digital", "Has superado el Nivel 4.", AchievementType.NIVEL, 4, ScoreCategory.BRONZE, R.drawable.ic_locked, R.drawable.ic_unlocked),
            Achievement("NIVEL4_MENTE_MAESTRA", "Mente Maestra", "Has descifrado el PIN del Nivel 4 en 3 intentos o menos.", AchievementType.NIVEL, 4, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked),

            // --- LOGROS GLOBALES ---
            Achievement("JUEGO_COMPLETADO", "¡ViperWare Derrotado!", "Has completado los 4 niveles.", AchievementType.NIVEL, 5, ScoreCategory.GOLD, R.drawable.ic_locked, R.drawable.ic_unlocked)
        )
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        allAchievements.forEach { ach ->
            ach.unlocked = prefs.getBoolean(ach.id, false)
        }
    }

    // --- Métodos para NIVELES ---
    fun getNivelDesbloqueado(): Int {
        return prefs.getInt("nivel_desbloqueado", 1)
    }

    fun desbloquearSiguienteNivel(nivelActualCompletado: Int) {
        val nivelMasAlto = getNivelDesbloqueado()
        if (nivelActualCompletado >= nivelMasAlto) {
            prefs.edit().putInt("nivel_desbloqueado", nivelActualCompletado + 1).apply()
        }
    }

    // --- Métodos para LOGROS ---
    fun unlockAchievement(achievementId: String) {
        val ach = allAchievements.find { it.id == achievementId } ?: return
        if (!ach.unlocked) {
            ach.unlocked = true
            prefs.edit().putBoolean(achievementId, true).apply()
        }
    }
}
