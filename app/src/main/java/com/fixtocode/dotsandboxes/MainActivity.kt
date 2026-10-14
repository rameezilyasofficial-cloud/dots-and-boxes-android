package com.fixtocode.dotsandboxes

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var configContainer: View
    private lateinit var arenaContainer: View
    private lateinit var levelOptionsLayout: LinearLayout
    private lateinit var tvLevelInfo: TextView
    private lateinit var tvTotalBank: TextView
    private lateinit var gridOptionsLayout: LinearLayout
    private lateinit var playerCountTabsLayout: LinearLayout
    private lateinit var rosterLayout: LinearLayout
    private lateinit var tvTurnIndicator: TextView
    private lateinit var arenaScoresLayout: LinearLayout
    private lateinit var gameView: GameView

    // Settings
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSpeedAnalytics: Switch

    // Victory Screen Overlay Views
    private lateinit var winnerOverlay: FrameLayout
    private lateinit var tvWinnerTitle: TextView
    private lateinit var tvWinnerSubtitle: TextView
    private lateinit var winnerStatsLayout: LinearLayout
    private lateinit var tvAnalyticsHeading: TextView
    private lateinit var winnerAnalyticsLayout: LinearLayout
    private lateinit var btnOverlayPlayAgain: Button
    private lateinit var btnOverlaySettings: Button

    private val playerColors = intArrayOf(
        0xFFEF4444.toInt(), // P1 Red
        0xFFF59E0B.toInt(), // P2 Amber
        0xFF06B6D4.toInt(), // P3 Cyan
        0xFF10B981.toInt(), // P4 Green
        0xFF8B5CF6.toInt()  // P5 Violet
    )

    private var selectedLevel: Int = 1
    private var selectedRows = 3
    private var selectedCols = 3
    private var playerCount = 2
    private val activePlayers = mutableListOf<Player>()
    private var gridPresets: List<Pair<String, Pair<Int, Int>>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("dots_boxes_progress", Context.MODE_PRIVATE)
        selectedLevel = getUnlockedLevel()

        configContainer = findViewById(R.id.configContainer)
        arenaContainer = findViewById(R.id.arenaContainer)
        levelOptionsLayout = findViewById(R.id.levelOptionsLayout)
        tvLevelInfo = findViewById(R.id.tvLevelInfo)
        tvTotalBank = findViewById(R.id.tvTotalBank)
        gridOptionsLayout = findViewById(R.id.gridOptionsLayout)
        playerCountTabsLayout = findViewById(R.id.playerCountTabsLayout)
        rosterLayout = findViewById(R.id.rosterLayout)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)
        arenaScoresLayout = findViewById(R.id.arenaScoresLayout)
        gameView = findViewById(R.id.gameView)

        switchSound = findViewById(R.id.switchSound)
        switchVibration = findViewById(R.id.switchVibration)
        switchSpeedAnalytics = findViewById(R.id.switchSpeedAnalytics)

        winnerOverlay = findViewById(R.id.winnerOverlay)
        tvWinnerTitle = findViewById(R.id.tvWinnerTitle)
        tvWinnerSubtitle = findViewById(R.id.tvWinnerSubtitle)
        winnerStatsLayout = findViewById(R.id.winnerStatsLayout)
        tvAnalyticsHeading = findViewById(R.id.tvAnalyticsHeading)
        winnerAnalyticsLayout = findViewById(R.id.winnerAnalyticsLayout)
        btnOverlayPlayAgain = findViewById(R.id.btnOverlayPlayAgain)
        btnOverlaySettings = findViewById(R.id.btnOverlaySettings)

        setupAdaptiveGridOptions()
        setupPlayerCountTabs()
        setupLevelSelectorBar()
        applyLevelPreset(selectedLevel)

        findViewById<Button>(R.id.btnStartMatch).setOnClickListener { startMatch() }
        findViewById<ImageButton>(R.id.btnLeaveArena).setOnClickListener { returnToConfig() }
        findViewById<ImageButton>(R.id.btnResetMatch).setOnClickListener { startMatch() }

        btnOverlayPlayAgain.setOnClickListener {
            winnerOverlay.visibility = View.GONE
            startMatch()
        }
        btnOverlaySettings.setOnClickListener {
            winnerOverlay.visibility = View.GONE
            returnToConfig()
        }

        gameView.onTurnCompleted = {
            updateArenaHud()
            if (gameView.engine?.isGameOver == true) {
                gameView.postDelayed({ showVictoryModal() }, 350L)
            }
        }
    }

    private fun getUnlockedLevel(): Int {
        return prefs.getInt("unlocked_level", 1).coerceIn(1, 30)
    }

    private fun getTotalBankPoints(): Int {
        return prefs.getInt("total_bank_pts", 0)
    }

    private fun setupLevelSelectorBar() {
        levelOptionsLayout.removeAllViews()
        val unlocked = getUnlockedLevel()
        tvTotalBank.text = "🏆 ${getTotalBankPoints()} PTS"

        LevelRepository.levels.forEach { spec ->
            val isUnlocked = spec.number <= unlocked
            val isSelected = spec.number == selectedLevel
            val isCleared = spec.number < unlocked

            val btn = Button(this).apply {
                text = when {
                    !isUnlocked -> "🔒 L${spec.number}"
                    isCleared -> "★ L${spec.number}"
                    else -> "LVL ${spec.number}"
                }
                textSize = 12f
                isEnabled = isUnlocked
                setTextColor(
                    when {
                        isSelected -> Color.BLACK
                        !isUnlocked -> 0xFF475569.toInt()
                        else -> Color.WHITE
                    }
                )
                setBackgroundColor(
                    when {
                        isSelected -> 0xFF38BDF8.toInt()
                        !isUnlocked -> 0xFF0E131D.toInt()
                        else -> 0xFF121824.toInt()
                    }
                )
                setOnClickListener {
                    if (isUnlocked) {
                        selectedLevel = spec.number
                        setupLevelSelectorBar()
                        applyLevelPreset(spec.number)
                    }
                }
            }
            levelOptionsLayout.addView(btn)
        }
    }

    private fun applyLevelPreset(levelNum: Int) {
        val spec = LevelRepository.levels[(levelNum - 1).coerceIn(0, 29)]
        tvLevelInfo.text = "LVL ${spec.number}: ${spec.title} • ${spec.pointsPerBox} pts/box • Win Bonus: +${spec.winBonus}"

        // Sync Grid Selection
        selectedRows = spec.rows
        selectedCols = spec.cols
        for (i in 0 until gridOptionsLayout.childCount) {
            val b = gridOptionsLayout.getChildAt(i) as Button
            val preset = gridPresets.getOrNull(i)
            val match = preset?.second?.first == selectedRows && preset.second.second == selectedCols
            b.setBackgroundColor(if (match) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
            b.setTextColor(if (match) Color.BLACK else Color.WHITE)
        }

        // Sync Player Count
        playerCount = spec.playerCount
        val counts = listOf(2, 3, 4, 5)
        for (i in 0 until playerCountTabsLayout.childCount) {
            val b = playerCountTabsLayout.getChildAt(i) as Button
            val match = counts.getOrNull(i) == playerCount
            b.setBackgroundColor(if (match) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
            b.setTextColor(if (match) Color.BLACK else Color.WHITE)
        }

        renderRoster(forceBotsForLevel = true)
    }

    private fun isTablet(): Boolean {
        val screenLayout = resources.configuration.screenLayout
        val size = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun setupAdaptiveGridOptions() {
        val tvDevice = findViewById<TextView>(R.id.tvDeviceType)
        gridOptionsLayout.removeAllViews()

        gridPresets = if (isTablet()) {
            tvDevice.text = "TABLET HARDWARE DETECTED"
            listOf(
                "3x3" to Pair(3, 3),
                "4x4" to Pair(4, 4),
                "4x5" to Pair(5, 4),
                "5x5" to Pair(5, 5),
                "5x7" to Pair(7, 5),
                "5x8" to Pair(8, 5),
                "6x6" to Pair(6, 6),
                "8x8" to Pair(8, 8)
            )
        } else {
            tvDevice.text = "SMARTPHONE DISPLAY OPTIMIZED"
            listOf(
                "3x3" to Pair(3, 3),
                "4x4" to Pair(4, 4),
                "4x5" to Pair(5, 4),
                "5x5" to Pair(5, 5),
                "5x7" to Pair(7, 5),
                "5x8" to Pair(8, 5)
            )
        }

        gridPresets.forEachIndexed { idx, pair ->
            val isDefault = (idx == 0)
            val btn = Button(this).apply {
                text = pair.first
                textSize = 12f
                setTextColor(if (isDefault) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (isDefault) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
                setOnClickListener {
                    selectedRows = pair.second.first
                    selectedCols = pair.second.second
                    for (i in 0 until gridOptionsLayout.childCount) {
                        val b = gridOptionsLayout.getChildAt(i) as Button
                        b.setBackgroundColor(0xFF121824.toInt())
                        b.setTextColor(Color.WHITE)
                    }
                    setBackgroundColor(0xFF38BDF8.toInt())
                    setTextColor(Color.BLACK)
                }
            }
            gridOptionsLayout.addView(btn)
        }
    }

    private fun setupPlayerCountTabs() {
        playerCountTabsLayout.removeAllViews()
        val counts = listOf(2, 3, 4, 5)

        counts.forEach { count ->
            val isSelected = (count == playerCount)
            val btn = Button(this).apply {
                text = "${count}P"
                textSize = 13f
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                lp.setMargins(4, 0, 4, 0)
                layoutParams = lp
                setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (isSelected) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
                setOnClickListener {
                    playerCount = count
                    for (i in 0 until playerCountTabsLayout.childCount) {
                        val b = playerCountTabsLayout.getChildAt(i) as Button
                        b.setBackgroundColor(0xFF121824.toInt())
                        b.setTextColor(Color.WHITE)
                    }
                    setBackgroundColor(0xFF38BDF8.toInt())
                    setTextColor(Color.BLACK)
                    renderRoster(forceBotsForLevel = false)
                }
            }
            playerCountTabsLayout.addView(btn)
        }
    }

    private fun renderRoster(forceBotsForLevel: Boolean = false) {
        rosterLayout.removeAllViews()
        activePlayers.clear()

        for (i in 0 until playerCount) {
            val color = playerColors[i]
            val isBot = if (forceBotsForLevel) (i >= 1) else (i == 1 && playerCount == 2)
            val defaultName = if (i == 0) "Player 1" else if (isBot) "Bot L$selectedLevel-${i}" else "Player ${i + 1}"
            val player = Player(i, defaultName, color, isBot)
            activePlayers.add(player)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(14, 10, 14, 10)
                setBackgroundColor(0xFF121824.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 6, 0, 6) }
                layoutParams = lp
            }

            val colorBadge = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(26, 26).apply { setMargins(4, 0, 14, 0) }
                setBackgroundColor(color)
            }

            val etName = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setText(defaultName)
                setTextColor(Color.WHITE)
                textSize = 13f
                setBackgroundColor(Color.TRANSPARENT)
            }

            val btnBot = Button(this).apply {
                text = if (isBot) "BOT" else "HUMAN"
                textSize = 11f
                setBackgroundColor(if (isBot) 0xFF4C1D95.toInt() else 0xFF1E293B.toInt())
                setTextColor(Color.WHITE)
                setOnClickListener {
                    player.isBot = !player.isBot
                    text = if (player.isBot) "BOT" else "HUMAN"
                    setBackgroundColor(if (player.isBot) 0xFF4C1D95.toInt() else 0xFF1E293B.toInt())
                }
            }

            row.addView(colorBadge)
            row.addView(etName)
            row.addView(btnBot)
            rosterLayout.addView(row)
        }
    }

    private fun startMatch() {
        for (i in 0 until rosterLayout.childCount) {
            val row = rosterLayout.getChildAt(i) as? LinearLayout ?: continue
            val etName = row.getChildAt(1) as? EditText ?: continue
            val inputName = etName.text.toString().trim()
            if (inputName.isNotEmpty() && i < activePlayers.size) {
                activePlayers[i].name = inputName
            }
        }

        gameView.audio.isEnabled = switchSound.isChecked
        gameView.isVibrationEnabled = switchVibration.isChecked

        winnerOverlay.visibility = View.GONE
        configContainer.visibility = View.GONE
        arenaContainer.visibility = View.VISIBLE

        activePlayers.forEach {
            it.score = 0
            it.boxesCount = 0
            it.totalTimeMillis = 0L
            it.movesCount = 0
        }

        val spec = LevelRepository.levels[(selectedLevel - 1).coerceIn(0, 29)]
        val engine = GameEngine(
            rows = selectedRows,
            cols = selectedCols,
            players = activePlayers,
            pointsPerBox = spec.pointsPerBox,
            aiSkillLevel = spec.aiSkillLevel
        )
        gameView.engine = engine
        engine.startTurnClock()
        gameView.invalidate()

        updateArenaHud()

        if (engine.getCurrentPlayer().isBot) {
            gameView.postDelayed({ gameView.runBotTurn() }, 400L)
        }
    }

    private fun returnToConfig() {
        winnerOverlay.visibility = View.GONE
        arenaContainer.visibility = View.GONE
        configContainer.visibility = View.VISIBLE
        setupLevelSelectorBar()
    }

    private fun updateArenaHud() {
        val eng = gameView.engine ?: return
        val current = eng.getCurrentPlayer()
        tvTurnIndicator.text = "LVL $selectedLevel • ${current.name}'s Turn"
        tvTurnIndicator.setTextColor(current.color)

        val totalHumanMillis = eng.players.filter { !it.isBot }.sumOf { it.totalTimeMillis }

        arenaScoresLayout.removeAllViews()
        eng.players.forEach { p ->
            val tv = TextView(this).apply {
                val baseText = "${p.name}: ${p.score}"
                text = if (switchSpeedAnalytics.isChecked && !p.isBot && totalHumanMillis > 0) {
                    val pct = (p.totalTimeMillis.toDouble() / totalHumanMillis.toDouble()) * 100.0
                    "$baseText (${String.format(Locale.US, "%.0f", pct)}% time)  "
                } else {
                    "$baseText  "
                }
                textSize = 12f
                setTextColor(if (p.id == current.id) p.color else Color.LTGRAY)
                setPadding(14, 4, 14, 4)
            }
            arenaScoresLayout.addView(tv)
        }
    }

    private fun showVictoryModal() {
        val eng = gameView.engine ?: return
        val sorted = eng.players.sortedByDescending { it.score }
        val winner = sorted.firstOrNull() ?: return
        val spec = LevelRepository.levels[(selectedLevel - 1).coerceIn(0, 29)]
        val hasBotOpponent = eng.players.any { it.isBot }

        if (winner.id == 0 && hasBotOpponent) {
            // Player 1 beat the level! Award level bonus and unlock next level
            val newBank = getTotalBankPoints() + winner.score + spec.winBonus
            val unlocked = getUnlockedLevel()
            val nextLevel = (selectedLevel + 1).coerceAtMost(30)

            prefs.edit()
                .putInt("total_bank_pts", newBank)
                .putInt("unlocked_level", maxOf(unlocked, nextLevel))
                .apply()

            tvWinnerTitle.text = "LEVEL $selectedLevel CLEARED!"
            tvWinnerTitle.setTextColor(winner.color)
            tvWinnerSubtitle.text = "Score: ${winner.score} + ${spec.winBonus} Bonus  •  Bank: 🏆 $newBank"

            if (selectedLevel < 30) {
                btnOverlayPlayAgain.text = "NEXT: LEVEL ${selectedLevel + 1} ⏭"
                btnOverlayPlayAgain.setOnClickListener {
                    winnerOverlay.visibility = View.GONE
                    selectedLevel = (selectedLevel + 1).coerceAtMost(30)
                    setupLevelSelectorBar()
                    applyLevelPreset(selectedLevel)
                    startMatch()
                }
            } else {
                btnOverlayPlayAgain.text = "PLAY LEVEL 30 AGAIN"
                btnOverlayPlayAgain.setOnClickListener {
                    winnerOverlay.visibility = View.GONE
                    startMatch()
                }
            }
        } else {
            tvWinnerTitle.text = "${winner.name.uppercase()} WINS!"
            tvWinnerTitle.setTextColor(winner.color)
            tvWinnerSubtitle.text = "Captured ${winner.boxesCount} boxes (${winner.score} pts)"

            btnOverlayPlayAgain.text = if (hasBotOpponent) "RETRY LEVEL $selectedLevel" else "PLAY AGAIN"
            btnOverlayPlayAgain.setOnClickListener {
                winnerOverlay.visibility = View.GONE
                startMatch()
            }
        }

        winnerStatsLayout.removeAllViews()
        sorted.forEachIndexed { rank, p ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(14, 10, 14, 10)
                setBackgroundResource(R.drawable.bg_stat_item)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 4, 0, 4) }
                layoutParams = lp
            }

            val rankBadge = TextView(this).apply {
                text = "#${rank + 1} "
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(18, 18).apply { setMargins(6, 6, 10, 0) }
                setBackgroundColor(p.color)
            }

            val nameView = TextView(this).apply {
                text = p.name + if (p.isBot) " (AI)" else ""
                setTextColor(Color.WHITE)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val scoreView = TextView(this).apply {
                text = "${p.boxesCount} boxes (${p.score} pts)"
                setTextColor(p.color)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            item.addView(rankBadge)
            item.addView(dot)
            item.addView(nameView)
            item.addView(scoreView)
            winnerStatsLayout.addView(item)
        }

        winnerAnalyticsLayout.removeAllViews()
        if (switchSpeedAnalytics.isChecked) {
            tvAnalyticsHeading.visibility = View.VISIBLE
            winnerAnalyticsLayout.visibility = View.VISIBLE

            val humanPlayers = eng.players.filter { !it.isBot }
            val totalHumanMillis = humanPlayers.sumOf { it.totalTimeMillis }

            val rankedBySpeed = humanPlayers.sortedBy { it.getAverageResponseSec() }
            val fastestHuman = rankedBySpeed.firstOrNull { it.movesCount > 0 }

            humanPlayers.forEach { p ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(14, 8, 14, 8)
                    setBackgroundResource(R.drawable.bg_stat_item)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 3, 0, 3) }
                    layoutParams = lp
                }

                val pct = if (totalHumanMillis > 0) (p.totalTimeMillis.toDouble() / totalHumanMillis.toDouble()) * 100.0 else 0.0
                val avgSec = p.getAverageResponseSec()

                val nameLabel = TextView(this).apply {
                    val badge = if (p.id == fastestHuman?.id && humanPlayers.size > 1) " ⚡ FASTEST" else ""
                    text = "${p.name}$badge"
                    setTextColor(if (p.id == fastestHuman?.id && humanPlayers.size > 1) 0xFF38BDF8.toInt() else Color.WHITE)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val metricsLabel = TextView(this).apply {
                    text = "${String.format(Locale.US, "%.1f", avgSec)}s avg  |  ${String.format(Locale.US, "%.0f", pct)}% time"
                    setTextColor(p.color)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }

                row.addView(nameLabel)
                row.addView(metricsLabel)
                winnerAnalyticsLayout.addView(row)
            }
        } else {
            tvAnalyticsHeading.visibility = View.GONE
            winnerAnalyticsLayout.visibility = View.GONE
        }

        winnerOverlay.visibility = View.VISIBLE
    }
}