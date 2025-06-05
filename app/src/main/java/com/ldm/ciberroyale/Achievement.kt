package com.ldm.ciberroyale


enum class AchievementType {
    TEMA,    // Logro de teoría
    NIVEL    // Logro basado en nivel
}

enum class ScoreCategory {
    BRONZE,  // 50..70
    SILVER,  // 71..99
    GOLD     // >=100
}

data class Achievement(
    val id: String,              // Ej: "TEMA1_LEIDO", "NIVEL3_SILVER"
    val title: String,           // Ej: "Tema 1 completado", "Nivel 3: Muy Bien"
    val description: String,     // Breve descripción del logro
    val type: AchievementType,   // TEMA o NIVEL
    val levelNumber: Int? = null,// Para logros de nivel: 1..5. Null si es logro de teoría
    val themeNumber: Int? = null,// Para logros de teoría: 1..4. Null si es logro de nivel
    val scoreCategory: ScoreCategory? = null,
    /* Para logros de nivel: BRONZE, SILVER o GOLD.
       Null en logros de teoría */
    val iconLockedRes: Int,      // R.drawable.xxx cuando está bloqueado
    val iconUnlockedRes: Int,    // R.drawable.xxx cuando está desbloqueado
    var unlocked: Boolean = false// Estado (bloqueado/desbloqueado). Se inicializa en false.
)
