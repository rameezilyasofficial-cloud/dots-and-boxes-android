package com.fixtocode.dotsandboxes

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var configContainer: View
    private lateinit var arenaContainer: View
    private lateinit var gridOptionsLayout: LinearLayout
    private lateinit var rosterLayout: LinearLayout
    private lateinit var playerSeekBar: SeekBar
    private lateinit var tvPlayerCount: TextView
    private lateinit var tvTurnIndicator: TextView
    private lateinit var arenaScoresLayout: LinearLayout
    private lateinit var gameView: GameView

    private val playerColors = intArrayOf(
        0xFFEF4444.toInt(),
        0xFFF59E0B.toInt(),
        0xFF06B6D4.toInt(),
        0xFF10B981.toInt(),
        0xFF8B5CF6.toInt()
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
        rosterLayout = findViewById(R.id.rosterLayout)
        playerSeekBar = findViewById(R.id.playerSeekBar)
        tvPlayerCount = findViewById(R.id.tvPlayerCount)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)
        arenaScoresLayout = findViewById(R.id.arenaScoresLayout)
        gameView = findViewById(R.id.gameView)

        setupAdaptiveGridOptions()
        setupPlayerSlider()
        renderRoster()

        findViewById<Button>(R.id.btnStartMatch).setOnClickListener { startMatch() }
        findViewById<ImageButton>(R.id.btnLeaveArena).setOnClickListener { returnToConfig() }
        findViewById<ImageButton>(R.id.btnResetMatch).setOnClickListener { startMatch() }

        gameView.onTurnCompleted = {
            updateArenaHud()
            if (gameView.engine?.isGameOver == true) {
                showVictoryModal()
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
            tvDevice.text = "TABLET HARDWARE DETECTED (EXPANDED ARENA)"
            listOf("5x5" to Pair(5, 5), "6x6" to Pair(6, 6), "8x8" to Pair(8, 8), "10x10" to Pair(10, 10))
        } else {
            tvDevice.text = "MOBILE HARDWARE DETECTED (COMPACT ARENA)"
            listOf("3x3" to Pair(3, 3), "4x4" to Pair(4, 4), "4x5" to Pair(4, 5), "5x5" to Pair(5, 5))
        }

        presets.forEachIndexed { idx, pair ->
            val btn = Button(this).apply {
                text = pair.first
                textSize = 12f
                setTextColor(if (idx == 1) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (idx == 1) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
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
            if (idx == 1) {
                selectedRows = pair.second.first
                selectedCols = pair.second.second
            }
            gridOptionsLayout.addView(btn)
        }
    }

    private fun setupPlayerSlider() {
        playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                playerCount = progress
                tvPlayerCount.text = "$playerCount Players"
                renderRoster()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun renderRoster() {
        rosterLayout.removeAllViews()
        activePlayers.clear()

        for (i in 0 until playerCount) {
            val color = playerColors[i]
            val isBot = (i == 1)
            val defaultName = if (i == 0) "Player 1" else if (i == 1) "Bot Alpha" else "Player ${i + 1}"
            val player = Player(i, defaultName, color, isBot)
            activePlayers.add(player)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12, 12, 12, 12)
                setBackgroundColor(0xFF121824.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
                layoutParams = lp
            }

            val colorBadge = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).apply { setMargins(8, 0, 16, 0) }
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
        configContainer.visibility = View.GONE
        arenaContainer.visibility = View.VISIBLE

        activePlayers.forEach { it.score = 0 }
        val engine = GameEngine(selectedRows, selectedCols, activePlayers)
        gameView.engine = engine
        gameView.invalidate()

        updateArenaHud()

        if (engine.getCurrentPlayer().isBot) {
            gameView.postDelayed({ gameView.runBotTurn() }, 400)
        }
    }

    private fun returnToConfig() {
        arenaContainer.visibility = View.GONE
        configContainer.visibility = View.VISIBLE
    }

    private fun updateArenaHud() {
        val eng = gameView.engine ?: return
        val current = eng.getCurrentPlayer()
        tvTurnIndicator.text = "${current.name}'s Turn"
        tvTurnIndicator.setTextColor(current.color)

        arenaScoresLayout.removeAllViews()
        eng.players.forEach { p ->
            val tv = TextView(this).apply {
                text = "${p.name}: ${p.score}  "
                textSize = 12f
                setTextColor(if (p.id == current.id) p.color else Color.LTGRAY)
                setPadding(16, 4, 16, 4)
            }
            arenaScoresLayout.addView(tv)
        }
    }

    private fun showVictoryModal() {
        val eng = gameView.engine ?: return
        val winner = eng.players.maxByOrNull { it.score } ?: return

        AlertDialog.Builder(this)
            .setTitle("VICTORY!")
            .setMessage("${winner.name} dominates the board with ${winner.score} boxes!")
            .setPositiveButton("PLAY AGAIN") { _, _ -> startMatch() }
            .setNegativeButton("SETTINGS") { _, _ -> returnToConfig() }
            .setCancelable(false)
            .show()
    }
}