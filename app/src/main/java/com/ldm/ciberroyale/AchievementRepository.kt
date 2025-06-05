package com.ldm.ciberroyale


import android.content.Context
import android.content.SharedPreferences
import com.ldm.ciberroyale.R
object AchievementRepository {

    private const val PREFS_NAME = "prefs_logros"
    private lateinit var prefs: SharedPreferences

    /** Lista estática con todos los logros que existen en la app. */
    val allAchievements: List<Achievement> by lazy {
        listOf(
            // ╔══════════════════════╗
            // ║  Logros de Teoría   ║
            // ╚══════════════════════╝
            Achievement(
                id = "TEMA1_LEIDO",
                title = "Tema 1 completado",
                description = "Has leído todo lo relativo a Contraseñas Seguras",
                type = AchievementType.TEMA,
                themeNumber = 1,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA2_LEIDO",
                title = "Tema 2 completado",
                description = "Has leído todo lo relativo a Phishing",
                type = AchievementType.TEMA,
                themeNumber = 2,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA3_LEIDO",
                title = "Tema 3 completado",
                description = "Has leído todo lo relativo a Privacidad en RRSS",
                type = AchievementType.TEMA,
                themeNumber = 3,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "TEMA4_LEIDO",
                title = "Tema 4 completado",
                description = "Has leído todo lo relativo a Seguridad en Dispositivos Móviles",
                type = AchievementType.TEMA,
                themeNumber = 4,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),

            // ╔════════════════════════════╗
            // ║  Logros de Nivel 1 (3)    ║
            // ╚════════════════════════════╝
            Achievement(
                id = "NIVEL1_BRONZE",
                title = "Nivel 1: Bien",
                description = "Has superado el Nivel 1 con puntuación entre 50 y 70",
                type = AchievementType.NIVEL,
                levelNumber = 1,
                scoreCategory = ScoreCategory.BRONZE,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL1_SILVER",
                title = "Nivel 1: Muy Bien",
                description = "Has superado el Nivel 1 con puntuación entre 71 y 99",
                type = AchievementType.NIVEL,
                levelNumber = 1,
                scoreCategory = ScoreCategory.SILVER,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL1_GOLD",
                title = "Nivel 1: ¡Perfecto!",
                description = "Has superado el Nivel 1 con 100 puntos",
                type = AchievementType.NIVEL,
                levelNumber = 1,
                scoreCategory = ScoreCategory.GOLD,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),

            // ╔════════════════════════════╗
            // ║  Logros de Nivel 2 (3)    ║
            // ╚════════════════════════════╝
            Achievement(
                id = "NIVEL2_BRONZE",
                title = "Nivel 2: Bien",
                description = "Has superado el Nivel 2 con puntuación entre 50 y 70",
                type = AchievementType.NIVEL,
                levelNumber = 2,
                scoreCategory = ScoreCategory.BRONZE,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL2_SILVER",
                title = "Nivel 2: Muy Bien",
                description = "Has superado el Nivel 2 con puntuación entre 71 y 99",
                type = AchievementType.NIVEL,
                levelNumber = 2,
                scoreCategory = ScoreCategory.SILVER,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL2_GOLD",
                title = "Nivel 2: ¡Perfecto!",
                description = "Has superado el Nivel 2 con 100 puntos",
                type = AchievementType.NIVEL,
                levelNumber = 2,
                scoreCategory = ScoreCategory.GOLD,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),

            // ╔════════════════════════════╗
            // ║  Logros de Nivel 3 (3)    ║
            // ╚════════════════════════════╝
            Achievement(
                id = "NIVEL3_BRONZE",
                title = "Nivel 3: Bien",
                description = "Has superado el Nivel 3 con puntuación entre 50 y 70",
                type = AchievementType.NIVEL,
                levelNumber = 3,
                scoreCategory = ScoreCategory.BRONZE,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL3_SILVER",
                title = "Nivel 3: Muy Bien",
                description = "Has superado el Nivel 3 con puntuación entre 71 y 99",
                type = AchievementType.NIVEL,
                levelNumber = 3,
                scoreCategory = ScoreCategory.SILVER,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL3_GOLD",
                title = "Nivel 3: ¡Perfecto!",
                description = "Has superado el Nivel 3 con 100 puntos",
                type = AchievementType.NIVEL,
                levelNumber = 3,
                scoreCategory = ScoreCategory.GOLD,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),

            // ╔════════════════════════════╗
            // ║  Logros de Nivel 4 (3)    ║
            // ╚════════════════════════════╝
            Achievement(
                id = "NIVEL4_BRONZE",
                title = "Nivel 4: Bien",
                description = "Has superado el Nivel 4 con puntuación entre 50 y 70",
                type = AchievementType.NIVEL,
                levelNumber = 4,
                scoreCategory = ScoreCategory.BRONZE,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL4_SILVER",
                title = "Nivel 4: Muy Bien",
                description = "Has superado el Nivel 4 con puntuación entre 71 y 99",
                type = AchievementType.NIVEL,
                levelNumber = 4,
                scoreCategory = ScoreCategory.SILVER,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL4_GOLD",
                title = "Nivel 4: ¡Perfecto!",
                description = "Has superado el Nivel 4 con 100 puntos",
                type = AchievementType.NIVEL,
                levelNumber = 4,
                scoreCategory = ScoreCategory.GOLD,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),

            // ╔════════════════════════════╗
            // ║  Logros del Nivel Final (3) ║
            // ╚════════════════════════════╝
            Achievement(
                id = "NIVEL5_BRONZE",
                title = "Final: Bien",
                description = "Has superado el nivel final con puntuación entre 50 y 70",
                type = AchievementType.NIVEL,
                levelNumber = 5, // O usa “null” y en lugar de 5 pon “FINAL” en el title/ID si prefieres
                scoreCategory = ScoreCategory.BRONZE,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL5_SILVER",
                title = "Final: Muy Bien",
                description = "Has superado el nivel final con puntuación entre 71 y 99",
                type = AchievementType.NIVEL,
                levelNumber = 5,
                scoreCategory = ScoreCategory.SILVER,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            ),
            Achievement(
                id = "NIVEL5_GOLD",
                title = "Final: ¡Perfecto!",
                description = "Has superado el nivel final con 100 puntos",
                type = AchievementType.NIVEL,
                levelNumber = 5,
                scoreCategory = ScoreCategory.GOLD,
                iconLockedRes = R.drawable.ic_locked,
                iconUnlockedRes = R.drawable.ic_unlocked
            )
        )
    }

