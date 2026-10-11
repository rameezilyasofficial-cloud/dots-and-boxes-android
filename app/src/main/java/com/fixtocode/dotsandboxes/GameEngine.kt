package com.fixtocode.dotsandboxes

data class Player(
    val id: Int,
    var name: String,
    val color: Int,
    var isBot: Boolean = false,
    var score: Int = 0,
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
    val players: List<Player>
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
            p.score += captured
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

        fun countSides(r: Int, c: Int): Int {
            var count = 0
            if (hLines[r][c] != null) count++
            if (hLines[r + 1][c] != null) count++
            if (vLines[r][c] != null) count++
            if (vLines[r][c + 1] != null) count++
            return count
        }

        fun completesBox(m: Move): Boolean {
            return if (m.isH) {
                (m.r > 0 && countSides(m.r - 1, m.c) == 3) ||
                (m.r < rows && countSides(m.r, m.c) == 3)
            } else {
                (m.c > 0 && countSides(m.r, m.c - 1) == 3) ||
                (m.c < cols && countSides(m.r, m.c) == 3)
            }
        }

        for (m in openMoves) {
            if (completesBox(m)) return m
        }

        fun creates3rdSide(m: Move): Boolean {
            return if (m.isH) {
                (m.r > 0 && countSides(m.r - 1, m.c) == 2) ||
                (m.r < rows && countSides(m.r, m.c) == 2)
            } else {
                (m.c > 0 && countSides(m.r, m.c - 1) == 2) ||
                (m.c < cols && countSides(m.r, m.c) == 2)
            }
        }

        val safeMoves = openMoves.filter { !creates3rdSide(it) }
        return if (safeMoves.isNotEmpty()) safeMoves.random() else openMoves.random()
    }
}