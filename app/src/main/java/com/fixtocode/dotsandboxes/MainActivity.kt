package com.fixtocode.dotsandboxes

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var campaignManager: CampaignManager

    private lateinit var configContainer: View
    private lateinit var arenaContainer: View

    // Header Stats
    private lateinit var tvRankBadge: TextView
    private lateinit var tvTotalStars: TextView
    private lateinit var tvTotalCoins: TextView

    // Mode Tabs & Sections
    private lateinit var btnTabCampaign: Button
    private lateinit var btnTabCustom: Button
    private lateinit var campaignSection: LinearLayout
    private lateinit var campaignLevelsContainer: LinearLayout
    private lateinit var customPlaySection: LinearLayout

    // Custom Play Views
    private lateinit var gridOptionsLayout: LinearLayout
    private lateinit var playerCountTabsLayout: LinearLayout
    private lateinit var rosterLayout: LinearLayout

    // Arena HUD Views
    private lateinit var tvArenaStageTitle: TextView
    private lateinit var tvTurnIndicator: TextView
    private lateinit var arenaScoresLayout: LinearLayout
    private lateinit var gameView: GameView

    // Settings
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSpeedAnalytics: Switch

    // Victory Overlay Views
    private lateinit var winnerOverlay: FrameLayout
    private lateinit var tvWinnerStars: TextView
    private lateinit var tvWinnerTitle: TextView
    private lateinit var tvWinnerSubtitle: TextView
    private lateinit var winnerStatsLayout: LinearLayout
    private lateinit var tvAnalyticsHeading: TextView
    private lateinit var winnerAnalyticsLayout: LinearLayout
    private lateinit var btnOverlayPrimary: Button
    private lateinit var btnOverlaySettings: Button

    private val playerColors = intArrayOf(
        0xFFEF4444.toInt(), // P1 Red
        0xFFF59E0B.toInt(), // P2 Amber
        0xFF06B6D4.toInt(), // P3 Cyan
        0xFF10B981.toInt(), // P4 Green
        0xFF8B5CF6.toInt()  // P5 Violet
    )

    private var isCampaignMode = true
    private var activeCampaignLevel: CampaignLevel? = null

    private var selectedRows = 4
    private var selectedCols = 4
    private var playerCount = 2
    private val activePlayers = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        campaignManager = CampaignManager(this)

        configContainer = findViewById(R.id.configContainer)
        arenaContainer = findViewById(R.id.arenaContainer)

        tvRankBadge = findViewById(R.id.tvRankBadge)
        tvTotalStars = findViewById(R.id.tvTotalStars)
        tvTotalCoins = findViewById(R.id.tvTotalCoins)

        btnTabCampaign = findViewById(R.id.btnTabCampaign)
        btnTabCustom = findViewById(R.id.btnTabCustom)
        campaignSection = findViewById(R.id.campaignSection)
        campaignLevelsContainer = findViewById(R.id.campaignLevelsContainer)
        customPlaySection = findViewById(R.id.customPlaySection)

        gridOptionsLayout = findViewById(R.id.gridOptionsLayout)
        playerCountTabsLayout = findViewById(R.id.playerCountTabsLayout)
        rosterLayout = findViewById(R.id.rosterLayout)

        tvArenaStageTitle = findViewById(R.id.tvArenaStageTitle)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)
        arenaScoresLayout = findViewById(R.id.arenaScoresLayout)
        gameView = findViewById(R.id.gameView)

        switchSound = findViewById(R.id.switchSound)
        switchVibration = findViewById(R.id.switchVibration)
        switchSpeedAnalytics = findViewById(R.id.switchSpeedAnalytics)

        winnerOverlay = findViewById(R.id.winnerOverlay)
        tvWinnerStars = findViewById(R.id.tvWinnerStars)
        tvWinnerTitle = findViewById(R.id.tvWinnerTitle)
        tvWinnerSubtitle = findViewById(R.id.tvWinnerSubtitle)
        winnerStatsLayout = findViewById(R.id.winnerStatsLayout)
        tvAnalyticsHeading = findViewById(R.id.tvAnalyticsHeading)
        winnerAnalyticsLayout = findViewById(R.id.winnerAnalyticsLayout)
        btnOverlayPrimary = findViewById(R.id.btnOverlayPrimary)
        btnOverlaySettings = findViewById(R.id.btnOverlaySettings)

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            gameView.audio.isEnabled = isChecked
            if (isChecked) gameView.audio.uiClick()
        }
        switchVibration.setOnCheckedChangeListener { btn, isChecked ->
            gameView.haptics.isEnabled = isChecked
            if (isChecked) gameView.haptics.uiTick(btn)
        }
        switchSpeedAnalytics.setOnCheckedChangeListener { btn, _ ->
            triggerUiFeedback(btn)
        }

        btnTabCampaign.setOnClickListener {
            triggerUiFeedback(it)
            switchMainMode(true)
        }
        btnTabCustom.setOnClickListener {
            triggerUiFeedback(it)
            switchMainMode(false)
        }

        setupAdaptiveGridOptions()
        setupPlayerCountTabs()
        renderRoster()
        refreshProfileAndCampaignUI()
        switchMainMode(true)

        findViewById<Button>(R.id.btnStartMatch).setOnClickListener {
            triggerUiFeedback(it)
            startCustomMatch()
        }
        findViewById<ImageButton>(R.id.btnLeaveArena).setOnClickListener {
            triggerUiFeedback(it)
            returnToConfig()
        }
        findViewById<ImageButton>(R.id.btnResetMatch).setOnClickListener {
            triggerUiFeedback(it)
            val lvl = activeCampaignLevel
            if (isCampaignMode && lvl != null) {
                startCampaignLevel(lvl)
            } else {
                startCustomMatch()
            }
        }

        btnOverlaySettings.setOnClickListener {
            triggerUiFeedback(it)
            winnerOverlay.visibility = View.GONE
            returnToConfig()
        }

        gameView.onTurnCompleted = {
            updateArenaHud()
            if (gameView.engine?.isGameOver == true) {
                gameView.postDelayed({ showVictoryModal() }, 380L)
            }
        }
    }

    private fun triggerUiFeedback(view: View? = null) {
        gameView.audio.uiClick()
        gameView.haptics.uiTick(view)
    }

    private fun switchMainMode(campaign: Boolean) {
        isCampaignMode = campaign
        if (campaign) {
            btnTabCampaign.setBackgroundColor(0xFF38BDF8.toInt())
            btnTabCampaign.setTextColor(Color.BLACK)
            btnTabCustom.setBackgroundColor(0xFF121824.toInt())
            btnTabCustom.setTextColor(Color.WHITE)
            campaignSection.visibility = View.VISIBLE
            customPlaySection.visibility = View.GONE
            refreshProfileAndCampaignUI()
        } else {
            btnTabCustom.setBackgroundColor(0xFF38BDF8.toInt())
            btnTabCustom.setTextColor(Color.BLACK)
            btnTabCampaign.setBackgroundColor(0xFF121824.toInt())
            btnTabCampaign.setTextColor(Color.WHITE)
            campaignSection.visibility = View.GONE
            customPlaySection.visibility = View.VISIBLE
        }
    }

    private fun refreshProfileAndCampaignUI() {
        tvRankBadge.text = campaignManager.getPlayerRankTitle()
        tvTotalStars.text = "⭐ ${campaignManager.getTotalStars()}/90"
        tvTotalCoins.text = "🪙 ${campaignManager.totalCoins}"

        campaignLevelsContainer.removeAllViews()
        val unlocked = campaignManager.unlockedLevel

        campaignManager.levels.forEach { lvl ->
            val isUnlocked = lvl.number <= unlocked
            val stars = campaignManager.getStarsForLevel(lvl.number)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 14, 16, 14)
                val bgColor = when {
                    !isUnlocked -> 0xFF0E131D.toInt()
                    lvl.isBoss -> 0xFF2A1215.toInt()
                    lvl.number == unlocked -> 0xFF16273E.toInt()
                    else -> 0xFF121824.toInt()
                }
                setBackgroundColor(bgColor)
                gravity = Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 6, 0, 6) }
                layoutParams = lp

                if (isUnlocked) {
                    setOnClickListener {
                        triggerUiFeedback(this)
                        startCampaignLevel(lvl)
                    }
                }
            }

            // Left Level Badge
            val badgeText = TextView(this).apply {
                text = if (!isUnlocked) "🔒\nL${lvl.number}" else if (lvl.isBoss) "💀\nL${lvl.number}" else "L${lvl.number}"
                setTextColor(if (lvl.isBoss) 0xFFEF4444.toInt() else if (isUnlocked) 0xFF38BDF8.toInt() else 0xFF475569.toInt())
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(88, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            // Middle Info Column
            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(12, 0, 12, 0)
                }
            }

            val titleTv = TextView(this).apply {
                text = lvl.title
                setTextColor(if (isUnlocked) Color.WHITE else 0xFF64748B.toInt())
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }

            val subTv = TextView(this).apply {
                val bonusInfo = buildString {
                    append("${lvl.cols}x${lvl.rows} • ")
                    if (lvl.gold2xBoxes > 0) append("★${lvl.gold2xBoxes}x2X ")
                    if (lvl.diamond3xBoxes > 0) append("💎${lvl.diamond3xBoxes}x3X ")
                    if (lvl.wallPattern != WallPattern.NONE) append("• ${lvl.wallPattern.tag}")
                }
                text = bonusInfo
                setTextColor(if (isUnlocked) 0xFF94A3B8.toInt() else 0xFF475569.toInt())
                textSize = 11f
            }

            infoCol.addView(titleTv)
            infoCol.addView(subTv)

            // Right Stars / Play Button
            val rightCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
            }

            val starsTv = TextView(this).apply {
                text = when (stars) {
                    3 -> "⭐⭐⭐"
                    2 -> "⭐⭐☆"
                    1 -> "⭐☆☆"
                    else -> if (isUnlocked) "☆☆☆" else "LOCKED"
                }
                setTextColor(if (stars > 0) 0xFFF59E0B.toInt() else 0xFF64748B.toInt())
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val rewardTv = TextView(this).apply {
                text = if (isUnlocked) "🪙 +${lvl.baseCoinReward} ▶" else "Clear L${lvl.number - 1}"
                setTextColor(if (isUnlocked) 0xFF10B981.toInt() else 0xFF475569.toInt())
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            }

            rightCol.addView(starsTv)
            rightCol.addView(rewardTv)

            card.addView(badgeText)
            card.addView(infoCol)
            card.addView(rightCol)
            campaignLevelsContainer.addView(card)
        }
    }

    private fun startCampaignLevel(lvl: CampaignLevel) {
        isCampaignMode = true
        activeCampaignLevel = lvl

        val campaignPlayers = mutableListOf<Player>()
        // Human Player 1
        campaignPlayers.add(Player(0, "You", playerColors[0], BotDifficulty.NONE))
        // Add Level Bots
        lvl.bots.forEachIndexed { idx, botPair ->
            val color = playerColors[(idx + 1) % playerColors.size]
            campaignPlayers.add(Player(idx + 1, botPair.first, color, botPair.second))
        }

        gameView.audio.isEnabled = switchSound.isChecked
        gameView.haptics.isEnabled = switchVibration.isChecked
        gameView.resetHighlights()

        winnerOverlay.visibility = View.GONE
        configContainer.visibility = View.GONE
        arenaContainer.visibility = View.VISIBLE

        tvArenaStageTitle.text = "STAGE ${lvl.number}/30 • ${lvl.title.uppercase()} (🪙 +${lvl.baseCoinReward})"

        val engine = GameEngine(
            rows = lvl.rows,
            cols = lvl.cols,
            players = campaignPlayers,
            gold2xCount = lvl.gold2xBoxes,
            diamond3xCount = lvl.diamond3xBoxes,
            wallPattern = lvl.wallPattern,
            seed = lvl.number * 997
        )
        gameView.engine = engine
        engine.startTurnClock()
        gameView.invalidate()

        updateArenaHud()
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
                    triggerUiFeedback(this)
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
                    triggerUiFeedback(this)
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
            val initialDiff = if (i == 1 && playerCount == 2) BotDifficulty.MEDIUM else BotDifficulty.NONE
            val defaultName = when (initialDiff) {
                BotDifficulty.EASY -> "Bot Easy"
                BotDifficulty.MEDIUM -> "Bot Alpha"
                BotDifficulty.PRO -> "Pro AI"
                BotDifficulty.NONE -> "Player ${i + 1}"
            }
            val player = Player(i, defaultName, color, initialDiff)
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

            val btnMode = Button(this).apply {
                text = player.botDifficulty.label
                textSize = 11f
                setBackgroundColor(player.botDifficulty.badgeColor)
                setTextColor(Color.WHITE)
                setOnClickListener {
                    triggerUiFeedback(this)
                    player.botDifficulty = player.botDifficulty.next()
                    text = player.botDifficulty.label
                    setBackgroundColor(player.botDifficulty.badgeColor)

                    val currentText = etName.text.toString().trim()
                    if (currentText.startsWith("Player") || currentText.startsWith("Bot") || currentText.startsWith("Pro")) {
                        val updatedName = when (player.botDifficulty) {
                            BotDifficulty.NONE -> "Player ${i + 1}"
                            BotDifficulty.EASY -> "Bot Easy"
                            BotDifficulty.MEDIUM -> "Bot Med"
                            BotDifficulty.PRO -> "Pro AI"
                        }
                        etName.setText(updatedName)
                        player.name = updatedName
                    }
                }
            }

            row.addView(colorBadge)
            row.addView(etName)
            row.addView(btnMode)
            rosterLayout.addView(row)
        }
    }

    private fun startCustomMatch() {
        isCampaignMode = false
        activeCampaignLevel = null

        for (i in 0 until rosterLayout.childCount) {
            val row = rosterLayout.getChildAt(i) as? LinearLayout ?: continue
            val etName = row.getChildAt(1) as? EditText ?: continue
            val inputName = etName.text.toString().trim()
            if (inputName.isNotEmpty() && i < activePlayers.size) {
                activePlayers[i].name = inputName
            }
        }

        gameView.audio.isEnabled = switchSound.isChecked
        gameView.haptics.isEnabled = switchVibration.isChecked
        gameView.resetHighlights()

        winnerOverlay.visibility = View.GONE
        configContainer.visibility = View.GONE
        arenaContainer.visibility = View.VISIBLE

        tvArenaStageTitle.text = "CUSTOM MATCH • ${selectedCols}x${selectedRows} ARENA"

        activePlayers.forEach {
            it.score = 0
            it.totalTimeMillis = 0L
            it.movesCount = 0
        }
        val engine = GameEngine(
            rows = selectedRows,
            cols = selectedCols,
            players = activePlayers,
            gold2xCount = 2,
            diamond3xCount = 1,
            wallPattern = WallPattern.NONE,
            seed = System.currentTimeMillis().toInt()
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
        refreshProfileAndCampaignUI()
    }

    private fun updateArenaHud() {
        val eng = gameView.engine ?: return
        val current = eng.getCurrentPlayer()
        val modeTag = when (current.botDifficulty) {
            BotDifficulty.EASY -> " [EASY]"
            BotDifficulty.MEDIUM -> " [MED]"
            BotDifficulty.PRO -> " [PRO]"
            BotDifficulty.NONE -> ""
        }
        tvTurnIndicator.text = "${current.name}$modeTag's Turn"
        tvTurnIndicator.setTextColor(current.color)

        val totalHumanMillis = eng.players.filter { !it.isBot }.sumOf { it.totalTimeMillis }

        arenaScoresLayout.removeAllViews()
        eng.players.forEach { p ->
            val tv = TextView(this).apply {
                val baseText = "${p.name}: ${p.score} pts"
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
        val lvl = activeCampaignLevel

        if (isCampaignMode && lvl != null) {
            val humanWon = (winner.id == 0)
            if (humanWon) {
                val (stars, coinsEarned) = campaignManager.recordVictory(
                    lvl,
                    winner.score,
                    eng.getTotalBoardPoints()
                )
                tvWinnerStars.text = when (stars) {
                    3 -> "⭐⭐⭐"
                    2 -> "⭐⭐☆"
                    else -> "⭐☆☆"
                }
                tvWinnerTitle.text = "LEVEL ${lvl.number} CLEARED!"
                tvWinnerTitle.setTextColor(0xFF10B981.toInt())
                tvWinnerSubtitle.text = "🪙 +$coinsEarned Gold Coins  •  ${ campaignManager.getPlayerRankTitle() }"

                if (lvl.number < 30) {
                    val nextLvl = campaignManager.levels[lvl.number] // index = lvl.number for next
                    btnOverlayPrimary.text = "NEXT: LEVEL ${nextLvl.number} ⏭"
                    btnOverlayPrimary.setOnClickListener {
                        triggerUiFeedback(it)
                        winnerOverlay.visibility = View.GONE
                        startCampaignLevel(nextLvl)
                    }
                } else {
                    btnOverlayPrimary.text = "👑 CAMPAIGN COMPLETED! REPLAY"
                    btnOverlayPrimary.setOnClickListener {
                        triggerUiFeedback(it)
                        winnerOverlay.visibility = View.GONE
                        startCampaignLevel(lvl)
                    }
                }
            } else {
                tvWinnerStars.text = "💀"
                tvWinnerTitle.text = "STAGE ${lvl.number} FAILED"
                tvWinnerTitle.setTextColor(0xFFEF4444.toInt())
                tvWinnerSubtitle.text = "${winner.name} won with ${winner.score} pts. Try again!"

                btnOverlayPrimary.text = "RETRY LEVEL ${lvl.number} 🔄"
                btnOverlayPrimary.setOnClickListener {
                    triggerUiFeedback(it)
                    winnerOverlay.visibility = View.GONE
                    startCampaignLevel(lvl)
                }
            }
        } else {
            tvWinnerStars.text = "🏆"
            tvWinnerTitle.text = "${winner.name.uppercase()} WINS!"
            tvWinnerTitle.setTextColor(winner.color)
            tvWinnerSubtitle.text = "Scored ${winner.score} total points"

            btnOverlayPrimary.text = "PLAY AGAIN 🔄"
            btnOverlayPrimary.setOnClickListener {
                triggerUiFeedback(it)
                winnerOverlay.visibility = View.GONE
                startCustomMatch()
            }
        }

        // Render Standings
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

            val aiSuffix = when (p.botDifficulty) {
                BotDifficulty.EASY -> " (Easy)"
                BotDifficulty.MEDIUM -> " (Med)"
                BotDifficulty.PRO -> " (Pro AI)"
                BotDifficulty.NONE -> ""
            }

            val nameView = TextView(this).apply {
                text = "${p.name}$aiSuffix"
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

        // Render Speed IQ Analytics
        winnerAnalyticsLayout.removeAllViews()
        val humanPlayers = eng.players.filter { !it.isBot }
        if (switchSpeedAnalytics.isChecked && humanPlayers.isNotEmpty()) {
            tvAnalyticsHeading.visibility = View.VISIBLE
            winnerAnalyticsLayout.visibility = View.VISIBLE

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