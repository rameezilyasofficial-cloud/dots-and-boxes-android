package com.fixtocode.dotsandboxes

import android.content.Context
import android.content.SharedPreferences

enum class WallPattern(val tag: String) {
    NONE("Standard Grid"),
    CENTER_CROSS("Cross Barrier"),
    CORNERS("Corner Bunkers"),
    CORRIDOR("Tactical Corridor"),
    FORTRESS("Center Fortress")
}

data class CampaignLevel(
    val number: Int,
    val title: String,
    val subtitle: String,
    val rows: Int,
    val cols: Int,
    val bots: List<Pair<String, BotDifficulty>>,
    val gold2xBoxes: Int,
    val diamond3xBoxes: Int,
    val wallPattern: WallPattern,
    val baseCoinReward: Int,
    val isBoss: Boolean = false
)

class CampaignManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dots_boxes_campaign_v1", Context.MODE_PRIVATE)

    val levels: List<CampaignLevel> = buildThirtyLevels()

    var unlockedLevel: Int
        get() = prefs.getInt("unlocked_level", 1).coerceIn(1, 30)
        set(value) = prefs.edit().putInt("unlocked_level", value.coerceIn(1, 30)).apply()

    var totalCoins: Int
        get() = prefs.getInt("total_coins", 100)
        set(value) = prefs.edit().putInt("total_coins", value).apply()

    fun getStarsForLevel(levelNum: Int): Int {
        return prefs.getInt("stars_lvl_$levelNum", 0).coerceIn(0, 3)
    }

    fun getTotalStars(): Int {
        var sum = 0
        for (i in 1..30) {
            sum += getStarsForLevel(i)
        }
        return sum
    }

    fun getPlayerRankTitle(): String {
        val stars = getTotalStars()
        return when {
            stars >= 80 -> "👑 APEX LEGEND"
            stars >= 60 -> "🔥 GRANDMASTER"
            stars >= 42 -> "⚡ CYBER COMMANDER"
            stars >= 25 -> "🛡️ GRID TACTICIAN"
            stars >= 10 -> "⚔️ ARENA RUNNER"
            else -> "🌱 ROOKIE STRIKER"
        }
    }

    /**
     * Records level victory, calculates stars & coins earned, and unlocks next stage.
     */
    fun recordVictory(level: CampaignLevel, playerScore: Int, totalBoardPoints: Int): Pair<Int, Int> {
        val share = if (totalBoardPoints > 0) playerScore.toDouble() / totalBoardPoints.toDouble() else 0.5
        val earnedStars = when {
            share >= 0.68 -> 3
            share >= 0.52 -> 2
            else -> 1
        }

        val previousStars = getStarsForLevel(level.number)
        val isFirstClear = previousStars == 0

        if (earnedStars > previousStars) {
            prefs.edit().putInt("stars_lvl_${level.number}", earnedStars).apply()
        }

        if (level.number >= unlockedLevel && level.number < 30) {
            unlockedLevel = level.number + 1
        }

        val starBonus = earnedStars * 25
        val firstClearBonus = if (isFirstClear) level.baseCoinReward else (level.baseCoinReward / 3)
        val coinsEarned = firstClearBonus + starBonus
        totalCoins += coinsEarned

        return Pair(earnedStars, coinsEarned)
    }

    private fun buildThirtyLevels(): List<CampaignLevel> {
        return listOf(
            // WORLD 1: BOOTCAMP (Levels 1 - 5)
            CampaignLevel(1, "First Contact", "3x3 Warmup • +2X Gold Box", 3, 3,
                listOf("Scout Bot" to BotDifficulty.EASY), 1, 0, WallPattern.NONE, 50),
            CampaignLevel(2, "Double Trouble", "3x3 Grid • Two 2X Gold Boxes", 3, 3,
                listOf("Rusty AI" to BotDifficulty.EASY), 2, 0, WallPattern.NONE, 60),
            CampaignLevel(3, "Quad Core", "4x4 Grid • 1x Diamond 3X Box", 4, 4,
                listOf("Circuit Bot" to BotDifficulty.EASY), 2, 1, WallPattern.NONE, 75),
            CampaignLevel(4, "Crossfire", "4x4 Cross Barrier • 2X Gold", 4, 4,
                listOf("Pulse AI" to BotDifficulty.EASY), 3, 0, WallPattern.CENTER_CROSS, 90),
            CampaignLevel(5, "BOSS: Iron Guard", "4x4 Fortress • Medium Boss AI", 4, 4,
                listOf("IRON GUARD" to BotDifficulty.MEDIUM), 3, 1, WallPattern.FORTRESS, 150, isBoss = true),

            // WORLD 2: TACTICAL ARENA (Levels 6 - 10)
            CampaignLevel(6, "Triple Threat", "4x4 3-Player Chaos • 2X Boxes", 4, 4,
                listOf("Bot Alpha" to BotDifficulty.EASY, "Bot Beta" to BotDifficulty.EASY), 3, 1, WallPattern.NONE, 100),
            CampaignLevel(7, "Bunker Buster", "4x5 Corner Bunkers • Tactical", 5, 4,
                listOf("Trench AI" to BotDifficulty.MEDIUM), 3, 1, WallPattern.CORNERS, 110),
            CampaignLevel(8, "Diamond Rush", "4x5 Grid • 3x Diamond 3X Vaults", 5, 4,
                listOf("Greed Bot" to BotDifficulty.MEDIUM), 2, 3, WallPattern.NONE, 125),
            CampaignLevel(9, "Narrow Corridor", "4x5 Corridor Maze • 1v2 Bots", 5, 4,
                listOf("Viper" to BotDifficulty.MEDIUM, "Cobra" to BotDifficulty.EASY), 3, 2, WallPattern.CORRIDOR, 140),
            CampaignLevel(10, "BOSS: Cyber Stalker", "5x5 Cross Arena • Elite Boss", 5, 5,
                listOf("CYBER STALKER" to BotDifficulty.MEDIUM), 4, 2, WallPattern.CENTER_CROSS, 220, isBoss = true),

            // WORLD 3: HIGH VOLTAGE (Levels 11 - 15)
            CampaignLevel(11, "Pentagon Grid", "5x5 Open Warfare • High Value", 5, 5,
                listOf("Volt AI" to BotDifficulty.MEDIUM), 4, 2, WallPattern.NONE, 150),
            CampaignLevel(12, "Three-Way Siege", "5x5 Fortress • 1v2 Medium AI", 5, 5,
                listOf("Pyro" to BotDifficulty.MEDIUM, "Cryo" to BotDifficulty.MEDIUM), 4, 2, WallPattern.FORTRESS, 165),
            CampaignLevel(13, "Corner Pocket", "5x5 Bunkers • 4x Diamond Vaults", 5, 5,
                listOf("Sniper Bot" to BotDifficulty.MEDIUM), 3, 4, WallPattern.CORNERS, 180),
            CampaignLevel(14, "Quad Squad", "5x5 4-Player Free-For-All", 5, 5,
                listOf("Red" to BotDifficulty.MEDIUM, "Blue" to BotDifficulty.EASY, "Green" to BotDifficulty.EASY), 5, 2, WallPattern.NONE, 195),
            CampaignLevel(15, "BOSS: Nexus Prime", "5x6 Pro AI • First Pro Boss", 6, 5,
                listOf("NEXUS PRIME" to BotDifficulty.PRO), 4, 3, WallPattern.CORRIDOR, 300, isBoss = true),

            // WORLD 4: OVERDRIVE (Levels 16 - 20)
            CampaignLevel(16, "Six Pack", "5x6 Grid • Pro AI Duel", 6, 5,
                listOf("Razor AI" to BotDifficulty.PRO), 4, 2, WallPattern.NONE, 210),
            CampaignLevel(17, "Divided Kingdom", "5x6 Cross Wall • 1v2 Battle", 6, 5,
                listOf("Knight" to BotDifficulty.PRO, "Rook" to BotDifficulty.MEDIUM), 5, 2, WallPattern.CENTER_CROSS, 225),
            CampaignLevel(18, "Treasure Vault", "5x7 Tall Grid • 6x Diamond 3X", 7, 5,
                listOf("Vault Hunter" to BotDifficulty.MEDIUM), 4, 6, WallPattern.FORTRESS, 240),
            CampaignLevel(19, "Gauntlet Run", "5x7 Corridor • 4-Player War", 7, 5,
                listOf("Apex" to BotDifficulty.PRO, "Blitz" to BotDifficulty.MEDIUM, "Echo" to BotDifficulty.MEDIUM), 5, 3, WallPattern.CORRIDOR, 260),
            CampaignLevel(20, "BOSS: Shadow Weaver", "5x7 Bunkers • Pro Mastermind", 7, 5,
                listOf("SHADOW WEAVER" to BotDifficulty.PRO), 5, 4, WallPattern.CORNERS, 400, isBoss = true),

            // WORLD 5: GRANDMASTER TRIALS (Levels 21 - 25)
            CampaignLevel(21, "Seven Pillars", "5x7 Pro Duel • Minimax Logic", 7, 5,
                listOf("Oracle AI" to BotDifficulty.PRO), 6, 3, WallPattern.NONE, 280),
            CampaignLevel(22, "Twin Assassins", "5x7 Fortress • Two Pro Bots", 7, 5,
                listOf("Phantom" to BotDifficulty.PRO, "Spectre" to BotDifficulty.PRO), 5, 4, WallPattern.FORTRESS, 310),
            CampaignLevel(23, "Octagon Stretch", "5x8 Max Grid • High Stakes", 8, 5,
                listOf("Titan AI" to BotDifficulty.PRO), 6, 4, WallPattern.CENTER_CROSS, 330),
            CampaignLevel(24, "Five-Star General", "5x8 5-Player Battle Royale", 8, 5,
                listOf("Alpha" to BotDifficulty.PRO, "Bravo" to BotDifficulty.MEDIUM, "Charlie" to BotDifficulty.MEDIUM, "Delta" to BotDifficulty.EASY), 6, 4, WallPattern.NONE, 360),
            CampaignLevel(25, "BOSS: Quantum Cortex", "5x8 Corridor • Twin Pro Bosses", 8, 5,
                listOf("QUANTUM" to BotDifficulty.PRO, "CORTEX" to BotDifficulty.PRO), 6, 5, WallPattern.CORRIDOR, 500, isBoss = true),

            // WORLD 6: SILICON VALLEY LEGENDS (Levels 26 - 30)
            CampaignLevel(26, "Zero Margin", "5x8 Pure Pro 1v1 • 8x Diamonds", 8, 5,
                listOf("DeepGrid AI" to BotDifficulty.PRO), 4, 8, WallPattern.CORNERS, 400),
            CampaignLevel(27, "Triad Syndicate", "5x8 Cross • Three Pro Bots", 8, 5,
                listOf("Vector" to BotDifficulty.PRO, "Matrix" to BotDifficulty.PRO, "Tensor" to BotDifficulty.PRO), 6, 5, WallPattern.CENTER_CROSS, 430),
            CampaignLevel(28, "Diamond Minefield", "5x8 Fortress • 10x Diamond 3X", 8, 5,
                listOf("Aegis" to BotDifficulty.PRO, "Nova" to BotDifficulty.PRO), 5, 10, WallPattern.FORTRESS, 460),
            CampaignLevel(29, "Pentagon Chaos", "5x8 5-Player All-Pro War", 8, 5,
                listOf("Zero" to BotDifficulty.PRO, "One" to BotDifficulty.PRO, "Two" to BotDifficulty.PRO, "Three" to BotDifficulty.PRO), 8, 6, WallPattern.CORRIDOR, 500),
            CampaignLevel(30, "FINAL BOSS: OMEGA", "5x8 Ultimate Championship", 8, 5,
                listOf("OMEGA ARCHITECT" to BotDifficulty.PRO, "OMEGA GUARD" to BotDifficulty.PRO), 8, 8, WallPattern.FORTRESS, 1000, isBoss = true)
        )
    }
}