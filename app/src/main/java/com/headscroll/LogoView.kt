package com.headscroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** لوگوی انیمیشنی: صورت که سر تکون میده و فلش‌ها بالا/پایین چشمک میزنن */
class LogoView(ctx: Context) : View(ctx) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5B3FD9.toInt() }
    private val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5B3FD9.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()
    private val path = Path()

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            if (isAttachedToWindow) postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        bg.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(), C.ACCENT, C.ACCENT2, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val s = width.toFloat()
        val phase = ((SystemClock.uptimeMillis() % 2600L) / 2600f) * 2f * PI.toFloat()
        val nod = sin(phase)

        rect.set(0f, 0f, s, s)
        canvas.drawRoundRect(rect, s * 0.26f, s * 0.26f, bg)

        val cx = s / 2f
        val cy = s / 2f + nod * s * 0.03f
        val r = s * 0.22f
        canvas.drawCircle(cx, cy, r, white)
        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.12f, r * 0.11f, dark)
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.12f, r * 0.11f, dark)
        smilePaint.strokeWidth = s * 0.028f
        rect.set(cx - r * 0.5f, cy - r * 0.08f, cx + r * 0.5f, cy + r * 0.5f)
        canvas.drawArc(rect, 25f, 130f, false, smilePaint)

        arrow.strokeWidth = s * 0.045f
        val upA = (110 + 145 * max(0f, nod)).toInt().coerceIn(0, 255)
        val dnA = (110 + 145 * max(0f, -nod)).toInt().coerceIn(0, 255)

        arrow.alpha = upA
        val uy = s * 0.2f - max(0f, nod) * s * 0.02f
        path.reset()
        path.moveTo(cx - s * 0.09f, uy + s * 0.05f)
        path.lineTo(cx, uy)
        path.lineTo(cx + s * 0.09f, uy + s * 0.05f)
        canvas.drawPath(path, arrow)

        arrow.alpha = dnA
        val dy = s * 0.8f + max(0f, -nod) * s * 0.02f
        path.reset()
        path.moveTo(cx - s * 0.09f, dy - s * 0.05f)
        path.lineTo(cx, dy)
        path.lineTo(cx + s * 0.09f, dy - s * 0.05f)
        canvas.drawPath(path, arrow)
    }
}
