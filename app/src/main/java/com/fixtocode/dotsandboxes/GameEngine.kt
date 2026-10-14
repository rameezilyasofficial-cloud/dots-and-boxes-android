package com.fixtocode.dotsandboxes

import kotlin.random.Random

enum class BotDifficulty(val label: String, val badgeColor: Int) {
    NONE("👤 HUMAN", 0xFF1E293B.toInt()),
    EASY("🤖 EASY", 0xFF065F46.toInt()),
    MEDIUM("🤖 MED", 0xFF4C1D95.toInt()),
    PRO("🔥 PRO AI", 0xFF991B1B.toInt());

    fun next(): BotDifficulty {
        val vals = values()
        return vals[(ordinal + 1) % vals.size]
    }
}

data class Player(
    val id: Int,
    var name: String,
    val color: Int,
    var botDifficulty: BotDifficulty = BotDifficulty.NONE,
    var score: Int = 0,
    var totalTimeMillis: Long = 0L,
    var movesCount: Int = 0
) {
    val isBot: Boolean
        get() = botDifficulty != BotDifficulty.NONE

    fun getAverageResponseSec(): Double {
        return if (movesCount > 0) (totalTimeMillis / 1000.0) / movesCount else 0.0
    }
}

class GameEngine(
    val rows: Int,
    val cols: Int,
    val players: List<Player>,
    val gold2xCount: Int = 0,
    val diamond3xCount: Int = 0,
    val wallPattern: WallPattern = WallPattern.NONE,
    val seed: Int = (rows * 31 + cols * 17)
) {
    // null = empty, -1 = neutral pre-placed wall, >=0 = player ID
    val hLines = Array(rows + 1) { arrayOfNulls<Int>(cols) }
    val vLines = Array(rows) { arrayOfNulls<Int>(cols + 1) }
    val boxes = Array(rows) { arrayOfNulls<Int>(cols) }

    // Value of each box: 1 (Normal), 2 (Gold 2X), 3 (Diamond 3X)
    val boxValues = Array(rows) { IntArray(cols) { 1 } }

    var currentPlayerIdx = 0
    var isGameOver = false
    var currentComboStreak = 0
    var turnStartTimeMillis: Long = System.currentTimeMillis()

    init {
        setupBonusBoxes()
        applyWallPattern()
    }

    private fun setupBonusBoxes() {
        val coords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                coords.add(Pair(r, c))
            }
        }
        coords.shuffle(Random(seed))

        var idx = 0
        for (i in 0 until diamond3xCount) {
            if (idx < coords.size) {
                val (r, c) = coords[idx++]
                boxValues[r][c] = 3
            }
        }
        for (i in 0 until gold2xCount) {
            if (idx < coords.size) {
                val (r, c) = coords[idx++]
                boxValues[r][c] = 2
            }
        }
    }

    private fun applyWallPattern() {
        val midR = rows / 2
        val midC = cols / 2

        when (wallPattern) {
            WallPattern.NONE -> {}
            WallPattern.CENTER_CROSS -> {
                if (cols >= 3) {
                    hLines[midR][midC] = -1
                    if (midC > 0) hLines[midR][midC - 1] = -1
                }
                if (rows >= 3) {
                    vLines[midR][midC] = -1
                    if (midR > 0) vLines[midR - 1][midC] = -1
                }
            }
            WallPattern.CORNERS -> {
                hLines[1][0] = -1
                hLines[1][cols - 1] = -1
                hLines[rows - 1][0] = -1
                hLines[rows - 1][cols - 1] = -1
            }
            WallPattern.CORRIDOR -> {
                for (r in 1 until rows - 1) {
                    if (r % 2 == 1 && cols >= 4) {
                        vLines[r][1] = -1
                        vLines[r][cols - 1] = -1
                    }
                }
            }
            WallPattern.FORTRESS -> {
                if (rows >= 4 && cols >= 4) {
                    hLines[1][midC] = -1
                    hLines[rows - 1][midC] = -1
                    vLines[midR][1] = -1
                    vLines[midR][cols - 1] = -1
                }
            }
        }
    }

    fun getTotalBoardPoints(): Int {
        var total = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                total += boxValues[r][c]
            }
        }
        return total
    }

    fun getCurrentPlayer(): Player = players[currentPlayerIdx]

    fun startTurnClock() {
        turnStartTimeMillis = System.currentTimeMillis()
    }

    /**
     * Claims a line and returns the number of boxes closed (0, 1, or 2), or -1 if invalid.
     * Player score increases by the sum of boxValues[r][c] of captured boxes.
     */
    fun claimLine(isH: Boolean, r: Int, c: Int): Int {
        if (isH && hLines[r][c] != null) return -1
        if (!isH && vLines[r][c] != null) return -1

        val p = getCurrentPlayer()

        if (!p.isBot) {
            val elapsed = System.currentTimeMillis() - turnStartTimeMillis
            p.totalTimeMillis += elapsed
            p.movesCount++
        }

        if (isH) hLines[r][c] = p.id else vLines[r][c] = p.id

        val (boxesClosed, pointsEarned) = evaluateBoxes(isH, r, c, p.id)
        if (boxesClosed > 0) {
            p.score += pointsEarned
            currentComboStreak += boxesClosed
            if (checkBoardFull()) {
                isGameOver = true
            } else {
                startTurnClock()
            }
        } else {
            currentComboStreak = 0
            currentPlayerIdx = (currentPlayerIdx + 1) % players.size
            startTurnClock()
        }
        return boxesClosed
    }

    private fun evaluateBoxes(isH: Boolean, r: Int, c: Int, playerId: Int): Pair<Int, Int> {
        var closedCount = 0
        var pointsEarned = 0

        fun isClosed(row: Int, col: Int): Boolean {
            return hLines[row][col] != null &&
                   hLines[row + 1][col] != null &&
                   vLines[row][col] != null &&
                   vLines[row][col + 1] != null
        }

        if (isH) {
            if (r > 0 && boxes[r - 1][c] == null && isClosed(r - 1, c)) {
                boxes[r - 1][c] = playerId
                closedCount++
                pointsEarned += boxValues[r - 1][c]
            }
            if (r < rows && boxes[r][c] == null && isClosed(r, c)) {
                boxes[r][c] = playerId
                closedCount++
                pointsEarned += boxValues[r][c]
            }
        } else {
            if (c > 0 && boxes[r][c - 1] == null && isClosed(r, c - 1)) {
                boxes[r][c - 1] = playerId
                closedCount++
                pointsEarned += boxValues[r][c - 1]
            }
            if (c < cols && boxes[r][c] == null && isClosed(r, c)) {
                boxes[r][c] = playerId
                closedCount++
                pointsEarned += boxValues[r][c]
            }
        }
        return Pair(closedCount, pointsEarned)
    }

    private fun checkBoardFull(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (boxes[r][c] == null) return false
            }
        }
        return true
    }

    data class Move(val isH: Boolean, val r: Int, val c: Int)

    private fun countBoxSides(r: Int, c: Int): Int {
        var count = 0
        if (hLines[r][c] != null) count++
        if (hLines[r + 1][c] != null) count++
        if (vLines[r][c] != null) count++
        if (vLines[r][c + 1] != null) count++
        return count
    }

    private fun pointsCompletedByMove(m: Move): Int {
        var pts = 0
        if (m.isH) {
            if (m.r > 0 && countBoxSides(m.r - 1, m.c) == 3) pts += boxValues[m.r - 1][m.c]
            if (m.r < rows && countBoxSides(m.r, m.c) == 3) pts += boxValues[m.r][m.c]
        } else {
            if (m.c > 0 && countBoxSides(m.r, m.c - 1) == 3) pts += boxValues[m.r][m.c - 1]
            if (m.c < cols && countBoxSides(m.r, m.c) == 3) pts += boxValues[m.r][m.c]
        }
        return pts
    }

    private fun moveCreatesThirdSide(m: Move): Boolean {
        return if (m.isH) {
            (m.r > 0 && countBoxSides(m.r - 1, m.c) == 2) ||
            (m.r < rows && countBoxSides(m.r, m.c) == 2)
        } else {
            (m.c > 0 && countBoxSides(m.r, m.c - 1) == 2) ||
            (m.c < cols && countBoxSides(m.r, m.c) == 2)
        }
    }

    private fun maxAdjacentSidesBeforeMove(m: Move): Int {
        return if (m.isH) {
            val top = if (m.r > 0) countBoxSides(m.r - 1, m.c) else 0
            val bot = if (m.r < rows) countBoxSides(m.r, m.c) else 0
            maxOf(top, bot)
        } else {
            val left = if (m.c > 0) countBoxSides(m.r, m.c - 1) else 0
            val right = if (m.c < cols) countBoxSides(m.r, m.c) else 0
            maxOf(left, right)
        }
    }

    private fun simulateOpponentChainPoints(candidate: Move): Int {
        val simH = Array(rows + 1) { r -> BooleanArray(cols) { c -> hLines[r][c] != null } }
        val simV = Array(rows) { r -> BooleanArray(cols + 1) { c -> vLines[r][c] != null } }

        if (candidate.isH) {
            simH[candidate.r][candidate.c] = true
        } else {
            simV[candidate.r][candidate.c] = true
        }

        var sacrificedPoints = 0
        var foundCapturableBox = true

        while (foundCapturableBox) {
            foundCapturableBox = false
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    var sides = 0
                    if (simH[r][c]) sides++
                    if (simH[r + 1][c]) sides++
                    if (simV[r][c]) sides++
                    if (simV[r][c + 1]) sides++

                    if (sides == 3) {
                        if (!simH[r][c]) simH[r][c] = true
                        else if (!simH[r + 1][c]) simH[r + 1][c] = true
                        else if (!simV[r][c]) simV[r][c] = true
                        else if (!simV[r][c + 1]) simV[r][c + 1] = true

                        sacrificedPoints += boxValues[r][c]
                        foundCapturableBox = true
                    }
                }
            }
        }
        return sacrificedPoints
    }

    fun calculateBotMove(): Move? {
        val openMoves = mutableListOf<Move>()
        for (r in 0..rows) {
            for (c in 0 until cols) {
                if (hLines[r][c] == null) openMoves.add(Move(true, r, c))
            }
        }
        for (r in 0 until rows) {
            for (c in 0..cols) {
                if (vLines[r][c] == null) openMoves.add(Move(false, r, c))
            }
        }
        if (openMoves.isEmpty()) return null

        val difficulty = getCurrentPlayer().botDifficulty

        // TIER 1: EASY BOT
        if (difficulty == BotDifficulty.EASY) {
            val scoringMoves = openMoves.filter { pointsCompletedByMove(it) > 0 }
            if (scoringMoves.isNotEmpty() && Math.random() < 0.65) {
                return scoringMoves.random()
            }
            val safeMoves = openMoves.filter { !moveCreatesThirdSide(it) }
            if (safeMoves.isNotEmpty() && Math.random() < 0.55) {
                return safeMoves.random()
            }
            return openMoves.random()
        }

        // TIER 2: MEDIUM BOT
        if (difficulty == BotDifficulty.MEDIUM) {
            val scoringMoves = openMoves.filter { pointsCompletedByMove(it) > 0 }
            if (scoringMoves.isNotEmpty()) {
                return scoringMoves.maxByOrNull { pointsCompletedByMove(it) }
            }
            val safeMoves = openMoves.filter { !moveCreatesThirdSide(it) }
            return if (safeMoves.isNotEmpty()) safeMoves.random() else openMoves.random()
        }

        // TIER 3: PRO AI (Hunts 3X/2X Boxes + Tempo Control + Minimax Value Sacrifice)
        val scoringMoves = openMoves.filter { pointsCompletedByMove(it) > 0 }
        if (scoringMoves.isNotEmpty()) {
            val maxPts = scoringMoves.maxOf { pointsCompletedByMove(it) }
            return scoringMoves.filter { pointsCompletedByMove(it) == maxPts }.random()
        }

        val safeMoves = openMoves.filter { !moveCreatesThirdSide(it) }
        if (safeMoves.isNotEmpty()) {
            val minAdjacentSides = safeMoves.minOf { maxAdjacentSidesBeforeMove(it) }
            val optimalSafeMoves = safeMoves.filter { maxAdjacentSidesBeforeMove(it) == minAdjacentSides }
            return optimalSafeMoves.random()
        }

        var minSacrificePts = Int.MAX_VALUE
        val bestSacrificeCandidates = mutableListOf<Move>()

        for (candidate in openMoves) {
            val chainCost = simulateOpponentChainPoints(candidate)
            if (chainCost < minSacrificePts) {
                minSacrificePts = chainCost
                bestSacrificeCandidates.clear()
                bestSacrificeCandidates.add(candidate)
            } else if (chainCost == minSacrificePts) {
                bestSacrificeCandidates.add(candidate)
            }
        }

        return if (bestSacrificeCandidates.isNotEmpty()) {
            bestSacrificeCandidates.random()
        } else {
            openMoves.random()
        }
    }
}