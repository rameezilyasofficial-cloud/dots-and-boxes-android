package com.fixtocode.dotsandboxes

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var configContainer: View
    private lateinit var arenaContainer: View
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

    private var selectedRows = 4
    private var selectedCols = 4
    private var playerCount = 2
    private val activePlayers = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configContainer = findViewById(R.id.configContainer)
        arenaContainer = findViewById(R.id.arenaContainer)
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
        renderRoster()

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

    private fun isTablet(): Boolean {
        val screenLayout = resources.configuration.screenLayout
        val size = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun setupAdaptiveGridOptions() {
        val tvDevice = findViewById<TextView>(R.id.tvDeviceType)
        gridOptionsLayout.removeAllViews()

        val presets = if (isTablet()) {
            tvDevice.text = "TABLET HARDWARE DETECTED"
            listOf(
                "4x4" to Pair(4, 4),
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

        presets.forEachIndexed { idx, pair ->
            val isDefault = (idx == 1)
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
            if (isDefault) {
                selectedRows = pair.second.first
                selectedCols = pair.second.second
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
                    renderRoster()
                }
            }
            playerCountTabsLayout.addView(btn)
        }
    }

    private fun renderRoster() {
        rosterLayout.removeAllViews()
        activePlayers.clear()

        for (i in 0 until playerCount) {
            val color = playerColors[i]
            val isBot = (i == 1 && playerCount == 2)
            val defaultName = if (i == 0) "Player 1" else if (i == 1 && isBot) "Bot Alpha" else "Player ${i + 1}"
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
            it.totalTimeMillis = 0L
            it.movesCount = 0
        }
        val engine = GameEngine(selectedRows, selectedCols, activePlayers)
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
    }

    private fun updateArenaHud() {
        val eng = gameView.engine ?: return
        val current = eng.getCurrentPlayer()
        tvTurnIndicator.text = "${current.name}'s Turn"
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

        tvWinnerTitle.text = "${winner.name.uppercase()} WINS!"
        tvWinnerTitle.setTextColor(winner.color)
        tvWinnerSubtitle.text = "Dominated with ${winner.score} boxes (${eng.rows * eng.cols} total)"

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
                text = "${p.score} pts"
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