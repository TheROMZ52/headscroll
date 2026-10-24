package com.headscroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/** صفحه‌ی تست زنده: نقطه = وضعیت سر، کادر = آستانه‌ی ژست */
class GaugeView(ctx: Context, private val prefs: Prefs, private val ui: Ui) : View(ctx) {

    private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.CARD2 }
    private val calm = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F2A3D.toInt() }
    private val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ui.dp(2).toFloat()
        color = C.ACCENT
        pathEffect = DashPathEffect(floatArrayOf(ui.dp(8).toFloat(), ui.dp(6).toFloat()), 0f)
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ui.dp(3).toFloat()
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = ui.base
        textSize = ui.dp(12).toFloat()
    }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var ox = 0f
    private var oy = 0f
    private val trailX = FloatArray(10)
    private val trailY = FloatArray(10)
    private var trailI = 0

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

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(MeasureSpec.getSize(w), ui.dp(240))
    }

    private fun short(t: Trigger): String {
        val a = prefs.action(t)
        return if (a == Action.NONE) "" else a.label
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val now = SystemClock.elapsedRealtime()
        val r = ui.dp(20).toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, r, r, panel)

        val cx = w / 2f
        val cy = h / 2f
        val thP = prefs.pitchTh.toFloat()
        val thY = prefs.yawTh.toFloat()
        val sx = (w / 2f - ui.dp(60)) / (thY * 1.7f)
        val sy = (h / 2f - ui.dp(46)) / (thP * 1.7f)

        rect.set(cx - thY * 0.5f * sx, cy - thP * 0.5f * sy, cx + thY * 0.5f * sx, cy + thP * 0.5f * sy)
        canvas.drawRoundRect(rect, ui.dp(10).toFloat(), ui.dp(10).toFloat(), calm)
        rect.set(cx - thY * sx, cy - thP * sy, cx + thY * sx, cy + thP * sy)
        canvas.drawRoundRect(rect, ui.dp(10).toFloat(), ui.dp(10).toFloat(), box)

        text.color = C.SUB
        text.textSize = ui.dp(11).toFloat()
        text.textAlign = Paint.Align.CENTER
        canvas.drawText(short(Trigger.UP), cx, ui.dp(22).toFloat(), text)
        canvas.drawText(short(Trigger.DOWN), cx, h - ui.dp(46).toFloat(), text)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText(short(Trigger.LEFT), ui.dp(10).toFloat(), cy, text)
        text.textAlign = Paint.Align.RIGHT
        canvas.drawText(short(Trigger.RIGHT), w - ui.dp(10).toFloat(), cy, text)

        // نقطه
        val live = LiveState.running && LiveState.face
        val dyaw = if (live) LiveState.dYaw else 0f
        val dpit = if (live) LiveState.dPitch else 0f
        val maxX = w / 2f - ui.dp(16)
        val maxY = h / 2f - ui.dp(16)
        val tx = (dyaw * sx).coerceIn(-maxX, maxX)
        val ty = (-dpit * sy).coerceIn(-maxY, maxY)
        ox += (tx - ox) * 0.3f
        oy += (ty - oy) * 0.3f
        val px = cx + ox
        val py = cy + oy

        trailX[trailI] = px
        trailY[trailI] = py
        trailI = (trailI + 1) % trailX.size
        if (live) {
            for (k in 1..trailX.size) {
                val idx = (trailI + k - 1) % trailX.size
                val a = k / trailX.size.toFloat()
                dot.color = C.ACCENT2
                dot.alpha = (a * 70).toInt()
                canvas.drawCircle(trailX[idx], trailY[idx], ui.dp(10).toFloat() * a, dot)
            }
        }

        val mag = max(abs(dyaw / thY), abs(dpit / thP))
        val col = when {
            !live -> C.SUB
            mag >= 1f -> C.GOOD
            else -> C.ACCENT2
        }
        dot.alpha = 255
        dot.color = col
        canvas.drawCircle(px, py, ui.dp(11).toFloat(), dot)
        dot.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(px, py, ui.dp(4).toFloat(), dot)

        // موج بعد از ژست
        val el = now - LiveState.lastGestureAt
        if (LiveState.lastGestureAt > 0 && el in 0..650) {
            val p = el / 650f
            ring.color = if (LiveState.lastPerformed) C.GOOD else C.ACCENT
            ring.alpha = ((1f - p) * 200).toInt()
            canvas.drawCircle(px, py, ui.dp(12).toFloat() + p * ui.dp(60), ring)
        }

        // ایموجی‌های ژست دست و لبخند
        text.textAlign = Paint.Align.LEFT
        text.textSize = ui.dp(26).toFloat()
        text.color = C.TEXT
        if (LiveState.hand.isNotEmpty()) canvas.drawText(LiveState.hand, ui.dp(12).toFloat(), ui.dp(38).toFloat(), text)
        text.textAlign = Paint.Align.RIGHT
        if (live && LiveState.smile > 0.6f) canvas.drawText("😄", w - ui.dp(12).toFloat(), ui.dp(38).toFloat(), text)

        // پیام وسط پایین
        text.textAlign = Paint.Align.CENTER
        text.textSize = ui.dp(13).toFloat()
        var msg = ""
        var msgColor = C.SUB
        var prog = -1f
        when {
            LiveState.wizardStep > 0 -> {
                msg = LiveState.wizardMsg; msgColor = C.TEXT; prog = LiveState.wizardProgress
                text.textSize = ui.dp(15).toFloat()
            }
            now - LiveState.wizardResultAt < 4500 && LiveState.wizardResult.isNotEmpty() -> {
                msg = LiveState.wizardResult; msgColor = C.GOOD
            }
            !LiveState.running -> msg = "سرویس خاموشه، روشنش کن"
            LiveState.paused -> { msg = "⏸ مکث شده"; msgColor = C.WARN }
            !LiveState.cameraOn -> msg = "دوربین خاموشه (منتظر باز شدن اپ مقصد)"
            !LiveState.face && LiveState.hand.isEmpty() -> msg = "چهره‌ای پیدا نشد"
            LiveState.calibrating -> {
                msg = "در حال کالیبره… سرتو صاف نگه دار"; msgColor = C.WARN
                prog = LiveState.calProgress
            }
        }
        text.color = msgColor
        if (msg.isNotEmpty()) canvas.drawText(msg, cx, h - ui.dp(24).toFloat(), text)
        if (prog >= 0f) {
            bar.color = C.CARD
            rect.set(ui.dp(16).toFloat(), h - ui.dp(14).toFloat(), w - ui.dp(16), h - ui.dp(9).toFloat())
            canvas.drawRoundRect(rect, ui.dp(3).toFloat(), ui.dp(3).toFloat(), bar)
            bar.color = C.ACCENT2
            rect.set(ui.dp(16).toFloat(), h - ui.dp(14).toFloat(), ui.dp(16) + (w - ui.dp(32)) * prog, h - ui.dp(9).toFloat())
            canvas.drawRoundRect(rect, ui.dp(3).toFloat(), ui.dp(3).toFloat(), bar)
        }
    }
}
