package com.headscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.android.systemui" || pkg.contains("inputmethod")) return
        if (pkg != currentPkg) {
            currentPkg = pkg
            onPackageChanged?.invoke()
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun isTargetForeground(): Boolean {
        val p = Prefs(this)
        val pk = currentPkg
        if (pk == packageName) return false
        if (pk.isEmpty() || p.allApps) return true
        return p.targetPackages().contains(pk)
    }

    /** true یعنی اکشن اجرا شد */
    fun perform(a: Action): Boolean {
        if (a == Action.NONE || !isTargetForeground()) return false
        val ms = Prefs(this).swipeMs.toLong()
        main.post {
            when (a) {
                Action.NEXT -> swipeV(true, ms)
                Action.PREV -> swipeV(false, ms)
                Action.NEXT_H -> swipeH(true, ms)
                Action.PREV_H -> swipeH(false, ms)
                Action.LIKE -> tap(2)
                Action.TAP -> tap(1)
                Action.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                Action.NONE -> {}
            }
        }
        return true
    }

    private fun swipeV(next: Boolean, ms: Long) {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val top = dm.heightPixels * 0.25f
        val bottom = dm.heightPixels * 0.75f
        val path = Path().apply {
            if (next) {
                moveTo(x, bottom); lineTo(x, top)
            } else {
                moveTo(x, top); lineTo(x, bottom)
            }
        }
        dispatch(path, ms)
    }

    private fun swipeH(next: Boolean, ms: Long) {
        val dm = resources.displayMetrics
        val y = dm.heightPixels / 2f
        val left = dm.widthPixels * 0.25f
        val right = dm.widthPixels * 0.75f
        val path = Path().apply {
            if (next) {
                moveTo(right, y); lineTo(left, y)
            } else {
                moveTo(left, y); lineTo(right, y)
            }
        }
        dispatch(path, ms)
    }

    private fun dispatch(path: Path, ms: Long) {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, ms))
            .build()
        dispatchGesture(g, null, null)
    }

    private fun tap(times: Int) {
        val dm = resources.displayMetrics
        val x = dm.widthPixels / 2f
        val y = dm.heightPixels * 0.5f
        val b = GestureDescription.Builder()
        for (i in 0 until times) {
            val p = Path().apply { moveTo(x, y) }
            b.addStroke(GestureDescription.StrokeDescription(p, i * 110L, 40L))
        }
        dispatchGesture(b.build(), null, null)
    }

    companion object {
        @Volatile var instance: ScrollAccessibilityService? = null
        @Volatile var currentPkg: String = ""
        @Volatile var onPackageChanged: (() -> Unit)? = null
    }
}