    /** Inicializa SharedPreferences y carga el estado “unlocked” de cada logro. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        allAchievements.forEach { ach ->
            ach.unlocked = prefs.getBoolean(ach.id, false)
        }
    }

    /**
     * Marca un logro como desbloqueado.
     * Si ya estaba desbloqueado, no hace nada.
     */
    fun unlock(achievementId: String) {
        val ach = allAchievements.find { it.id == achievementId } ?: return
        if (!ach.unlocked) {
            ach.unlocked = true
            prefs.edit().putBoolean(achievementId, true).apply()
        }
    }

    /** Devuelve true si el logro ya está desbloqueado en SharedPreferences. */
    fun isUnlocked(achievementId: String): Boolean {
        return prefs.getBoolean(achievementId, false)
    }
}
/**
 *
| Resource                   | Estado   | Descripción breve                                                              |
| -------------------------- | -------- | ------------------------------------------------------------------------------ |
| `ic_tema_locked`           | Locked   | Libro/hoja en gris o desaturado con candado.                                   |
| `ic_tema_unlocked`         | Unlocked | Mismo libro en color vivo, sin candado y con un “check” pequeño.               |
| `ic_nivel_bronze_locked`   | Locked   | Trofeo/medalla en tonos bronce, pero en gris grisáceo/desaturado, con candado. |
| `ic_nivel_bronze_unlocked` | Unlocked | Trofeo/medalla con brillo cobrizo (bronce real), sin candado.                  |
| `ic_nivel_silver_locked`   | Locked   | Trofeo/medalla plateada (silhouette) en gris claro/desaturado, con candado.    |
| `ic_nivel_silver_unlocked` | Unlocked | Trofeo/medalla plateada con reflejos y sin candado.                            |
| `ic_nivel_gold_locked`     | Locked   | Trofeo/medalla dorada en versión grisácea/apagada, con candado.                |
| `ic_nivel_gold_unlocked`   | Unlocked | Trofeo/medalla dorada brillante, con destellos, sin candado.                   |

 */