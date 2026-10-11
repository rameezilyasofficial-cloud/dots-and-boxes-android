package com.fixtocode.dotsandboxes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
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
    private val audio = AudioFx()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF475569.toInt()
        style = Paint.Style.FILL
    }
    private val idleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E293B.toInt()
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val activeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
    }
    private val boxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 48f
        isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return

        val padding = 48f
        val availW = width - (padding * 2)
        val availH = height - (padding * 2)
        val cellSize = min(availW / eng.cols, availH / eng.rows)

        val startX = (width - (cellSize * eng.cols)) / 2f
        val startY = (height - (cellSize * eng.rows)) / 2f

        for (r in 0 until eng.rows) {
            for (c in 0 until eng.cols) {
                val ownerId = eng.boxes[r][c] ?: continue
                val p = eng.players.firstOrNull { it.id == ownerId } ?: continue

                val left = startX + (c * cellSize)
                val top = startY + (r * cellSize)
                val rect = RectF(left + 4f, top + 4f, left + cellSize - 4f, top + cellSize - 4f)

                boxFillPaint.color = (p.color and 0x00FFFFFF) or 0x33000000
                canvas.drawRoundRect(rect, 12f, 12f, boxFillPaint)

                textPaint.color = p.color
                val textY = top + (cellSize / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText(p.name.take(1).uppercase(), left + (cellSize / 2f), textY, textPaint)
            }
        }

        for (r in 0..eng.rows) {
            for (c in 0 until eng.cols) {
                val ownerId = eng.hLines[r][c]
                val x1 = startX + (c * cellSize)
                val x2 = x1 + cellSize
                val y = startY + (r * cellSize)

                if (ownerId != null) {
                    val p = eng.players.first { it.id == ownerId }
                    activeLinePaint.color = p.color
                    canvas.drawLine(x1, y, x2, y, activeLinePaint)
                } else {
                    canvas.drawLine(x1, y, x2, y, idleLinePaint)
                }
            }
        }

        for (r in 0 until eng.rows) {
            for (c in 0..eng.cols) {
                val ownerId = eng.vLines[r][c]
                val y1 = startY + (r * cellSize)
                val y2 = y1 + cellSize
                val x = startX + (c * cellSize)

                if (ownerId != null) {
                    val p = eng.players.first { it.id == ownerId }
                    activeLinePaint.color = p.color
                    canvas.drawLine(x, y1, x, y2, activeLinePaint)
                } else {
                    canvas.drawLine(x, y1, x, y2, idleLinePaint)
                }
            }
        }

        val dotRadius = min(12f, cellSize * 0.12f)
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

        val padding = 48f
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
                    executeMove(true, r, c)
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
                    executeMove(false, r, c)
                    return true
                }
            }
        }
        return true
    }

    private fun executeMove(isH: Boolean, r: Int, c: Int) {
        val eng = engine ?: return
        val captured = eng.claimLine(isH, r, c)
        if (captured >= 0) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (captured > 0) audio.boxClaimed() else audio.snap()
            invalidate()
            onTurnCompleted?.invoke()

            if (eng.isGameOver) {
                audio.victory()
            } else if (eng.getCurrentPlayer().isBot) {
                postDelayed({ runBotTurn() }, 400L)
            }
        }
    }

    fun runBotTurn() {
        val eng = engine ?: return
        if (eng.isGameOver || !eng.getCurrentPlayer().isBot) return

        val move = eng.calculateBotMove() ?: return
        executeMove(move.isH, move.r, move.c)
    }
}
