package com.headscroll

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

object C {
    val BG = Color.parseColor("#0E1117")
    val CARD = Color.parseColor("#171B24")
    val CARD2 = Color.parseColor("#222836")
    val ACCENT = Color.parseColor("#7C5CFF")
    val ACCENT2 = Color.parseColor("#22D3EE")
    val TEXT = Color.parseColor("#EDEFF5")
    val SUB = Color.parseColor("#8A93A6")
    val GOOD = Color.parseColor("#3DDC97")
    val WARN = Color.parseColor("#FFB020")
    val BAD = Color.parseColor("#FF5C6C")
}

class Ui(val ctx: Context) {
    val base: Typeface? = runCatching { ctx.resources.getFont(R.font.vazirmatn) }.getOrNull()
    val bold: Typeface? = base?.let { Typeface.create(it, Typeface.BOLD) }
    private val density = ctx.resources.displayMetrics.density

    fun dp(v: Int): Int = (v * density).toInt()

    fun label(s: String, size: Float = 14f, color: Int = C.TEXT, isBold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = size
            setTextColor(color)
            typeface = if (isBold) (bold ?: Typeface.DEFAULT_BOLD) else (base ?: Typeface.DEFAULT)
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }

    fun vlp(top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(0, dp(top), 0, dp(bottom)) }

    fun rounded(color: Int, r: Int = 16): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(r).toFloat()
        }

    fun card(): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(C.CARD, 20)
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    fun pressable(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            false
        }
    }

    fun button(s: String, primary: Boolean = true, onClick: () -> Unit): TextView {
        val bg: GradientDrawable = if (primary) {
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(C.ACCENT, C.ACCENT2))
                .apply { cornerRadius = dp(14).toFloat() }
        } else rounded(C.CARD2, 14)
        return label(s, 15f, C.TEXT, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), bg, null)
            isClickable = true
            setOnClickListener { onClick() }
            pressable(this)
        }
    }

    fun switchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label(title, 15f))
        if (sub != null) col.addView(label(sub, 12f, C.SUB))
        row.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        val sw = Switch(ctx).apply {
            isChecked = checked
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(Color.WHITE, Color.parseColor("#B0B7C6")))
            trackTintList = ColorStateList(states, intArrayOf(C.ACCENT, C.CARD2))
            setOnCheckedChangeListener { _, c -> onChange(c) }
        }
        row.addView(sw)
        return row
    }

    fun sliderRow(
        title: String, lo: Int, hi: Int, step: Int, value: Int,
        fmt: (Int) -> String, onChange: (Int) -> Unit
    ): View {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        val valueTv = label(fmt(value), 14f, C.ACCENT2, true)
        head.addView(label(title, 15f), LinearLayout.LayoutParams(0, WRAP, 1f))
        head.addView(valueTv)
        val sb = SeekBar(ctx)
        sb.max = (hi - lo) / step
        sb.progress = (value - lo) / step
        sb.progressTintList = ColorStateList.valueOf(C.ACCENT)
        sb.thumbTintList = ColorStateList.valueOf(C.ACCENT2)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                val nv = lo + p * step
                valueTv.text = fmt(nv)
                if (fromUser) onChange(nv)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        box.addView(head)
        box.addView(sb)
        return box
    }

    fun choiceRow(
        emoji: String, title: String, value: String, sub: String? = null,
        onClick: (TextView) -> Unit
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            isClickable = true
            background = RippleDrawable(ColorStateList.valueOf(0x22FFFFFF), null, ColorDrawable(Color.WHITE))
        }
        row.addView(label(emoji, 22f), LinearLayout.LayoutParams(dp(40), WRAP))
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label(title, 15f))
        if (sub != null) col.addView(label(sub, 12f, C.SUB))
        row.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        val v = label(value, 13f, C.ACCENT2, true)
        row.addView(v)
        row.addView(label("  ‹", 18f, C.SUB))
        row.setOnClickListener { onClick(v) }
        pressable(row)
        return row
    }

    fun divider(): View = View(ctx).apply {
        setBackgroundColor(C.CARD2)
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(1))
    }

    fun pickDialog(title: String, items: List<String>, onPick: (Int) -> Unit) {
        AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, i -> onPick(i) }
            .show()
    }

    /** ورود پله‌ای کارت‌ها: محو + حرکت از پایین */
    fun stagger(parent: ViewGroup, startDelay: Long = 0L) {
        for (i in 0 until parent.childCount) {
            val c = parent.getChildAt(i)
            c.alpha = 0f
            c.translationY = dp(22).toFloat()
            c.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelay + i * 60L)
                .setDuration(380)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}
