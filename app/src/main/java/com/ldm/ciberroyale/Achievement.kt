package com.ldm.ciberroyale

enum class AchievementType { TEMA, NIVEL }
enum class ScoreCategory { BRONZE, SILVER, GOLD }
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val type: AchievementType,
    val levelNumber: Int?,
    val scoreCategory: ScoreCategory?,
    val iconLockedRes: Int,
    val iconUnlockedRes: Int,
    var unlocked: Boolean = false
)
