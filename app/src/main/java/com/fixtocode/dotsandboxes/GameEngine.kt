package com.fixtocode.dotsandboxes

data class LevelSpec(
    val number: Int,
    val title: String,
    val rows: Int,
    val cols: Int,
    val playerCount: Int,
    val pointsPerBox: Int,
    val winBonus: Int,
    val aiSkillLevel: Int
)

object LevelRepository {
    val levels: List<LevelSpec> = (1..30).map { lvl ->
        // Starts at 5x5 from Level 1 as requested, scaling up to 5x7 and 5x8
        val (r, c) = when {
            lvl <= 15 -> Pair(5, 5) // 5x5 default starting grid
            lvl <= 23 -> Pair(7, 5) // 5x7
            else -> Pair(8, 5)      // 5x8
        }
        val pCount = when {
            lvl in listOf(6, 12, 18, 24) -> 3
            lvl in listOf(14, 22, 28) -> 4
            lvl in listOf(20, 29, 30) -> 5
            else -> 2
        }
        val title = when (lvl) {
            1 -> "Rookie Start"
            5 -> "Boss: Iron Grid"
            10 -> "Boss: Cyber Core"
            15 -> "Boss: Nexus AI"
            20 -> "Boss: Penta War"
            25 -> "Boss: Deep Matrix"
            30 -> "Final Boss: Omega"
            else -> "Stage $lvl"
        }
        LevelSpec(
            number = lvl,
            title = title,
            rows = r,
            cols = c,
            playerCount = pCount,
            pointsPerBox = lvl * 10,
            winBonus = lvl * 50,
            aiSkillLevel = lvl
        )
    }
}

data class Player(
    val id: Int,
    var name: String,
    val color: Int,
    var isBot: Boolean = false,
    var score: Int = 0,
    var boxesCount: Int = 0,
    var totalTimeMillis: Long = 0L,
    var movesCount: Int = 0
) {
    fun getAverageResponseSec(): Double {
        return if (movesCount > 0) (totalTimeMillis / 1000.0) / movesCount else 0.0
    }
}

class GameEngine(
    val rows: Int,
    val cols: Int,
    val players: List<Player>,
    val pointsPerBox: Int = 1,
    val aiSkillLevel: Int = 15
) {
    val hLines = Array(rows + 1) { arrayOfNulls<Int>(cols) }
    val vLines = Array(rows) { arrayOfNulls<Int>(cols + 1) }
    val boxes = Array(rows) { arrayOfNulls<Int>(cols) }

    var currentPlayerIdx = 0
    var isGameOver = false
    var turnStartTimeMillis: Long = System.currentTimeMillis()

    fun getCurrentPlayer(): Player = players[currentPlayerIdx]

    fun startTurnClock() {
        turnStartTimeMillis = System.currentTimeMillis()
    }

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

        val captured = evaluateBoxes(isH, r, c, p.id)
        if (captured > 0) {
            p.boxesCount += captured
            p.score += (captured * pointsPerBox)
            if (checkBoardFull()) {
                isGameOver = true
            } else {
                startTurnClock()
            }
        } else {
            currentPlayerIdx = (currentPlayerIdx + 1) % players.size
            startTurnClock()
        }
        return captured
    }

    private fun evaluateBoxes(isH: Boolean, r: Int, c: Int, playerId: Int): Int {
        var count = 0
        fun isClosed(row: Int, col: Int): Boolean {
            return hLines[row][col] != null &&
                   hLines[row + 1][col] != null &&
                   vLines[row][col] != null &&
                   vLines[row][col + 1] != null
        }

        if (isH) {
            if (r > 0 && boxes[r - 1][c] == null && isClosed(r - 1, c)) {
                boxes[r - 1][c] = playerId
                count++
            }
            if (r < rows && boxes[r][c] == null && isClosed(r, c)) {
                boxes[r][c] = playerId
                count++
            }
        } else {
            if (c > 0 && boxes[r][c - 1] == null && isClosed(r, c - 1)) {
                boxes[r][c - 1] = playerId
                count++
            }
            if (c < cols && boxes[r][c] == null && isClosed(r, c)) {
                boxes[r][c] = playerId
                count++
            }
        }
        return count
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

    private fun countSides(r: Int, c: Int): Int {
        var count = 0
        if (hLines[r][c] != null) count++
        if (hLines[r + 1][c] != null) count++
        if (vLines[r][c] != null) count++
        if (vLines[r][c + 1] != null) count++
        return count
    }

    private fun boxesCompleted(m: Move): Int {
        var c = 0
        if (m.isH) {
            if (m.r > 0 && countSides(m.r - 1, m.c) == 3) c++
            if (m.r < rows && countSides(m.r, m.c) == 3) c++
        } else {
            if (m.c > 0 && countSides(m.r, m.c - 1) == 3) c++
            if (m.c < cols && countSides(m.r, m.c) == 3) c++
        }
        return c
    }

    private fun creates3rdSide(m: Move): Boolean {
        return if (m.isH) {
            (m.r > 0 && countSides(m.r - 1, m.c) == 2) ||
            (m.r < rows && countSides(m.r, m.c) == 2)
        } else {
            (m.c > 0 && countSides(m.r, m.c - 1) == 2) ||
            (m.c < cols && countSides(m.r, m.c) == 2)
        }
    }

    private fun simulateSacrificeChain(candidate: Move): Int {
        val simH = Array(rows + 1) { r -> BooleanArray(cols) { c -> hLines[r][c] != null } }
        val simV = Array(rows) { r -> BooleanArray(cols + 1) { c -> vLines[r][c] != null } }

        if (candidate.isH) simH[candidate.r][candidate.c] = true
        else simV[candidate.r][candidate.c] = true

        var chain = 0
        var found = true
        while (found) {
            found = false
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    var s = 0
                    if (simH[r][c]) s++
                    if (simH[r + 1][c]) s++
                    if (simV[r][c]) s++
                    if (simV[r][c + 1]) s++
                    if (s == 3) {
                        if (!simH[r][c]) simH[r][c] = true
                        else if (!simH[r + 1][c]) simH[r + 1][c] = true
                        else if (!simV[r][c]) simV[r][c] = true
                        else if (!simV[r][c + 1]) simV[r][c + 1] = true
                        chain++
                        found = true
                    }
                }
            }
        }
        return chain
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

        val scoringMoves = openMoves.filter { boxesCompleted(it) > 0 }
        val captureChance = (0.55 + (aiSkillLevel * 0.02)).coerceAtMost(1.0)
        if (scoringMoves.isNotEmpty() && Math.random() <= captureChance) {
            return scoringMoves.maxByOrNull { boxesCompleted(it) }
        }

        val safeMoves = openMoves.filter { !creates3rdSide(it) }
        val safeChance = (0.45 + (aiSkillLevel * 0.025)).coerceAtMost(1.0)
        if (safeMoves.isNotEmpty() && Math.random() <= safeChance) {
            return safeMoves.random()
        }

        if (aiSkillLevel >= 12) {
            return openMoves.minByOrNull { simulateSacrificeChain(it) } ?: openMoves.random()
        }

        return openMoves.random()
    }
}