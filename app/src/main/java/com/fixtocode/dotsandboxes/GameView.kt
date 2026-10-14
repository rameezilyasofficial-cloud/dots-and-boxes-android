package com.fixtocode.dotsandboxes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var engine: GameEngine? = null
    var onTurnCompleted: (() -> Unit)? = null
    val audio = AudioFx()
    val haptics = HapticEngine(context)

    private var lastPlacedIsH: Boolean? = null
    private var lastPlacedR: Int = -1
    private var lastPlacedC: Int = -1

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF64748B.toInt()
        style = Paint.Style.FILL
    }
    private val idleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E293B.toInt()
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val wallLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }
    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }
    private val highlightGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
    }
    private val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bonusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun resetHighlights() {
        lastPlacedIsH = null
        lastPlacedR = -1
        lastPlacedC = -1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        val padding = 38f
        val availW = width - (padding * 2)
        val availH = height - (padding * 2)
        val cellSize = min(availW / eng.cols, availH / eng.rows)

        val startX = (width - (cellSize * eng.cols)) / 2f
        val startY = (height - (cellSize * eng.rows)) / 2f

        // 1. Draw Unclaimed Bonus Boxes (2X Gold & 3X Diamond) and Claimed Boxes
        for (r in 0 until eng.rows) {
            for (c in 0 until eng.cols) {
                val ownerId = eng.boxes[r][c]
                val bonusVal = eng.boxValues[r][c]
                val left = startX + (c * cellSize)
                val top = startY + (r * cellSize)
                val rect = RectF(left + 4f, top + 4f, left + cellSize - 4f, top + cellSize - 4f)

                if (ownerId == null) {
                    // Unclaimed Box: Show 2X Gold or 3X Diamond Objective!
                    if (bonusVal == 2) {
                        boxFillPaint.color = 0x1AF59E0B
                        canvas.drawRoundRect(rect, 10f, 10f, boxFillPaint)
                        bonusStrokePaint.color = 0x66F59E0B
                        canvas.drawRoundRect(rect, 10f, 10f, bonusStrokePaint)

                        badgePaint.color = 0xFFF59E0B.toInt()
                        badgePaint.textSize = cellSize * 0.25f
                        val fm = badgePaint.fontMetrics
                        val ty = top + (cellSize / 2f) - ((fm.descent + fm.ascent) / 2f)
                        canvas.drawText("★ 2X", left + (cellSize / 2f), ty, badgePaint)
                    } else if (bonusVal == 3) {
                        boxFillPaint.color = 0x2238BDF8
                        canvas.drawRoundRect(rect, 10f, 10f, boxFillPaint)
                        bonusStrokePaint.color = 0x8838BDF8.toInt()
                        canvas.drawRoundRect(rect, 10f, 10f, bonusStrokePaint)

                        badgePaint.color = 0xFF38BDF8.toInt()
                        badgePaint.textSize = cellSize * 0.25f
                        val fm = badgePaint.fontMetrics
                        val ty = top + (cellSize / 2f) - ((fm.descent + fm.ascent) / 2f)
                        canvas.drawText("💎 3X", left + (cellSize / 2f), ty, badgePaint)
                    }
                } else {
                    // Claimed Box
                    val p = eng.players.firstOrNull { it.id == ownerId } ?: continue
                    boxFillPaint.color = (p.color and 0x00FFFFFF) or 0x33000000
                    canvas.drawRoundRect(rect, 10f, 10f, boxFillPaint)

                    val initialLetter = if (p.name.isNotBlank()) p.name.trim().take(1).uppercase() else "P"
                    textPaint.textSize = cellSize * 0.42f
                    textPaint.color = p.color

                    val fontMetrics = textPaint.fontMetrics
                    val textY = top + (cellSize / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
                    canvas.drawText(initialLetter, left + (cellSize / 2f), textY, textPaint)

                    if (bonusVal > 1) {
                        badgePaint.color = if (bonusVal == 3) 0xFF38BDF8.toInt() else 0xFFF59E0B.toInt()
                        badgePaint.textSize = cellSize * 0.18f
                        canvas.drawText("+${bonusVal}", left + cellSize * 0.78f, top + cellSize * 0.26f, badgePaint)
                    }
                }
            }
        }

        // 2. Horizontal Lines
        for (r in 0..eng.rows) {
            for (c in 0 until eng.cols) {
                val ownerId = eng.hLines[r][c]
                val x1 = startX + (c * cellSize)
                val x2 = x1 + cellSize
                val y = startY + (r * cellSize)

                if (ownerId == -1) {
                    canvas.drawLine(x1, y, x2, y, wallLinePaint)
                } else if (ownerId != null) {
                    val p = eng.players.firstOrNull { it.id == ownerId }
                    val lineColor = p?.color ?: 0xFF38BDF8.toInt()
                    if (lastPlacedIsH == true && lastPlacedR == r && lastPlacedC == c) {
                        highlightGlowPaint.color = (lineColor and 0x00FFFFFF) or 0x55000000
                        canvas.drawLine(x1, y, x2, y, highlightGlowPaint)
                    }
                    activeLinePaint.color = lineColor
                    canvas.drawLine(x1, y, x2, y, activeLinePaint)
                } else {
                    canvas.drawLine(x1, y, x2, y, idleLinePaint)
                }
            }
        }

        // 3. Vertical Lines
        for (r in 0 until eng.rows) {
            for (c in 0..eng.cols) {
                val ownerId = eng.vLines[r][c]
                val y1 = startY + (r * cellSize)
                val y2 = y1 + cellSize
                val x = startX + (c * cellSize)

                if (ownerId == -1) {
                    canvas.drawLine(x, y1, x, y2, wallLinePaint)
                } else if (ownerId != null) {
                    val p = eng.players.firstOrNull { it.id == ownerId }
                    val lineColor = p?.color ?: 0xFF38BDF8.toInt()
                    if (lastPlacedIsH == false && lastPlacedR == r && lastPlacedC == c) {
                        highlightGlowPaint.color = (lineColor and 0x00FFFFFF) or 0x55000000
                        canvas.drawLine(x, y1, x, y2, highlightGlowPaint)
                    }
                    activeLinePaint.color = lineColor
                    canvas.drawLine(x, y1, x, y2, activeLinePaint)
                } else {
                    canvas.drawLine(x, y1, x, y2, idleLinePaint)
                }
            }
        }

        // 4. Grid Pegs
        val dotRadius = min(11f, cellSize * 0.11f)
        for (r in 0..eng.rows) {
            for (c in 0..eng.cols) {
                val cx = startX + (c * cellSize)
                val cy = startY + (r * cellSize)
                canvas.drawCircle(cx, cy, dotRadius, dotPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)
        val eng = engine ?: return false
        if (eng.isGameOver || eng.getCurrentPlayer().isBot) return false

        val padding = 38f
        val availW = width - (padding * 2)
        val availH = height - (padding * 2)
        val cellSize = min(availW / eng.cols, availH / eng.rows)
        val startX = (width - (cellSize * eng.cols)) / 2f
        val startY = (height - (cellSize * eng.rows)) / 2f
        val threshold = cellSize * 0.4f

        val tx = event.x
        val ty = event.y

        for (r in 0..eng.rows) {
            for (c in 0 until eng.cols) {
                if (eng.hLines[r][c] != null) continue
                val x1 = startX + (c * cellSize)
                val x2 = x1 + cellSize
                val y = startY + (r * cellSize)

                if (tx in (x1 - 15f)..(x2 + 15f) && abs(ty - y) < threshold) {
                    executeMove(true, r, c, isBotMove = false)
                    return true
                }
            }
        }

        for (r in 0 until eng.rows) {
            for (c in 0..eng.cols) {
                if (eng.vLines[r][c] != null) continue
                val y1 = startY + (r * cellSize)
                val y2 = y1 + cellSize
                val x = startX + (c * cellSize)

                if (ty in (y1 - 15f)..(y2 + 15f) && abs(tx - x) < threshold) {
                    executeMove(false, r, c, isBotMove = false)
                    return true
                }
            }
        }
        return true
    }

    private fun executeMove(isH: Boolean, r: Int, c: Int, isBotMove: Boolean) {
        val eng = engine ?: return
        val captured = eng.claimLine(isH, r, c)
        if (captured >= 0) {
            lastPlacedIsH = isH
            lastPlacedR = r
            lastPlacedC = c

            if (captured > 0) {
                audio.boxClaimed(eng.currentComboStreak, captured)
                haptics.boxCaptured(eng.currentComboStreak, captured)
            } else {
                if (isBotMove) {
                    audio.botSnap()
                    haptics.botStep()
                } else {
                    audio.snap()
                    haptics.lineSnap(this)
                }
            }

            invalidate()
            onTurnCompleted?.invoke()

            if (eng.isGameOver) {
                postDelayed({
                    audio.victory()
                    haptics.victoryFanfare()
                }, 180L)
            } else if (eng.getCurrentPlayer().isBot) {
                val thinkDelay = when (eng.getCurrentPlayer().botDifficulty) {
                    BotDifficulty.EASY -> 460L
                    BotDifficulty.MEDIUM -> 360L
                    BotDifficulty.PRO -> 280L
                    else -> 380L
                }
                postDelayed({ runBotTurn() }, thinkDelay)
            }
        }
    }

    fun runBotTurn() {
        val eng = engine ?: return
        if (eng.isGameOver || !eng.getCurrentPlayer().isBot) return

        val move = eng.calculateBotMove() ?: return
        executeMove(move.isH, move.r, move.c, isBotMove = true)
    }
}